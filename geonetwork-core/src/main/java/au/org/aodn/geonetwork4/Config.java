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

import org.fao.geonet.domain.User;
import org.fao.geonet.repository.UserRepository;
import org.fao.geonet.util.PasswordUtil;
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
import java.sql.SQLException;
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

    @Value("${aodn.geonetwork4.env:dev}")
    protected Environment environment;

    @Value("${aodn.geonetwork4.githubBranch}")
    protected String gitBranch;

    @Value("${GEONETWORK_ADMIN_USERNAME:admin}")
    protected String adminUserName;

    @Value("${GEONETWORK_ADMIN_PASSWORD:admin}")
    protected String adminPassword;

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
    public void init() throws NoSuchAlgorithmException, KeyManagementException, SQLException {
        logger.info("AODN - Done set logger info");
        logger.info("Using git branch {} for setup", gitBranch);

        /*
         * No need to do host verfication, this should apply to dev env only
         */
        if(environment == Environment.dev) {
            HttpsTrustManager.allowAllSSL();
        }
        /*
         * The key here is to use the application context of a child JeevesApplicationContext where its parent
         * is ApplicationContext.
         */
        ConfigurableApplicationContext jeevesContext = ApplicationContextHolder.get();
        jeevesContext.getBeanFactory().registerSingleton("genericEntityListener", genericEntityListener);
        /*
         * Update password when start, password comes from an ENV variable, if missing use default
         */
        UserRepository ur = jeevesContext.getBean(UserRepository.class);
        User admin = ur.findOneByUsername(adminUserName);
        PasswordUtil.updatePasswordWithNew(false, null, adminPassword, admin.getId(), jeevesContext);
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
    public GenericEntityListener createGenericEntityListener(
            @Value("${aodn.geonetwork4.esIndexer.apikey}") String apiKey,
            @Value("${aodn.geonetwork4.esIndexer.host}") String host,
            @Value("${aodn.geonetwork4.esIndexer.urlIndex}") String indexUrl,
            RestTemplate restTemplate) {

        return new GenericEntityListener(apiKey, host, indexUrl, restTemplate);
    }
    /**
     * Must use prototype scope as there is a XSRF-TOKEN header for each api, that cannot share
     * with the same api.
     *
     * @return The api client connects geonetwork, it assumed an admin username and password
     */
    @Bean("apiClient")
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ApiClient getApiClient() {

        RestTemplate template = new RestTemplate();
        template.getInterceptors().add(new BasicAuthenticationInterceptor(adminUserName, adminPassword));

        return new ApiClient(template);
    }

    /**
     * Must use prototype scope as there is a XSRF-TOKEN header for each api, that cannot share
     * with the same api. This rest template is different from normal one is that it will not do urlencode on
     * param values, this is needed in some case where "/" is part of the param value and server side
     * received %2F and not convert it back correctly
     *
     * @return An api client which is username and password aware
     */
    @Bean("apiClientNoUrlEncode")
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ApiClient getApiClientNoUrlEncode() {

        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);

        RestTemplate template = new RestTemplate();
        template.getInterceptors().add(new BasicAuthenticationInterceptor(adminUserName, adminPassword));
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
        // Must use no encode because some param is like system/xxxx/xxxx where it becomes
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
     * By default, it uses the main branch, however when you do your development, you can use a different branch
     * by set up the parameter
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
