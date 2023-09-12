package au.org.aodn.geonetwork4;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fao.geonet.events.md.MetadataRemove;
import org.springframework.context.ApplicationListener;

/**
 * The event fire when user select a metadata record and hit the delete button, this result in
 * the whole metadata record deleted
 */
public class MetadataRemoveEventHandler implements ApplicationListener<MetadataRemove> {

    protected Logger logger = LogManager.getLogger(MetadataRemoveEventHandler.class);

    @Override
    public void onApplicationEvent(MetadataRemove event) {
        logger.info("AODN MetadataRemove event");
    }
}
