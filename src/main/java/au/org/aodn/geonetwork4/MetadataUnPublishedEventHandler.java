package au.org.aodn.geonetwork4;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fao.geonet.events.md.MetadataUnpublished;
import org.springframework.context.ApplicationListener;

/**
 * The event fire when user click "manage record" and un-publish the record.
 */
public class MetadataUnPublishedEventHandler implements ApplicationListener<MetadataUnpublished> {

    protected Logger logger = LogManager.getLogger(MetadataUnPublishedEventHandler.class);

    @Override
    public void onApplicationEvent(MetadataUnpublished metadataPublished) {
        logger.info("AODN MetadataPublished event {}", metadataPublished.getMd().getData());
    }
}
