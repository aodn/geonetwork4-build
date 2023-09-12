package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork4.handler.*;
import au.org.aodn.geonetwork4.ssl.HttpsTrustManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import au.org.aodn.geonetwork4.enumeration.Environment;

import javax.annotation.PostConstruct;
import org.fao.geonet.ApplicationContextHolder;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

@Configuration
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
         * this make it super hard to debug. The code here is to turn the ROOT logger back to INFO. However
         * by default log goes to FILE appender only.
         */
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.config.Configuration config = ctx.getConfiguration();

        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(Level.INFO);
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

}
