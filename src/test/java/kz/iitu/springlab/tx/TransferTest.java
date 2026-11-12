package kz.iitu.springlab.tx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static kz.iitu.springlab.tx.TxTestSupport.ALICE;
import static kz.iitu.springlab.tx.TxTestSupport.BOB;
import static kz.iitu.springlab.tx.TxTestSupport.balance;
import static kz.iitu.springlab.tx.TxTestSupport.print;
import static kz.iitu.springlab.tx.TxTestSupport.printBalances;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Task A. Deliberately NOT @Transactional: the service's own transaction boundary is what is tested. */
@SpringBootTest
@TestMethodOrder(MethodOrderer.DisplayName.class)
class TransferTest {

    @Autowired AccountService service;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        TxTestSupport.resetAccounts(jdbc);
    }

    @Test
    @DisplayName("A1: successful transfer of 100.00 from Alice to Bob")
    void successfulTransfer() {
        print("=== A1: transfer 100.00 Alice -> Bob ===");
        printBalances(jdbc, "before:");

        service.transfer(ALICE, BOB, new BigDecimal("100.00"));

        printBalances(jdbc, "after commit:");
        assertThat(balance(jdbc, ALICE)).isEqualByComparingTo("900.00");
        assertThat(balance(jdbc, BOB)).isEqualByComparingTo("600.00");
    }

    @Test
    @DisplayName("A2: transfer above Alice's balance fails, nothing changes")
    void insufficientFunds() {
        print("=== A2: transfer 5000.00 Alice -> Bob (more than Alice has) ===");
        printBalances(jdbc, "before:");

        assertThatThrownBy(() -> service.transfer(ALICE, BOB, new BigDecimal("5000.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient funds");

        print("operation failed: IllegalStateException: Insufficient funds");
        printBalances(jdbc, "after failure:");
        assertThat(balance(jdbc, ALICE)).isEqualByComparingTo("1000.00");
        assertThat(balance(jdbc, BOB)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("A3: unchecked exception after the debit rolls the whole transaction back")
    void uncheckedExceptionRollsBack() {
        print("=== A3: unchecked exception after debit (debit already flushed to the DB) ===");
        printBalances(jdbc, "before:");

        assertThatThrownBy(() -> service.transferFailAfterDebit(ALICE, BOB, new BigDecimal("100.00")))
                .isInstanceOf(IllegalStateException.class);

        print("operation failed: IllegalStateException (unchecked)");
        printBalances(jdbc, "after rollback:");
        assertThat(balance(jdbc, ALICE)).isEqualByComparingTo("1000.00");
        assertThat(balance(jdbc, BOB)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("A4a: checked exception, default rules -> NO rollback, the debit is committed")
    void checkedExceptionCommitsByDefault() {
        print("=== A4a: checked exception, plain @Transactional ===");
        printBalances(jdbc, "before:");

        assertThatThrownBy(() -> service.transferCheckedFailAfterDebit(ALICE, BOB, new BigDecimal("100.00")))
                .isInstanceOf(TransferFailedException.class);

        print("operation failed: TransferFailedException (checked)");
        printBalances(jdbc, "after (NOT rolled back):");
        assertThat(balance(jdbc, ALICE)).isEqualByComparingTo("900.00");
        assertThat(balance(jdbc, BOB)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("A4b: checked exception + rollbackFor -> rolled back")
    void checkedExceptionWithRollbackFor() {
        print("=== A4b: checked exception, @Transactional(rollbackFor = TransferFailedException.class) ===");
        printBalances(jdbc, "before:");

        assertThatThrownBy(() -> service.transferCheckedFailAfterDebitRollbackFor(ALICE, BOB, new BigDecimal("100.00")))
                .isInstanceOf(TransferFailedException.class);

        print("operation failed: TransferFailedException (checked)");
        printBalances(jdbc, "after rollback:");
        assertThat(balance(jdbc, ALICE)).isEqualByComparingTo("1000.00");
        assertThat(balance(jdbc, BOB)).isEqualByComparingTo("500.00");
    }
}
