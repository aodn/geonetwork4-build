package au.org.aodn.geonetwork4.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fao.geonet.events.md.MetadataRemove;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * The event fire when user select a metadata record and hit the delete button, this result in
 * the whole metadata record deleted
 */
public class MetadataRemoveEventHandler implements ApplicationListener<MetadataRemove> {

    protected Logger logger = LogManager.getLogger(MetadataRemoveEventHandler.class);

    @Value("${aodn.geonetwork4.esIndexer.urlIndex}")
    protected String indexUrl;

    @Autowired
    protected RestTemplate restTemplate;

    @Override
    public void onApplicationEvent(MetadataRemove event) {
        logger.info("Call indexer delete on metadata {}", event.getMd().getUuid());
        Map<String, Object> variable = new HashMap<>();

        variable.put("uuid", event.getMd().getUuid());
        restTemplate.delete(indexUrl, variable);
    }
}
