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
import org.springframework.http.HttpMethod;
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
    protected Map<String, Metadata> updateMap = new ConcurrentHashMap<>();

    protected Map<String, Metadata> deleteMap = new ConcurrentHashMap<>();

    protected ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    protected static final String UUID = "uuid";

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

    protected int delayStart = 5;

    @PostConstruct
    public void init() {
        // We pick up the items in map and then post trigger indexer call, this thread keep execute every 5 secs
        service.scheduleWithFixedDelay(() -> {
            logger.info("Execute batch of update/delete after time elapsed");

            // If the updateMap contain items that is going do delete, then there is no point to update
            deleteMap.forEach((key, value) -> updateMap.remove(key));

            // Noted, our geonetwork setup never use un-publish, therefore it will be always
            // public readable.
            for(String uuid : updateMap.keySet()) {
                try {
                    logger.info("Call indexer on metadata {} after metadata updated.", uuid);
                    Map<String, Object> variables = new HashMap<>();
                    variables.put(UUID, uuid);

                    callApiUpdate(indexUrl, variables);
                }
                catch (Exception e1) {
                    // Must not throw exception, can only print log and handle it manually
                    logger.error("Fail to call indexer on metadata {} after transaction committed. {}",
                            uuid, e1.getMessage());
                }
                finally {
                    updateMap.remove(uuid);
                }
            }

            for(String uuid : deleteMap.keySet()) {
                try {
                    logger.info("Call indexer to delete metadata {} after transaction committed.", uuid);
                    Map<String, Object> variables = new HashMap<>();
                    variables.put(UUID, uuid);

                    callApiDelete(indexUrl, variables);
                }
                catch (Exception e1) {
                    // Must not throw exception, can only print log and handle it manually
                    logger.error("Fail to call indexer to delete metadata {} after transaction committed. {}",
                            uuid, e1.getMessage());
                }
                finally {
                    deleteMap.remove(uuid);
                }
            }

        }, delayStart,10, TimeUnit.SECONDS);
    }

    @Override
    public void handleEvent(PersistentEventType persistentEventType, Metadata metaData) {
        if(persistentEventType == PersistentEventType.PostUpdate) {
            logger.info("{} handler for {}", persistentEventType, metaData);
            // We see same even fired multiple times, this map will combine the event into one
            // using a map with same key.
            updateMap.put(metaData.getUuid(), metaData);
        }
        else if(persistentEventType == PersistentEventType.PostRemove) {
            logger.info("{} handler for {}", persistentEventType, metaData);
            // We see same even fired multiple times, this map will combine the event into one
            // using a map with same key.
            deleteMap.put(metaData.getUuid(), metaData);
        }
    }
    /**
     * Call indexer rest api to update index.
     *
     * @param indexUrl - The template URL
     * @param variables - The variable for the template URL.
     */
    protected void callApiUpdate(String indexUrl, Map<String, Object> variables) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey.trim());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(null, headers);
        logger.info("Call indexer to update metadata {}", variables.get(UUID));
        restTemplate.postForEntity(indexUrl, request, Void.class, variables);
    }
    /**
     * Call indexer rest api to delete index.
     *
     * @param indexUrl - The template URL
     * @param variables - The variable for the template URL.
     */
    protected void callApiDelete(String indexUrl, Map<String, Object> variables) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey.trim());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(null, headers);
        logger.info("Call indexer to delete metadata {}", variables.get(UUID));
        restTemplate.exchange(indexUrl, HttpMethod.DELETE, request, Void.class, variables);
    }
}
