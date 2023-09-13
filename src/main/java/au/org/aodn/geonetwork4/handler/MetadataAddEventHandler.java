package au.org.aodn.geonetwork4.handler;

import org.fao.geonet.events.md.MetadataAdd;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * The event fire when user import or add new metadata record
 */
public class MetadataAddEventHandler implements ApplicationListener<MetadataAdd> {

    protected Logger logger = LogManager.getLogger(MetadataAddEventHandler.class);

    @Value("${aodn.geonetwork4.esIndexer.urlIndex}")
    protected String indexUrl;

    @Autowired
    protected RestTemplate restTemplate;

    @Override
    public void onApplicationEvent(MetadataAdd metadataEvent) {
        logger.info("Call indexer new on metadata {}", metadataEvent.getMd().getUuid());
        Map<String, Object> variable = new HashMap<>();

        variable.put("uuid", metadataEvent.getMd().getUuid());
        restTemplate.postForEntity(indexUrl, null, String.class, variable);
    }
}
