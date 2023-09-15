package au.org.aodn.geonetwork4.handler;

import org.fao.geonet.events.md.MetadataUpdate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.context.support.ServletRequestHandledEvent;

public class MetadataUpdateEventHandler extends GenericEventHandler<MetadataUpdate> {
    @Override
    public void onApplicationEvent(MetadataUpdate event) {
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
