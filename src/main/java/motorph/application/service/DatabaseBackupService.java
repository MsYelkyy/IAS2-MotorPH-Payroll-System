package motorph.application.service;

import motorph.infrastructure.config.DatabaseConnection;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Creates, lists, restores, and deletes SQL backup files for the MotorPH database. */
public class DatabaseBackupService {

    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String BACKUP_DIR = "backups";

    private final UserAccessLogService userAccessLogService;

    public DatabaseBackupService(UserAccessLogService userAccessLogService) {
        this.userAccessLogService = userAccessLogService;
    }

    // ── Backup creation ───────────────────────────────────────────────────────

    public Path createBackup(int performedByUserId) throws Exception {
        Files.createDirectories(Paths.get(BACKUP_DIR));
        String filename = "motorph_backup_" + LocalDateTime.now().format(FILE_FMT) + ".sql";
        Path backupPath = Paths.get(BACKUP_DIR, filename);

        try (Connection conn = DatabaseConnection.getConnection();
             BufferedWriter writer = Files.newBufferedWriter(backupPath)) {

            writer.write("-- MotorPH Database Backup\n");
            writer.write("-- Created: " + LocalDateTime.now() + "\n\n");
            writer.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");

            for (String table : listTables(conn)) {
                dumpTable(conn, writer, table);
            }

            writer.write("SET FOREIGN_KEY_CHECKS = 1;\n");
        }

        userAccessLogService.logDatabaseBackupCreated(performedByUserId, filename);
        return backupPath;
    }

    private List<String> listTables(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) tables.add(rs.getString("TABLE_NAME"));
        }
        return tables;
    }

    private void dumpTable(Connection conn, BufferedWriter writer, String table) throws Exception {
        writer.write("-- Table: " + table + "\n");

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
            if (rs.next()) {
                writer.write("DROP TABLE IF EXISTS `" + table + "`;\n");
                writer.write(rs.getString(2) + ";\n\n");
            }
        }

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM `" + table + "`")) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            while (rs.next()) {
                writer.write("INSERT INTO `" + table + "` VALUES (");
                for (int i = 1; i <= cols; i++) {
                    String val = rs.getString(i);
                    if (val == null) {
                        writer.write("NULL");
                    } else {
                        writer.write("'" + escapeSQL(val) + "'");
                    }
                    if (i < cols) writer.write(", ");
                }
                writer.write(");\n");
            }
            writer.write("\n");
        }
    }

    private String escapeSQL(String val) {
        return val.replace("\\", "\\\\")
                  .replace("'", "\\'")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\0", "\\0");
    }

    // ── Restore ───────────────────────────────────────────────────────────────

    public void restoreBackup(Path backupFile, int performedByUserId) throws Exception {
        String content = Files.readString(backupFile);

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (String stmt : splitStatements(content)) {
                    try (Statement s = conn.createStatement()) {
                        s.execute(stmt);
                    }
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }

        userAccessLogService.logDatabaseRestore(performedByUserId, backupFile.getFileName().toString());
    }

    private List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (inString) {
                if (c == '\\') {
                    current.append(c);
                    if (i + 1 < sql.length()) current.append(sql.charAt(++i));
                } else if (c == stringChar) {
                    inString = false;
                    current.append(c);
                } else {
                    current.append(c);
                }
            } else if (c == '\'' || c == '"' || c == '`') {
                inString = true;
                stringChar = c;
                current.append(c);
            } else if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                while (i < sql.length() && sql.charAt(i) != '\n') i++;
            } else if (c == ';') {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) statements.add(stmt);
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        String last = current.toString().trim();
        if (!last.isEmpty()) statements.add(last);
        return statements;
    }

    // ── Listing & deletion ────────────────────────────────────────────────────

    public List<BackupEntry> listBackups() throws IOException {
        Path dir = Paths.get(BACKUP_DIR);
        if (!Files.exists(dir)) return Collections.emptyList();

        List<BackupEntry> entries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.sql")) {
            for (Path p : stream) {
                BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                entries.add(new BackupEntry(p.toAbsolutePath(), attrs.size(), attrs.creationTime().toInstant()));
            }
        }
        entries.sort(Comparator.comparing((BackupEntry e) -> e.createdAt).reversed());
        return entries;
    }

    public void deleteBackup(Path backupFile) throws IOException {
        Files.deleteIfExists(backupFile);
    }

    // ── BackupEntry ───────────────────────────────────────────────────────────

    public static class BackupEntry {
        public final Path path;
        public final long sizeBytes;
        public final Instant createdAt;

        public BackupEntry(Path path, long sizeBytes, Instant createdAt) {
            this.path = path;
            this.sizeBytes = sizeBytes;
            this.createdAt = createdAt;
        }

        public String getFileName() {
            return path.getFileName().toString();
        }

        public String getFormattedSize() {
            if (sizeBytes < 1024) return sizeBytes + " B";
            if (sizeBytes < 1024 * 1024) return String.format("%.1f KB", sizeBytes / 1024.0);
            return String.format("%.2f MB", sizeBytes / (1024.0 * 1024));
        }

        public String getFormattedDate() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");
            return fmt.format(createdAt.atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
    }
}
