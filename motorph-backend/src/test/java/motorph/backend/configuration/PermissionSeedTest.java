package motorph.backend.configuration;

import motorph.backend.domain.value.PermissionKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Makes sure every permission key used in the code also exists in the database.
 * This checks both the database dump and the migration used for existing databases.
 */
class PermissionSeedTest {

    private static final Path DATABASE_DIR = Paths.get("..", "database");
    private static final Path DUMP = DATABASE_DIR.resolve("payrollsystem_db.sql");
    private static final Path MIGRATION = DATABASE_DIR.resolve("sql_scripts").resolve("support_ticket_permissions.sql");
    private static final Path PAYSLIP_MIGRATION =
            DATABASE_DIR.resolve("sql_scripts").resolve("payslip_view_all_permission.sql");

    private static final Set<String> ROLES_WITH_VIEW_ALL_PAYSLIPS = Set.of("ADMIN", "FINANCE", "HR");
    private static final Set<String> ALL_ROLES = Set.of("ADMIN", "HR", "FINANCE", "IT", "EMPLOYEE");

    private static final Map<String, Set<PermissionKey>> EXPECTED_TICKET_GRANTS = Map.of(
            "ADMIN", Set.of(PermissionKey.VIEW_TICKETS, PermissionKey.RESOLVE_TICKETS,
                    PermissionKey.CANCEL_TICKETS, PermissionKey.VIEW_ALL_TICKETS),
            "HR", Set.of(PermissionKey.VIEW_TICKETS, PermissionKey.RESOLVE_TICKETS),
            "FINANCE", Set.of(PermissionKey.VIEW_TICKETS, PermissionKey.RESOLVE_TICKETS),
            "IT", Set.of(PermissionKey.VIEW_TICKETS, PermissionKey.RESOLVE_TICKETS),
            "EMPLOYEE", Set.of());

    private static final Set<PermissionKey> TICKET_KEYS = Set.of(
            PermissionKey.VIEW_TICKETS, PermissionKey.RESOLVE_TICKETS,
            PermissionKey.CANCEL_TICKETS, PermissionKey.VIEW_ALL_TICKETS);

    @Test
    @DisplayName("The database dump seeds every permission key the backend defines")
    void dumpSeedsEveryPermissionKey() throws IOException {
        Map<String, Integer> idsByKey = permissionIds(read(DUMP));

        for (PermissionKey key : PermissionKey.values()) {
            assertThat(idsByKey).describedAs("permission %s in payrollsystem_db.sql", key).containsKey(key.name());
        }
    }

    @Test
    @DisplayName("The database dump grants ticket permissions exactly as the ticket policy requires")
    void dumpGrantsTicketPermissionsPerPolicy() throws IOException {
        String dump = read(DUMP);
        Map<String, Integer> idsByKey = permissionIds(dump);
        Map<String, Integer> roleIds = roleIds(dump);
        Set<String> grants = rolePermissionPairs(dump);

        for (Map.Entry<String, Set<PermissionKey>> expected : EXPECTED_TICKET_GRANTS.entrySet()) {
            Integer roleId = roleIds.get(expected.getKey());
            assertThat(roleId).describedAs("role %s", expected.getKey()).isNotNull();

            for (PermissionKey key : TICKET_KEYS) {
                boolean granted = grants.contains(roleId + ":" + idsByKey.get(key.name()));

                assertThat(granted)
                        .describedAs("%s granted %s", expected.getKey(), key)
                        .isEqualTo(expected.getValue().contains(key));
            }
        }
    }

    @Test
    @DisplayName("The migration for existing databases only adds ticket permissions")
    void migrationOnlyAddsTicketPermissions() throws IOException {
        String migration = read(MIGRATION);
        String statements = migration.replaceAll("(?m)^--.*$", "").toUpperCase();

        for (PermissionKey key : TICKET_KEYS) {
            assertThat(migration).contains("'" + key.name() + "'");
        }

        assertThat(statements)
                .doesNotContain("DELETE ")
                .doesNotContain("DROP ")
                .doesNotContain("TRUNCATE ")
                .doesNotContain("UPDATE ")
                .doesNotContain("REPLACE ");
        assertThat(statements).contains("NOT EXISTS");
    }

