package au.org.aodn.geonetwork4.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fao.geonet.events.md.MetadataPublished;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * The event fire when user click "manage record" and publish the record.
 */
public class MetadataPublishedEventHandler implements ApplicationListener<MetadataPublished> {

    protected Logger logger = LogManager.getLogger(MetadataPublishedEventHandler.class);

    @Value("${aodn.geonetwork4.esIndexer.urlIndex}")
    protected String indexUrl;

    @Autowired
    protected RestTemplate restTemplate;

    @Override
    public void onApplicationEvent(MetadataPublished metadataPublished) {
        logger.info("Call indexer published on metadata {}", metadataPublished.getMd().getUuid());
        Map<String, Object> variable = new HashMap<>();

        variable.put("uuid", metadataPublished.getMd().getUuid());
        restTemplate.postForEntity(indexUrl, null, String.class, variable);
    }
}
