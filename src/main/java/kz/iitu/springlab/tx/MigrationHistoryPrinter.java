package kz.iitu.springlab.tx;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Task E: prints Flyway's flyway_schema_history table at startup. */
@Component
@ConditionalOnProperty(name = "lab.print-migrations", havingValue = "true", matchIfMissing = true)
public class MigrationHistoryPrinter implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public MigrationHistoryPrinter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT \"installed_rank\", \"version\", \"description\", \"script\", \"checksum\", \"success\" "
                        + "FROM \"flyway_schema_history\" WHERE \"installed_rank\" > 0 ORDER BY \"installed_rank\"");
        System.out.println();
        System.out.println("[MIGRATIONS] flyway_schema_history");
        System.out.printf("[MIGRATIONS] %-5s %-8s %-22s %-28s %-12s %s%n",
                "rank", "version", "description", "script", "checksum", "success");
        for (Map<String, Object> r : rows) {
            System.out.printf("[MIGRATIONS] %-5s %-8s %-22s %-28s %-12s %s%n",
                    r.get("installed_rank"), r.get("version"), r.get("description"),
                    r.get("script"), r.get("checksum"), r.get("success"));
        }
        System.out.println();
    }
}
