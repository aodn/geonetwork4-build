package au.org.aodn.geonetwork4.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.entitylistener.GeonetworkEntityListener;
import org.fao.geonet.entitylistener.PersistentEventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.HttpServerErrorException;
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

    protected String indexUrl;

    protected String apiKey;

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

    @Autowired
    public GenericEntityListener(String apiKey, String host, String indexUrl, RestTemplate restTemplate) {

        this.apiKey = apiKey;
        this.indexUrl = host != null && !host.isEmpty() ? indexUrl : null;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        if(indexUrl == null) {
            logger.warn("Call to es-indexer is off due to config missing");
        }
        else {
            // We pick up the items in map and then post trigger indexer call, this thread keep execute every 5 secs
            service.scheduleWithFixedDelay(() -> {

                // If the updateMap contain items that is going do delete, then there is no point to update
                deleteMap.forEach((key, value) -> updateMap.remove(key));

                // Noted, our geonetwork setup never use un-publish, therefore it will be always
                // public readable.
                for (String uuid : updateMap.keySet()) {
                    boolean needRemoveFromMap = true;

                    try {
                        logger.info("Call indexer on metadata {} after metadata updated.", uuid);
                        Map<String, Object> variables = new HashMap<>();
                        variables.put(UUID, uuid);

                        callApiUpdate(indexUrl, variables);
                    } catch (HttpServerErrorException server) {
                        if (server.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                            // Error may due to indexer reboot, so we just need to keep retry
                            logger.warn("Indexer not available, will keep retry update operation");
                            needRemoveFromMap = false;
                        }
                    } catch (Exception e1) {
                        // Must not throw exception, can only print log and handle it manually
                        logger.error("Fail to call indexer on metadata {} after transaction committed. {}",
                                uuid, e1.getMessage());
                    } finally {
                        if (needRemoveFromMap) {
                            updateMap.remove(uuid);
                        }
                    }
                }

                for (String uuid : deleteMap.keySet()) {
                    boolean needRemoveFromMap = true;

                    try {
                        logger.info("Call indexer to delete metadata {} after transaction committed.", uuid);
                        Map<String, Object> variables = new HashMap<>();
                        variables.put(UUID, uuid);

                        callApiDelete(indexUrl, variables);
                    } catch (HttpServerErrorException server) {
                        if (server.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                            // Error may due to indexer reboot, so we just need to keep retry
                            logger.warn("Indexer not available, will keep retry delete operation");
                            needRemoveFromMap = false;
                        }
                    } catch (Exception e1) {
                        // Must not throw exception, can only print log and handle it manually
                        logger.error("Fail to call indexer to delete metadata {} after transaction committed. {}",
                                uuid, e1.getMessage());
                    } finally {
                        if (needRemoveFromMap) {
                            deleteMap.remove(uuid);
                        }
                    }
                }

            }, delayStart, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void handleEvent(PersistentEventType persistentEventType, Metadata metaData) {
        if(indexUrl != null) {
            if (persistentEventType == PersistentEventType.PostUpdate) {
                logger.info("PostUpdate handler for {}", metaData);
                // We see same even fired multiple times, this map will combine the event into one
                // using a map with same key.
                updateMap.put(metaData.getUuid(), metaData);
            } else if (persistentEventType == PersistentEventType.PostRemove) {
                logger.info("PostRemove handler for {}", metaData);
                // We see same even fired multiple times, this map will combine the event into one
                // using a map with same key.
                deleteMap.put(metaData.getUuid(), metaData);
            }
        }
    }
    /**
     * Call indexer rest api to update index.
     *
     * @param indexUrl - The template URL
     * @param variables - The variable for the template URL.
     */
    protected void callApiUpdate(String indexUrl, Map<String, Object> variables) {
        if(indexUrl != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", apiKey.trim());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> request = new HttpEntity<>(null, headers);
            logger.info("Call indexer update {} metadata {}", indexUrl, variables.get(UUID));
            restTemplate.postForEntity(indexUrl, request, Void.class, variables);
        }
    }
    /**
     * Call indexer rest api to delete index.
     *
     * @param indexUrl - The template URL
     * @param variables - The variable for the template URL.
     */
    protected void callApiDelete(String indexUrl, Map<String, Object> variables) {
        if(indexUrl != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", apiKey.trim());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> request = new HttpEntity<>(null, headers);
            logger.info("Call indexer delete {} metadata {}", indexUrl, variables.get(UUID));
            restTemplate.exchange(indexUrl, HttpMethod.DELETE, request, Void.class, variables);
        }
    }
}
