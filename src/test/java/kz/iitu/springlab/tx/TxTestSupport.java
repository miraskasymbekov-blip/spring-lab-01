package kz.iitu.springlab.tx;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

/** Helpers shared by the lab tests: reset the seed data and print screenshot-friendly lines. */
final class TxTestSupport {

    static final long ALICE = 1L;
    static final long BOB = 2L;

    private TxTestSupport() { }

    static void resetAccounts(JdbcTemplate jdbc) {
        jdbc.update("UPDATE accounts SET balance = 1000.00, version = 0, status = 'ACTIVE' WHERE id = ?", ALICE);
        jdbc.update("UPDATE accounts SET balance = 500.00, version = 0, status = 'ACTIVE' WHERE id = ?", BOB);
    }

    static BigDecimal balance(JdbcTemplate jdbc, long id) {
        return jdbc.queryForObject("SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, id);
    }

    static void print(String message) {
        System.out.println("[TX] " + message);
    }

    static void printBalances(JdbcTemplate jdbc, String when) {
        print(String.format("%-28s Alice = %8s   Bob = %8s", when, balance(jdbc, ALICE), balance(jdbc, BOB)));
    }
}
