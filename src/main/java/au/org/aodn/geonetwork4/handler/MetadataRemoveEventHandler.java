package au.org.aodn.geonetwork4.handler;

import org.fao.geonet.events.md.MetadataRemove;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.context.support.ServletRequestHandledEvent;

import java.util.Map;

public class MetadataRemoveEventHandler extends GenericEventHandler<MetadataRemove> {

    @Override
    protected void callApi(String indexUrl, Map<String, Object> variables) {
        restTemplate.delete(indexUrl, variables);
    }

    @Override
    public void onApplicationEvent(MetadataRemove event) {
        super.onApplicationEvent(event);
    }

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCommitted(ServletRequestHandledEvent event) {
        super.onTransactionCommitted(event);
    }

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onTransactionRollback(ServletRequestHandledEvent event) {
        super.onTransactionRollback(event);
    }
}
