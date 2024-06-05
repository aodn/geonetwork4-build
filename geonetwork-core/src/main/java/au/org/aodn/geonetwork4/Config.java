package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork4.handler.*;
import au.org.aodn.geonetwork4.model.GitRemoteConfig;
import au.org.aodn.geonetwork4.ssl.HttpsTrustManager;
import au.org.aodn.geonetwork_api.openapi.api.*;
import au.org.aodn.geonetwork_api.openapi.invoker.ApiClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import au.org.aodn.geonetwork4.enumeration.Environment;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import org.fao.geonet.ApplicationContextHolder;

import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.lang.reflect.Method;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

@Aspect
@Configuration
/*
 * EnableAutoConfiguration so that Actuator can config automatically, however, the
 * geonetwork itself start a few beans already so auto config will end up with
 * 2 same bean which is not needed.
 */
@EnableAutoConfiguration(exclude = {
        org.springdoc.core.SpringDocConfiguration.class,
        org.springdoc.webmvc.core.SpringDocWebMvcConfiguration.class,
        org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration.class
})
@EnableAspectJAutoProxy
@PropertySources({
        @PropertySource("classpath:application.properties"),
        @PropertySource(
                value = "classpath:application-${spring.profiles.active}.properties",
                ignoreResourceNotFound = true)
})
public class Config {

    protected Logger logger = LogManager.getLogger(Config.class);

    @Value("${aodn.geonetwork4.env:DEV}")
    protected Environment environment;

    @Value("${aodn.geonetwork4.githubBranch}")
    protected String gitBranch;

    @Autowired
    protected GenericEntityListener genericEntityListener;

    /**
     * Use aspectJ to intercept all call that ends with WithHttpInfo, we must always use geonetwork api call
     * ends with WithHttpInfo because the way geonetworks works is you must present an X-XSRF-TOKEN and session
     * in the call with username password, otherwise it will fail.
     * You need to make an init call to get the X-XSRF-TOKEN, the call will have status forbidden
     * and comes back with the token in cookie, then you need to set the token before next call.
     */
    @Pointcut("execution(public * au.org.aodn.geonetwork_api.openapi.api..*.*WithHttpInfo(..))")
    public void interceptWithHttpInfo() {}