    @Test
    @DisplayName("The database dump grants VIEW_ALL_PAYSLIPS to Admin, Finance and HR only")
    void dumpGrantsViewAllPayslipsOnlyToAdminFinanceAndHr() throws IOException {
        String dump = read(DUMP);
        Map<String, Integer> idsByKey = permissionIds(dump);
        Map<String, Integer> roleIds = roleIds(dump);
        Set<String> grants = rolePermissionPairs(dump);

        for (String role : ALL_ROLES) {
            boolean granted = grants.contains(roleIds.get(role) + ":" + idsByKey.get(PermissionKey.VIEW_ALL_PAYSLIPS.name()));

            assertThat(granted)
                    .describedAs("%s granted VIEW_ALL_PAYSLIPS", role)
                    .isEqualTo(ROLES_WITH_VIEW_ALL_PAYSLIPS.contains(role));
        }
    }

    @Test
    @DisplayName("The database dump still grants VIEW_PAYSLIP to every role, so everyone sees their own payslips")
    void dumpGrantsViewPayslipToEveryRole() throws IOException {
        String dump = read(DUMP);
        Map<String, Integer> idsByKey = permissionIds(dump);
        Map<String, Integer> roleIds = roleIds(dump);
        Set<String> grants = rolePermissionPairs(dump);

        for (String role : ALL_ROLES) {
            assertThat(grants)
                    .describedAs("%s granted VIEW_PAYSLIP", role)
                    .contains(roleIds.get(role) + ":" + idsByKey.get(PermissionKey.VIEW_PAYSLIP.name()));
        }
    }

    @Test
    @DisplayName("The payslip migration only adds VIEW_ALL_PAYSLIPS and grants it to Admin, Finance and HR, and can be rerun")
    void payslipMigrationIsAdditiveIdempotentAndScopedToAdminFinanceAndHr() throws IOException {
        String migration = read(PAYSLIP_MIGRATION);
        String statements = migration.replaceAll("(?m)^--.*$", "").toUpperCase();

        assertThat(migration)
                .contains("'VIEW_ALL_PAYSLIPS'")
                .contains("IN ('ADMIN', 'FINANCE', 'HR')")
                .doesNotContain("'IT'")
                .doesNotContain("'EMPLOYEE'");
        assertThat(statements)
                .doesNotContain("DELETE ")
                .doesNotContain("DROP ")
                .doesNotContain("TRUNCATE ")
                .doesNotContain("UPDATE ")
                .doesNotContain("REPLACE ");
        assertThat(statements.split("NOT EXISTS", -1).length - 1).isEqualTo(2);
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static Map<String, Integer> permissionIds(String dump) {
        Map<String, Integer> ids = new HashMap<>();
        Matcher matcher = Pattern.compile("\\((\\d+),'([A-Z0-9_]+)','[^']*','[^']*','[^']*',")
                .matcher(insertStatement(dump, "permission"));

        while (matcher.find()) {
            ids.put(matcher.group(2), Integer.parseInt(matcher.group(1)));
        }

        return ids;
    }

    private static Map<String, Integer> roleIds(String dump) {
        Map<String, Integer> ids = new HashMap<>();
        Matcher matcher = Pattern.compile("\\((\\d+),'([A-Z]+)','")
                .matcher(insertStatement(dump, "role"));

        while (matcher.find()) {
            ids.put(matcher.group(2), Integer.parseInt(matcher.group(1)));
        }

        return ids;
    }

    private static Set<String> rolePermissionPairs(String dump) {
        Set<String> pairs = new HashSet<>();
        Matcher matcher = Pattern.compile("\\(\\d+,(\\d+),(\\d+),'")
                .matcher(insertStatement(dump, "role_permission"));

        while (matcher.find()) {
            pairs.add(matcher.group(1) + ":" + matcher.group(2));
        }

        return pairs;
    }

    private static String insertStatement(String dump, String table) {
        String prefix = "INSERT INTO `" + table + "` VALUES ";
        int start = dump.indexOf(prefix);
        assertThat(start).describedAs("INSERT INTO %s in payrollsystem_db.sql", table).isNotNegative();

        int end = dump.indexOf(";\n", start);
        if (end < 0) {
            end = dump.indexOf(";\r\n", start);
        }

        return dump.substring(start, end);
    }
}
