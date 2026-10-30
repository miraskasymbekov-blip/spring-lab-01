package kz.iitu.springlab.tx;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Task B: the inner method. It lives in its own bean so the call goes through the proxy. */
@Service
public class AuditRecordService {

    private final AuditRecordRepository repository;

    public AuditRecordService(AuditRecordRepository repository) {
        this.repository = repository;
    }

    /** Joins the caller's transaction: the audit row commits or rolls back together with it. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void writeRequired(String message) {
        repository.saveAndFlush(new AuditRecord(message));
    }

    /** Suspends the caller's transaction and commits the audit row in an independent one. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeRequiresNew(String message) {
        repository.saveAndFlush(new AuditRecord(message));
    }
}
