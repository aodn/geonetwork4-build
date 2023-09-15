package au.org.aodn.geonetwork4.handler;

import org.fao.geonet.events.md.MetadataPublished;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.context.support.ServletRequestHandledEvent;

public class MetadataPublishedEventHandler extends GenericEventHandler<MetadataPublished> {
    @Override
    public void onApplicationEvent(MetadataPublished event) {
        super.onApplicationEvent(event);
        super.onTransactionCommitted(null);
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
