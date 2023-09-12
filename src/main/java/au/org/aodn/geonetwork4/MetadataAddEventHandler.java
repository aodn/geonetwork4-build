package au.org.aodn.geonetwork4;

import org.fao.geonet.events.md.MetadataAdd;
import org.springframework.context.ApplicationListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The event fire when user import or add new metadata record
 */
public class MetadataAddEventHandler implements ApplicationListener<MetadataAdd> {

    protected Logger logger = LogManager.getLogger(MetadataAddEventHandler.class);

    @Override
    public void onApplicationEvent(MetadataAdd metadataEvent) {
        logger.info("AODN MetadataAdd event {}", metadataEvent.getMd().getData());
    }
}
