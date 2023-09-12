package au.org.aodn.geonetwork4;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fao.geonet.events.md.MetadataUpdate;
import org.springframework.context.ApplicationListener;

/**
 * The event fire when user click a record and view record details or update details in the record
 */
public class MetadataUpdateEventHandler implements ApplicationListener<MetadataUpdate> {

    protected Logger logger = LogManager.getLogger(MetadataUpdateEventHandler.class);

    @Override
    public void onApplicationEvent(MetadataUpdate metadataEvent) {
        logger.info("AODN MetadataUpdate event {}", metadataEvent.getMd().getData());
    }
}
