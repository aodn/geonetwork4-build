package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork4.handler.*;
import au.org.aodn.geonetwork4.ssl.HttpsTrustManager;
import au.org.aodn.geonetwork_api.openapi.invoker.ApiClient;
import au.org.aodn.geonetwork_api.openapi.invoker.ApiException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.fao.geonet.events.md.MetadataAdd;
import org.fao.geonet.events.md.MetadataUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import au.org.aodn.geonetwork4.enumeration.Environment;

import javax.annotation.PostConstruct;
import org.fao.geonet.ApplicationContextHolder;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.web.client.RestTemplate;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

@Configuration
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
    protected Setup setup;

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

    @PostConstruct
    public void init() throws NoSuchAlgorithmException, KeyManagementException, ApiException {

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

        /**
         * Post setup here
         */
        setup.injectLogos("add_logo.json", "ace_logo.json");
    }

    @Bean
    public RestTemplate createRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public GenericEntityListener createGenericEntityListener() {
        return new GenericEntityListener();
    }

    @Bean
    public ApiClient getApiClient(
            @Value("${GEONETWORK_ADMIN_USERNAME:admin}") String username,
            @Value("${GEONETWORK_ADMIN_PASSWORD:admin}") String password) {

        ApiClient api = new ApiClient();

        api.setVerifyingSsl(false);
        api.setUsername(username);
        api.setPassword(password);

        return api;
    }

    @Bean
    public Setup getSetup(ApiClient apiClient) {
        return new Setup(apiClient);
    }
}
