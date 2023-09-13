package au.org.aodn.geonetwork4.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fao.geonet.events.md.MetadataUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * The event fire when user click a record and view record details or update details in the record
 */
public class MetadataUpdateEventHandler implements ApplicationListener<MetadataUpdate> {

    protected Logger logger = LogManager.getLogger(MetadataUpdateEventHandler.class);

    @Value("${aodn.geonetwork4.esIndexer.urlIndex}")
    protected String indexUrl;

    @Autowired
    protected RestTemplate restTemplate;

    @Override
    public void onApplicationEvent(MetadataUpdate metadataEvent) {
        logger.info("Call indexer update on metadata {}", metadataEvent.getMd().getUuid());
        Map<String, Object> variable = new HashMap<>();

        variable.put("uuid", metadataEvent.getMd().getUuid());
        restTemplate.postForEntity(indexUrl, null, String.class, variable);
    }
}
