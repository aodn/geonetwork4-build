package au.org.aodn.geonetwork4.handler;

import org.fao.geonet.events.md.MetadataUnpublished;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.context.support.ServletRequestHandledEvent;

/**
 * The event fire when user click "manage record" and un-publish the record.
 */
public class MetadataUnPublishedEventHandler extends GenericEventHandler<MetadataUnpublished> {
    @Override
    public void onApplicationEvent(MetadataUnpublished event) {
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
