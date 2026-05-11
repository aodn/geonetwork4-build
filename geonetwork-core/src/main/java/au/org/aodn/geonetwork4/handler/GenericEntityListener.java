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

    /**
     * Tracks the last changeDate string that was successfully dispatched to the external indexer
     * for each UUID. This prevents unnecessary re-indexing when only the popularity counter
     * changes (i.e. changeDate stays the same while popularity increments).
     */
    protected Map<String, String> lastIndexedChangeDateMap = new ConcurrentHashMap<>();

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
                    Metadata metadata = updateMap.get(uuid);

                    try {
                        logger.info("Call indexer on metadata {} after metadata updated.", uuid);
                        Map<String, Object> variables = new HashMap<>();
                        variables.put(UUID, uuid);

                        callApiUpdate(indexUrl, variables);

                        // Record the changeDate of the content we just sent so we can skip
                        // future PostUpdate events that only increment popularity.
                        if (metadata != null && metadata.getDataInfo() != null
                                && metadata.getDataInfo().getChangeDate() != null) {
                            lastIndexedChangeDateMap.put(uuid,
                                    metadata.getDataInfo().getChangeDate().toString());
                        }
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

                        // Remove tracking entry for deleted records
                        lastIndexedChangeDateMap.remove(uuid);
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

                String uuid = metaData.getUuid();

                // Only enqueue if the metadata content has actually changed.
                // Popularity-only increments keep changeDate the same, so we skip them
                // to avoid hammering the external indexer with no-op calls.
                if (hasContentChanged(uuid, metaData)) {
                    updateMap.put(uuid, metaData);
                } else {
                    logger.debug("Skipping external indexer call for {} - only popularity changed, changeDate unchanged",
                            uuid);
                }
            } else if (persistentEventType == PersistentEventType.PostRemove) {
                logger.info("PostRemove handler for {}", metaData);
                // We see same even fired multiple times, this map will combine the event into one
                // using a map with same key.
                deleteMap.put(metaData.getUuid(), metaData);
            }
        }
    }

    /**
     * Returns {@code true} when the metadata content has genuinely changed since the last
     * time we sent this UUID to the external indexer, or when it has never been indexed.
     * Returns {@code false} when only the popularity counter changed (changeDate identical).
     */
    protected boolean hasContentChanged(String uuid, Metadata metaData) {
        try {
            if (metaData.getDataInfo() == null || metaData.getDataInfo().getChangeDate() == null) {
                // Cannot determine changeDate — be conservative and allow indexing
                return true;
            }
            String newChangeDate = metaData.getDataInfo().getChangeDate().toString();
            String lastIndexed = lastIndexedChangeDateMap.get(uuid);
            if (lastIndexed == null) {
                // Never indexed before — always index
                return true;
            }
            return !lastIndexed.equals(newChangeDate);
        } catch (Exception e) {
            // Defensive: if anything goes wrong comparing dates, allow indexing
            logger.warn("Could not compare changeDates for {}, allowing indexer call: {}", uuid, e.getMessage());
            return true;
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
