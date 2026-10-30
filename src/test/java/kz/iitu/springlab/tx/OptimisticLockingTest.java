package kz.iitu.springlab.tx;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static kz.iitu.springlab.tx.TxTestSupport.ALICE;
import static kz.iitu.springlab.tx.TxTestSupport.balance;
import static kz.iitu.springlab.tx.TxTestSupport.print;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Task D. Two copies of the same row are loaded in two separate transactions (persistence contexts). */
@SpringBootTest
@TestMethodOrder(MethodOrderer.DisplayName.class)
class OptimisticLockingTest {

    @Autowired AccountRepository repository;
    @Autowired PlatformTransactionManager txManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory emf;

    private TransactionTemplate tx;

    @BeforeEach
    void reset() {
        TxTestSupport.resetAccounts(jdbc);
        tx = new TransactionTemplate(txManager);
    }

    private long dbVersion() {
        return jdbc.queryForObject("SELECT version FROM accounts WHERE id = ?", Long.class, ALICE);
    }

    private Account load() {
        return tx.execute(s -> repository.findById(ALICE).orElseThrow());
    }

    private void saveInOwnTransaction(Account account) {
        tx.executeWithoutResult(s -> repository.saveAndFlush(account));
    }

    private void state(String when) {
        print(String.format("%-34s DB: Alice = %8s, version = %d", when, balance(jdbc, ALICE), dbVersion()));
    }

    @Test
    @DisplayName("D1: the stale second copy cannot overwrite the first update")
    void staleUpdateIsRejected() {
        print("=== D1: two copies of Alice, update the first, then the stale second ===");
        Account first = load();
        Account second = load();
        print("loaded copy 1 and copy 2:  balance = " + first.getBalance() + ", version = " + first.getVersion());
        state("initial state:");

        first.credit(new BigDecimal("50.00"));
        saveInOwnTransaction(first);
        print("copy 1 credited +50.00 and COMMITTED");
        state("after first commit:");

        second.credit(new BigDecimal("200.00"));
        print("copy 2 (stale, version " + second.getVersion() + ") credited +200.00, trying to commit...");
        assertThatThrownBy(() -> saveInOwnTransaction(second))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .satisfies(e -> print("CONFLICT: " + e.getClass().getSimpleName()));
        state("after rejected second commit:");

        assertThat(balance(jdbc, ALICE)).isEqualByComparingTo("1050.00");
        assertThat(dbVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("D2: safe response - reload the fresh row and repeat the (idempotent-on-failure) operation")
    void reloadAndRetry() {
        print("=== D2: conflict handling: refresh + retry ===");
        Account first = load();
        Account second = load();

        first.credit(new BigDecimal("50.00"));
        saveInOwnTransaction(first);
        print("copy 1 credited +50.00 and COMMITTED");

        BigDecimal deposit = new BigDecimal("200.00");
        int attempt = 0;
        Account current = second;
        while (true) {
            attempt++;
            try {
                current.credit(deposit);
                saveInOwnTransaction(current);
                print("attempt " + attempt + ": deposit +200.00 committed");
                break;
            } catch (OptimisticLockingFailureException e) {
                print("attempt " + attempt + ": conflict (nothing was applied, the transaction rolled back); reloading");
                current = load();
                print("reloaded: balance = " + current.getBalance() + ", version = " + current.getVersion());
            }
        }
        state("final state:");
        assertThat(attempt).isEqualTo(2);
        assertThat(balance(jdbc, ALICE)).isEqualByComparingTo("1250.00");
        assertThat(dbVersion()).isEqualTo(2L);
    }

    @Test
    @DisplayName("D3: two open persistence contexts, the second UPDATE ... WHERE version=? matches 0 rows")
    void twoOpenPersistenceContexts() {
        print("=== D3: two EntityManagers, both transactions open at the same time ===");
        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();
        try {
            em1.getTransaction().begin();
            em2.getTransaction().begin();
            Account a1 = em1.find(Account.class, ALICE);
            Account a2 = em2.find(Account.class, ALICE);
            print("both loaded Alice: version = " + a1.getVersion() + " / " + a2.getVersion());

            a1.credit(new BigDecimal("50.00"));
            em1.getTransaction().commit();
            print("transaction 1 changed Alice and COMMITTED");
            state("after first commit:");

            a2.credit(new BigDecimal("200.00"));
            print("transaction 2 changed its stale copy, committing...");
            assertThatThrownBy(() -> em2.getTransaction().commit())
                    .satisfies(e -> print("CONFLICT: " + e.getClass().getSimpleName() + " -> "
                            + e.getCause().getClass().getSimpleName()))
                    .isInstanceOf(jakarta.persistence.RollbackException.class)
                    .hasCauseInstanceOf(OptimisticLockException.class);
            state("after rejected second commit:");
            assertThat(balance(jdbc, ALICE)).isEqualByComparingTo("1050.00");
        } finally {
            if (em1.getTransaction().isActive()) em1.getTransaction().rollback();
            if (em2.getTransaction().isActive()) em2.getTransaction().rollback();
            em1.close();
            em2.close();
        }
    }
}
