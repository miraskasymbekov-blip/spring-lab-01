package kz.iitu.springlab.tx;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Task B: the outer method. Debits Alice, writes an audit record, then optionally fails. */
@Service
public class TransferWithAuditService {

    private final AccountRepository accounts;
    private final AuditRecordService audit;

    public TransferWithAuditService(AccountRepository accounts, AuditRecordService audit) {
        this.accounts = accounts;
        this.audit = audit;
    }

    @Transactional
    public void debitWithRequiredAudit(Long accountId, BigDecimal amount, boolean failAfterAudit) {
        Account account = accounts.findById(accountId).orElseThrow();
        account.debit(amount);
        accounts.saveAndFlush(account);
        audit.writeRequired("debit " + amount + " from account " + accountId);
        if (failAfterAudit) throw new IllegalStateException("Outer method fails after the inner method returned");
    }

    @Transactional
    public void debitWithRequiresNewAudit(Long accountId, BigDecimal amount, boolean failAfterAudit) {
        Account account = accounts.findById(accountId).orElseThrow();
        account.debit(amount);
        accounts.saveAndFlush(account);
        audit.writeRequiresNew("debit " + amount + " from account " + accountId);
        if (failAfterAudit) throw new IllegalStateException("Outer method fails after the inner method returned");
    }
}
