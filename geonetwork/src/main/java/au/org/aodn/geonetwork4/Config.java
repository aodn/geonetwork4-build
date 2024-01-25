package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork4.handler.*;
import au.org.aodn.geonetwork4.ssl.HttpsTrustManager;
import au.org.aodn.geonetwork_api.openapi.api.HarvestersApiLegacy;
import au.org.aodn.geonetwork_api.openapi.api.HarvestersApi;
import au.org.aodn.geonetwork_api.openapi.api.LogosApi;
import au.org.aodn.geonetwork_api.openapi.api.MeApi;
import au.org.aodn.geonetwork_api.openapi.invoker.ApiClient;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import au.org.aodn.geonetwork4.enumeration.Environment;

import javax.annotation.PostConstruct;
import org.fao.geonet.ApplicationContextHolder;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

@Aspect
@Configuration
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

    @Autowired
    protected GenericEntityListener genericEntityListener;

    /**
     * Geonetwork set root logger to OFF for most log4j2 profile, hence you miss most of the information,
     * this make it super hard to debug. The code here is to turn the ROOT logger back to INFO. It will be,
     * logger dependent and by default log goes to FILE appender only.
     */
    protected void resetLoggerLevel(Level level) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.config.Configuration config = ctx.getConfiguration();

        AppenderRef file = AppenderRef.createAppenderRef("File", level, null);
        AppenderRef console = AppenderRef.createAppenderRef("Console", level, null);

        LoggerConfig c = LoggerConfig.newBuilder()
                .withLevel(level)
                .withRefs(new AppenderRef[] {file, console})
                .withLoggerName("au.org.aodn.geonetwork4")
                .withIncludeLocation("au.org.aodn.geonetwork4")
                .withAdditivity(false)
                .withConfig(config)
                .build();

        c.addAppender(config.getAppender("File"), level, null);
        c.addAppender(config.getAppender("Console"), level, null);

        config.addLogger("au.org.aodn.geonetwork4", c);
        // LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        // loggerConfig.setLevel(Level.INFO);
        ctx.updateLoggers();
    }

    /**
     * Use aspectJ to intercept all call that ends with WithHttpInfo, we must always use geonetwork api call
     * ends with WithHttpInfo because the way geonetworks works is you must present an X-XSRF-TOKEN and session
     * in the call with username password, otherwise it will fail.
     *
     * You need to make an init call to get the X-XSRF-TOKEN, the call will have status forbidden
     * and comes back with the token in cookie, then you need to set the token before next call.
     */
    @Pointcut("execution(public * au.org.aodn.geonetwork_api.openapi.api..*.*WithHttpInfo(..))")
    public void interceptWithHttpInfo() {};

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

        resetLoggerLevel(Level.INFO);
        logger.info("AODN - Done set logger info");

        /**
         * No need to do host verfication, this should apply to dev env only
         */
        if(environment == Environment.DEV) {
            HttpsTrustManager.allowAllSSL();
        }

        /**
         * The key here is to use the application context of a child JeevesApplicationContext where its parent
         * is ApplicationContext.
         */
        ConfigurableApplicationContext jeevesContext = ApplicationContextHolder.get();
        jeevesContext.getBeanFactory().registerSingleton("genericEntityListener", genericEntityListener);
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
     * @param username
     * @param password
     * @return
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ApiClient getApiClient(
            @Value("${GEONETWORK_ADMIN_USERNAME:admin}") String username,
            @Value("${GEONETWORK_ADMIN_PASSWORD:admin}") String password) {

        RestTemplate template = new RestTemplate();
        template.getInterceptors().add(new BasicAuthenticationInterceptor(username, password));

        return new ApiClient(template);
    }

    @Bean
    public MeApi getMeApi(ApiClient client) {
        return new MeApi(client);
    }

    @Bean
    public LogosApi getLogosApi(ApiClient client) {
        return new LogosApi(client);
    }

    @Bean("harvestersApi")
    public HarvestersApi getHarvestersApi(ApiClient client) {
        return new HarvestersApi(client);
    }

    @Bean("harvestersApiLegacy")
    public HarvestersApiLegacy getHarvestersApiLegacy(ApiClient client) {
        return new HarvestersApiLegacy(client);
    }

    @Bean
    public Setup getSetup(MeApi meApi,
                          LogosApi logosApi,
                          @Qualifier("harvestersApiLegacy") HarvestersApiLegacy harvestersApiLegacy,
                          @Qualifier("harvestersApi") HarvestersApi harvestersApi) {
        return new Setup(meApi, logosApi, harvestersApiLegacy, harvestersApi);
    }
}
