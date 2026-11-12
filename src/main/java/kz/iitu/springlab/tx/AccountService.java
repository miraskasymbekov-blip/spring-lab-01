package kz.iitu.springlab.tx;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    /** Task A: both balance changes commit together or not at all. */
    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        Account from = repository.findById(fromId).orElseThrow();
        Account to = repository.findById(toId).orElseThrow();
        from.debit(amount);
        to.credit(amount);
        repository.save(from);
        repository.save(to);
    }

    /**
     * Task A, failure injection: the debit is flushed to the database, then an unchecked
     * exception is thrown before the credit. The rollback must undo the already flushed debit.
     */
    @Transactional
    public void transferFailAfterDebit(Long fromId, Long toId, BigDecimal amount) {
        Account from = repository.findById(fromId).orElseThrow();
        Account to = repository.findById(toId).orElseThrow();
        from.debit(amount);
        repository.saveAndFlush(from);
        throw new IllegalStateException("Simulated failure after debit, before credit");
    }

    /** Same failure, but a CHECKED exception with the default rollback rules: commits the debit. */
    @Transactional
    public void transferCheckedFailAfterDebit(Long fromId, Long toId, BigDecimal amount)
            throws TransferFailedException {
        Account from = repository.findById(fromId).orElseThrow();
        from.debit(amount);
        repository.saveAndFlush(from);
        throw new TransferFailedException("Checked failure after debit, before credit");
    }

    /** Same checked failure, but rollbackFor makes Spring roll back for it too. */
    @Transactional(rollbackFor = TransferFailedException.class)
    public void transferCheckedFailAfterDebitRollbackFor(Long fromId, Long toId, BigDecimal amount)
            throws TransferFailedException {
        Account from = repository.findById(fromId).orElseThrow();
        from.debit(amount);
        repository.saveAndFlush(from);
        throw new TransferFailedException("Checked failure after debit, before credit");
    }

    @Transactional(readOnly = true)
    public BigDecimal balanceOf(Long id) {
        return repository.findById(id).orElseThrow().getBalance();
    }
}
