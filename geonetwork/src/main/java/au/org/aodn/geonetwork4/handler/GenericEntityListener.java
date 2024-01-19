package au.org.aodn.geonetwork4.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.entitylistener.GeonetworkEntityListener;
import org.fao.geonet.entitylistener.PersistentEventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GenericEntityListener implements GeonetworkEntityListener<Metadata> {

    protected Logger logger = LogManager.getLogger(GenericEntityListener.class);

    // Expiring map make sure unused item will be discarded
    protected Map<String, Metadata> maps = new ConcurrentHashMap<>();

    protected ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    @Value("${aodn.geonetwork4.esIndexer.urlIndex}")
    protected String indexUrl;

    @Value("${aodn.geonetwork4.esIndexer.apikey}")
    protected String apiKey;

    @Autowired
    protected RestTemplate restTemplate;

    @Override
    public Class<Metadata> getEntityClass() {
        return Metadata.class;
    }

    @PreDestroy
    public void cleanUp() {
        service.shutdown();
    }

    @PostConstruct
    public void init() {
        // We pick up the items in map and then post trigger indexer call, this thread keep execute every 5 secs
        service.scheduleWithFixedDelay(() -> {
            // TODO: If the metadata is unpublished, then indexer cannot read it and hence expect to
            // get NOT found error, so we can check the status of the metadata before calling indexer.
            for(String uuid : maps.keySet()) {
                try {
                    logger.info("Call indexer on metadata {} after transaction committed.", uuid);
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("uuid", uuid);

                    callApi(indexUrl, variables);
                }
                catch (Exception e1) {
                    // Must not throw exception, can only print log and handle it manually
                    logger.error("Fail to call indexer on metadata {} after transaction committed. {}",
                            uuid, e1.getMessage());
                }
                finally {
                    maps.remove(uuid);
                }
            }
        }, 0,5, TimeUnit.SECONDS);
    }

    @Override
    public void handleEvent(PersistentEventType persistentEventType, Metadata metaData) {
        if(persistentEventType == PersistentEventType.PostUpdate) {
            logger.info("{} handler for {}", persistentEventType, metaData);
            // We see same even fired multiple times, this map will combined the event into one
            // using a map with same key.
            maps.put(metaData.getUuid(), metaData);
        }
    }

    /**
     * All child override function as not always post operation
     * @param indexUrl
     * @param variables
     */
    protected void callApi(String indexUrl, Map<String, Object> variables) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey.trim());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(null, headers);
        restTemplate.postForEntity(indexUrl, request, Void.class, variables);
    }
}
