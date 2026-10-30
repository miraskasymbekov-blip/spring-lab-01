package kz.iitu.springlab.tx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static kz.iitu.springlab.tx.TxTestSupport.ALICE;
import static kz.iitu.springlab.tx.TxTestSupport.balance;
import static kz.iitu.springlab.tx.TxTestSupport.print;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Task B. Not @Transactional: the outer service method owns the transaction. */
@SpringBootTest
@TestMethodOrder(MethodOrderer.DisplayName.class)
class PropagationTest {

    private static final BigDecimal AMOUNT = new BigDecimal("100.00");

    @Autowired TransferWithAuditService outer;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        TxTestSupport.resetAccounts(jdbc);
        jdbc.update("DELETE FROM audit_log");
    }

    private int auditRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class);
    }

    private void report(String when) {
        print(String.format("%-22s Alice = %8s   audit_log rows = %d", when, balance(jdbc, ALICE), auditRows()));
    }

    @Test
    @DisplayName("B1: REQUIRED inner, outer succeeds -> debit and audit both committed")
    void requiredOuterSucceeds() {
        print("=== B1: inner REQUIRED, outer succeeds ===");
        report("before:");
        outer.debitWithRequiredAudit(ALICE, AMOUNT, false);
        report("after commit:");
        assertThat(balance(jdbc, ALICE)).isEqualByComparingTo("900.00");
        assertThat(auditRows()).isEqualTo(1);
    }

    @Test
    @DisplayName("B2: REQUIRED inner, outer fails -> audit row is rolled back too")
    void requiredOuterFails() {
        print("=== B2: inner REQUIRED, outer fails after inner returned ===");
        report("before:");
        assertThatThrownBy(() -> outer.debitWithRequiredAudit(ALICE, AMOUNT, true))
                .isInstanceOf(IllegalStateException.class);
        print("outer method failed: IllegalStateException");
        report("after rollback:");
        assertThat(balance(jdbc, ALICE)).isEqualByComparingTo("1000.00");
        assertThat(auditRows()).isZero();
    }

    @Test
    @DisplayName("B3: REQUIRES_NEW inner, outer fails -> audit row SURVIVES, debit rolled back")
    void requiresNewOuterFails() {
        print("=== B3: inner REQUIRES_NEW, outer fails after inner returned ===");
        report("before:");
        assertThatThrownBy(() -> outer.debitWithRequiresNewAudit(ALICE, AMOUNT, true))
                .isInstanceOf(IllegalStateException.class);
        print("outer method failed: IllegalStateException");
        report("after rollback:");
        assertThat(balance(jdbc, ALICE)).isEqualByComparingTo("1000.00");
        assertThat(auditRows()).isEqualTo(1);
    }
}
