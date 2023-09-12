package au.org.aodn.geonetwork4;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fao.geonet.events.md.MetadataPublished;
import org.springframework.context.ApplicationListener;

/**
 * The event fire when user click "manage record" and publish the record.
 */
public class MetadataPublishedEventHandler implements ApplicationListener<MetadataPublished> {

    protected Logger logger = LogManager.getLogger(MetadataPublishedEventHandler.class);

    @Override
    public void onApplicationEvent(MetadataPublished metadataPublished) {
        logger.info("AODN MetadataPublished event {}", metadataPublished.getMd().getData());
    }
}
