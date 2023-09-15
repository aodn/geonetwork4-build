package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork4.handler.*;
import au.org.aodn.geonetwork4.ssl.HttpsTrustManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
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

    @Autowired
    protected MetadataUpdateEventHandler metadataUpdateEventHandler;

    @Autowired
    protected MetadataRemoveEventHandler metadataRemoveEventHandler;

    @Autowired
    protected MetadataAddEventHandler metadataAddEventHandler;

    @Autowired
    protected MetadataPublishedEventHandler metadataPublishedEventHandler;

    @Autowired
    protected MetadataUnPublishedEventHandler metadataUnPublishedEventHandler;

    @Value("${aodn.geonetwork4:DEV}")
    protected Environment environment;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException, KeyManagementException {
        /**
         * Geonetwork set root logger to OFF for most log4j2 profile, hence you miss most of the information,
         * this make it super hard to debug. The code here is to turn the ROOT logger back to INFO. It will be,
         * logger dependent and by default log goes to FILE appender only.
         */
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.config.Configuration config = ctx.getConfiguration();

        AppenderRef file = AppenderRef.createAppenderRef("File", Level.INFO, null);
        AppenderRef console = AppenderRef.createAppenderRef("Console", Level.INFO, null);

        LoggerConfig c = LoggerConfig.newBuilder()
                .withLevel(Level.INFO)
                .withRefs(new AppenderRef[] {file, console})
                .withLoggerName("au.org.aodn.geonetwork4")
                .withIncludeLocation("au.org.aodn.geonetwork4")
                .withAdditivity(false)
                .withConfig(config)
                .build();

        c.addAppender(config.getAppender("File"), Level.INFO, null);
        c.addAppender(config.getAppender("Console"), Level.INFO, null);

        config.addLogger("au.org.aodn.geonetwork4", c);
        // LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        // loggerConfig.setLevel(Level.INFO);
        ctx.updateLoggers();

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
        jeevesContext.addApplicationListener(metadataUpdateEventHandler);
        jeevesContext.addApplicationListener(metadataRemoveEventHandler);
        jeevesContext.addApplicationListener(metadataAddEventHandler);
        jeevesContext.addApplicationListener(metadataPublishedEventHandler);
        jeevesContext.addApplicationListener(metadataUnPublishedEventHandler);
    }

    @Bean
    public MetadataAddEventHandler createMetadataAddEventHandler() {
        return new MetadataAddEventHandler();
    }

    @Bean
    public MetadataUpdateEventHandler createMetadataUpdateEventHandler() {
        return new MetadataUpdateEventHandler();
    }

    @Bean
    public MetadataRemoveEventHandler createMetadataRemoveEventHandler() {
        return new MetadataRemoveEventHandler();
    }

    @Bean
    public MetadataPublishedEventHandler createMetadataPublishedEventHandler() {
        return new MetadataPublishedEventHandler();
    }

    @Bean
    public MetadataUnPublishedEventHandler createMetadataUnPublishedEventHandler() {
        return new MetadataUnPublishedEventHandler();
    }

    @Bean
    public RestTemplate createRestTemplate() {
        return new RestTemplate();
    }
}
