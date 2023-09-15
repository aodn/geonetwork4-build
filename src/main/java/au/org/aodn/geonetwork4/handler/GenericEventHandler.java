package au.org.aodn.geonetwork4.handler;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fao.geonet.events.md.MetadataEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.support.ServletRequestHandledEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

class GenericEventHandler<T extends MetadataEvent> implements ApplicationListener<T> {

    protected Logger logger = LogManager.getLogger(this.getClass());

    // Expiring map make sure unused item will be discarded
    protected Map<String, T> maps = Collections.synchronizedMap(new PassiveExpiringMap<>(5, TimeUnit.MINUTES));

    @Value("${aodn.geonetwork4.esIndexer.urlIndex}")
    protected String indexUrl;

    @Autowired
    protected RestTemplate restTemplate;

    protected String getTransactionName() {
        return this.getClass().getSimpleName() + "TransId";
    }
    /**
     * Join the same transaction, we cannot use this call directly because of generic type erase which will
     * result in T = Object if use directly.
     *
     * When this call happens, it is still in mid of transaction hence db not yet commit. If external process query
     * info, it will be outdated info. We can only trigger remote call after db commit.
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(T event) {
        if(TransactionSynchronizationManager.isActualTransactionActive() &&
                TransactionSynchronizationManager.getResource(getTransactionName()) == null) {
            // It is possible the same event delivery multiple time with same transaction, we only need to
            // record once per transaction / after transaction inactive
            String id = UUID.randomUUID().toString();
            logger.info("Set transId to {}", id);

            TransactionSynchronizationManager.bindResource(getTransactionName(), id);
            maps.put(id, event);
        }
    }
    /**
     * Only fire when REST call db updated commit, then we can call the remote REST call. Noted
     * the param type is not of type T.
     *
     * @param event
     */
    protected void onTransactionCommitted(ServletRequestHandledEvent event) {

        String id = (String) TransactionSynchronizationManager.getResource(getTransactionName());

        if (id != null && maps.containsKey(id)) {
            logger.info("Get transId to {}", id);

            T e = maps.get(id);
            try {
                logger.info("Call indexer on metadata {} after transaction committed.", e.getMd().getUuid());
                Map<String, Object> variable = new HashMap<>();
                variable.put("uuid", e.getMd().getUuid());

                restTemplate.postForEntity(indexUrl, null, String.class, variable);
            }
            catch (Exception e1) {
                // Must not throw exception, can only print log and handle it manually
                logger.error("Fail to call indexer on metadata {} after transaction committed. {}", e.getMd().getUuid(), e1.getStackTrace());
            }
            finally {
                maps.remove(id);
            }
        }
    }
    /**
     * Remove the object as no longer needed due to transaction rollback.
     * @param event
     */
    protected void onTransactionRollback(ServletRequestHandledEvent event) {
        String id = (String) TransactionSynchronizationManager.getResource(getTransactionName());
        if (id != null && maps.containsKey(id)) {
            maps.remove(id);
        }
    }
}
