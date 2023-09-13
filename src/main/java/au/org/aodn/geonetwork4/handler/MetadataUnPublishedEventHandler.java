package au.org.aodn.geonetwork4.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fao.geonet.events.md.MetadataUnpublished;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * The event fire when user click "manage record" and un-publish the record.
 */
public class MetadataUnPublishedEventHandler implements ApplicationListener<MetadataUnpublished> {

    protected Logger logger = LogManager.getLogger(MetadataUnPublishedEventHandler.class);

    @Value("${aodn.geonetwork4.esIndexer.urlIndex}")
    protected String indexUrl;

    @Autowired
    protected RestTemplate restTemplate;

    @Override
    public void onApplicationEvent(MetadataUnpublished event) {

        logger.info("Call indexer unpublished on metadata {}", event.getMd().getUuid());
        logger.info("url {}", indexUrl);
        Map<String, Object> variable = new HashMap<>();

        variable.put("uuid", event.getMd().getUuid());
        restTemplate.delete(indexUrl, variable);
    }
}
