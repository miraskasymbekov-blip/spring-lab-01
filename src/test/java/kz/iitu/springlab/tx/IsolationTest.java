package kz.iitu.springlab.tx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static kz.iitu.springlab.tx.TxTestSupport.ALICE;
import static kz.iitu.springlab.tx.TxTestSupport.balance;
import static kz.iitu.springlab.tx.TxTestSupport.print;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task C. Two independent transactions (two threads, two connections) work with the same row.
 * T1 reads Alice's balance, T2 changes it, T1 reads it again. Reads go through JdbcTemplate so
 * Hibernate's first-level cache cannot hide what the database returns.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.DisplayName.class)
class IsolationTest {

    @Autowired PlatformTransactionManager txManager;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        TxTestSupport.resetAccounts(jdbc);
        print("database: H2 " + jdbc.queryForObject("SELECT H2VERSION()", String.class)
                + ", default isolation = " + sessionIsolation());
    }

    private TransactionTemplate template(int isolation) {
        TransactionTemplate t = new TransactionTemplate(txManager);
        t.setIsolationLevel(isolation);
        return t;
    }

    /** What the database itself reports about the session that is running T1. */
    private String sessionIsolation() {
        return jdbc.queryForObject(
                "SELECT ISOLATION_LEVEL FROM INFORMATION_SCHEMA.SESSIONS WHERE SESSION_ID = SESSION_ID()", String.class);
    }

    private BigDecimal read() {
        return jdbc.queryForObject("SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, ALICE);
    }

    /** T2: sets Alice's balance to 900.00; optionally waits before committing. */
    private CompletableFuture<Void> updateInOtherTransaction(CountDownLatch updated, CountDownLatch mayCommit) {
        return CompletableFuture.runAsync(() -> template(TransactionDefinition.ISOLATION_READ_COMMITTED).executeWithoutResult(s -> {
            jdbc.update("UPDATE accounts SET balance = 900.00 WHERE id = ?", ALICE);
            updated.countDown();
            await(mayCommit);
        }));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Non-repeatable read: T1 reads, T2 updates AND COMMITS, T1 reads again. Returns {first, second}. */
    private BigDecimal[] nonRepeatableRead(int isolation, String label) {
        print("=== " + label + " ===");
        BigDecimal[] result = new BigDecimal[2];
        template(isolation).executeWithoutResult(s -> {
            print("T1 started, database says isolation = " + sessionIsolation());
            result[0] = read();
            print("T1 first read : Alice = " + result[0]);

            CountDownLatch updated = new CountDownLatch(1);
            CountDownLatch go = new CountDownLatch(0);
            updateInOtherTransaction(updated, go).join();
            print("T2 updated Alice to 900.00 and COMMITTED");

            result[1] = read();
            print("T1 second read: Alice = " + result[1] + (result[0].compareTo(result[1]) == 0
                    ? "   (same value: repeatable read)" : "   (CHANGED: non-repeatable read)"));
        });
        return result;
    }

    @Test
    @DisplayName("C1: READ_COMMITTED sees the value committed by another transaction")
    void readCommitted() {
        BigDecimal[] r = nonRepeatableRead(TransactionDefinition.ISOLATION_READ_COMMITTED, "C1: READ_COMMITTED");
        assertThat(r[0]).isEqualByComparingTo("1000.00");
        assertThat(r[1]).isEqualByComparingTo("900.00");
    }

    @Test
    @DisplayName("C2: REPEATABLE_READ keeps returning the snapshot value")
    void repeatableRead() {
        BigDecimal[] r = nonRepeatableRead(TransactionDefinition.ISOLATION_REPEATABLE_READ, "C2: REPEATABLE_READ");
        assertThat(r[0]).isEqualByComparingTo("1000.00");
        assertThat(r[1]).isEqualByComparingTo("1000.00");
        print("after T1 finished, committed value in DB: Alice = " + balance(jdbc, ALICE));
    }

    @Test
    @DisplayName("C3: READ_UNCOMMITTED on H2 2.x really allows a dirty read")
    void dirtyRead() {
        print("=== C3: dirty read attempt with READ_UNCOMMITTED ===");
        CountDownLatch updated = new CountDownLatch(1);
        CountDownLatch mayCommit = new CountDownLatch(1);
        CompletableFuture<Void> t2 = updateInOtherTransaction(updated, mayCommit);
        await(updated);
        print("T2 updated Alice to 900.00, NOT committed yet");

        BigDecimal seen = template(TransactionDefinition.ISOLATION_READ_UNCOMMITTED).execute(s -> {
            print("T1 started, database says isolation = " + sessionIsolation());
            BigDecimal v = read();
            print("T1 read       : Alice = " + v + (v.compareTo(new BigDecimal("900.00")) == 0
                    ? "   (DIRTY READ)" : "   (uncommitted change NOT visible)"));
            return v;
        });
        mayCommit.countDown();
        t2.join();
        print("T2 committed, Alice = " + balance(jdbc, ALICE));
        // observed on H2 2.4: unlike its MVStore predecessors, it honours READ_UNCOMMITTED
        assertThat(seen).isEqualByComparingTo("900.00");
    }
}