    @Around("interceptWithHttpInfo()")
    public ResponseEntity<?> aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return (ResponseEntity<?>) joinPoint.proceed();
        }
        catch(HttpClientErrorException.Forbidden e) {
            // cookie format is like this
            // XSRF-TOKEN=634f5b0d-49b6-43a6-a995-c7cb0db9eb64; Path=/geonetwork
            String cookie = e.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
            String token = cookie.split(";")[0].split("=")[1].trim();

            // All these api object have the getApiClient() method
            Method method = joinPoint.getTarget().getClass().getMethod("getApiClient");
            ApiClient apiClient = (ApiClient)method.invoke(joinPoint.getTarget());

            logger.info("Setting X-XSRF-TOKEN for {} to {}", joinPoint.getTarget().getClass(), token);
            apiClient.addDefaultHeader("X-XSRF-TOKEN", token);
            apiClient.addDefaultCookie("XSRF-TOKEN", token);

            return (ResponseEntity<?>)joinPoint.proceed();
        }
    }

    @PostConstruct
    public void init() throws NoSuchAlgorithmException, KeyManagementException {
        logger.info("AODN - Done set logger info");
        logger.info("Using git branch {} for setup", gitBranch);

        /*
         * No need to do host verfication, this should apply to dev env only
         */
        if(environment == Environment.DEV) {
            HttpsTrustManager.allowAllSSL();
        }

        /*
         * The key here is to use the application context of a child JeevesApplicationContext where its parent
         * is ApplicationContext.
         */
        ConfigurableApplicationContext jeevesContext = ApplicationContextHolder.get();
        jeevesContext.getBeanFactory().registerSingleton("genericEntityListener", genericEntityListener);
    }
    /**
     * The reason we need is to set the WEB_ROOT context to be used by Actuator. In springboot application it is
     * set on start, but this geonetwork is a different species that it isn't a springboot app so this setting
     * is missing
     *
     * @param sc servlet context that pass around
     */
    @Autowired
    public void setRootContext(ServletContext sc, ConfigurableApplicationContext context) {
        sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
    }

    @Bean
    public RestTemplate createRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public GenericEntityListener createGenericEntityListener() {
        return new GenericEntityListener();
    }
    /**
     * Must use prototype scope as there is a XSRF-TOKEN header for each api, that cannot share
     * with the same api.
     *
     * @param username geonetwork admin user name
     * @param password geonetwork admin password
     * @return The api client connects geonetwork
     */
    @Bean("apiClient")
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ApiClient getApiClient(
            @Value("${GEONETWORK_ADMIN_USERNAME:admin}") String username,
            @Value("${GEONETWORK_ADMIN_PASSWORD:admin}") String password) {

        RestTemplate template = new RestTemplate();
        template.getInterceptors().add(new BasicAuthenticationInterceptor(username, password));

        return new ApiClient(template);
    }

    /**
     * Must use prototype scope as there is a XSRF-TOKEN header for each api, that cannot share
     * with the same api. This rest template is different from normal one is that it will not do urlencode on
     * param values, this is needed in some case where "/" is part of the param value and server side
     * received %2F and not convert it back correctly
     *
     * @param username
     * @param password
     * @return
     */
    @Bean("apiClientNoUrlEncode")
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ApiClient getApiClientNoUrlEncode(
            @Value("${GEONETWORK_ADMIN_USERNAME:admin}") String username,
            @Value("${GEONETWORK_ADMIN_PASSWORD:admin}") String password) {

        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);

        RestTemplate template = new RestTemplate();
        template.getInterceptors().add(new BasicAuthenticationInterceptor(username, password));
        template.setUriTemplateHandler(defaultUriBuilderFactory);

        return new ApiClient(template);
    }

    @Bean
    public MeApi getMeApi(@Qualifier("apiClient") ApiClient client) {
        return new MeApi(client);
    }

    @Bean
    public LogosApiExt getLogosApi(@Qualifier("apiClient") ApiClient client) {
        return new LogosApiExt(client);
    }

    @Bean
    public GroupsApi getGroupsApi(@Qualifier("apiClient") ApiClient client) { return new GroupsApi(client); }

    @Bean
    public UsersApi getUsersApi(@Qualifier("apiClient") ApiClient client) { return new UsersApi(client); }

    @Bean
    public TagsApi getTagsApi(@Qualifier("apiClient") ApiClient client) { return new TagsApi(client); }

    @Bean
    public SiteApi getSiteApi(@Qualifier("apiClientNoUrlEncode") ApiClient client) {
        // Must use no encode because some param is like system/xxxx/xxxx where it become
        // system%2Fselectionmanager%2Fmaxrecords on server side
        return new SiteApi(client);
    }

    @Bean("harvestersApi")
    public HarvestersApi getHarvestersApi(@Qualifier("apiClient") ApiClient client) {
        return new HarvestersApi(client);
    }

    @Bean("harvestersApiLegacy")
    public HarvestersApiLegacy getHarvestersApiLegacy(@Qualifier("apiClient") ApiClient client, GroupsApi groupsApi, TagsApi tagsApi) {
        return new HarvestersApiLegacy(client, groupsApi, tagsApi);
    }

    @Bean
    RegistriesApiExt getRegistriesApi(@Qualifier("apiClient") ApiClient client) {
        return new RegistriesApiExt(client);
    }

    @Bean
    public Setup getSetup(ResourceLoader resourceLoader,
                          MeApi meApi,
                          LogosApiExt logosApi,
                          GroupsApi groupsApi,
                          TagsApi tagsApi,
                          RegistriesApiExt registriesApi,
                          SiteApi siteApi,
                          UsersApi usersApi,
                          @Qualifier("harvestersApiLegacy") HarvestersApiLegacy harvestersApiLegacy,
                          @Qualifier("harvestersApi") HarvestersApi harvestersApi) {

        return new Setup(resourceLoader, meApi, logosApi, groupsApi, tagsApi, registriesApi, siteApi, usersApi, harvestersApiLegacy, harvestersApi);
    }
    /**
     * By default it use the main branch, however when you do your development, you can use a different branch
     * by setup the parameter
     */
    @Bean("remoteSources")
    public Map<String, GitRemoteConfig> createUtils(
            RestTemplate restTemplate,
            org.springframework.core.env.Environment environment,
            @Value("${aodn.geonetwork4.githubBranch:main}") String gitBranch) {

        String[] profiles = environment.getActiveProfiles();
        String profile = Arrays.stream(profiles).findFirst().orElse(null);

        // We only allow single profile hence we pick the first one
        return Map.of("github", new GitRemoteConfig(
                restTemplate,
                profile,
                gitBranch));
    }
}
