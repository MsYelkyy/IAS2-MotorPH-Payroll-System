package motorph.backend.service;

import motorph.backend.dto.system.BackupFileDTO;
import motorph.backend.exception.DataAccessException;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.ValidationException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Creates, lists, restores, and deletes SQL backup files stored in a configured directory on the
 * backend host.
 *
 * <p>Filenames arrive from remote callers, so each one must match the exact pattern this service
 * generates before the filesystem is touched. This stops {@code ..} and absolute paths from
 * escaping the backup directory.
 */
@Service
public class DatabaseBackupService {

    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String FILE_PREFIX = "motorph_backup_";
    private static final Pattern BACKUP_FILENAME =
            Pattern.compile("^motorph_backup_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.sql$");

    private final DataSource dataSource;
    private final UserAccessLogService userAccessLogService;
    private final Path backupDirectory;

    public DatabaseBackupService(DataSource dataSource,
                                 UserAccessLogService userAccessLogService,
                                 @Value("${motorph.backup.directory:backups}") String backupDirectory) {
        this.dataSource = dataSource;
        this.userAccessLogService = userAccessLogService;
        this.backupDirectory = Paths.get(backupDirectory).toAbsolutePath().normalize();
    }

    /**
     * Dumps every table to a timestamped {@code .sql} file.
     */
    public BackupFileDTO createBackup(int performedByUserAccountId) {
        String filename = FILE_PREFIX + LocalDateTime.now().format(FILE_FMT) + ".sql";
        Path backupPath = backupDirectory.resolve(filename);

        try {
            Files.createDirectories(backupDirectory);

            try (Connection conn = dataSource.getConnection();
                 BufferedWriter writer = Files.newBufferedWriter(backupPath)) {

                writer.write("-- MotorPH Database Backup\n");
                writer.write("-- Created: " + LocalDateTime.now() + "\n\n");
                writer.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");

                for (String table : listTables(conn)) {
                    dumpTable(conn, writer, table);
                }

                writer.write("SET FOREIGN_KEY_CHECKS = 1;\n");
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write backup file.", e);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to read the database for backup.", e);
        }

        userAccessLogService.logDatabaseBackupCreated(performedByUserAccountId, filename);

        try {
            BasicFileAttributes attributes = Files.readAttributes(backupPath, BasicFileAttributes.class);
            return new BackupFileDTO(filename, attributes.size(), attributes.creationTime().toInstant());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the created backup file.", e);
        }
    }

    /**
     * Lists existing backups, newest first.
     */
    public List<BackupFileDTO> listBackups() {
        if (!Files.exists(backupDirectory)) {
            return List.of();
        }

        List<BackupFileDTO> entries = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDirectory, "*.sql")) {
            for (Path path : stream) {
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);

                entries.add(new BackupFileDTO(
                        path.getFileName().toString(),
                        attributes.size(),
                        attributes.creationTime().toInstant()
                ));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list backup files.", e);
        }

        entries.sort(Comparator.comparing(BackupFileDTO::createdAt).reversed());
        return entries;
    }

    public byte[] readBackup(String filename, int performedByUserAccountId) {
        Path path = resolveExisting(filename);
        byte[] content;

        try {
            content = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read backup file.", e);
        }

        userAccessLogService.logEvent(performedByUserAccountId,
                UserAccessLogService.ACTION_DATABASE_BACKUP_DOWNLOADED, "Downloaded backup: " + filename);

        return content;
    }

    /**
     * Replays a backup file over the live database in a single transaction.
     */
    public void restoreBackup(String filename, int performedByUserAccountId) {
        Path path = resolveExisting(filename);

        String content;

        try {
            content = Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read backup file.", e);
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                for (String sql : splitStatements(content)) {
                    try (Statement statement = conn.createStatement()) {
                        statement.execute(sql);
                    }
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to restore the database from backup.", e);
        }

        userAccessLogService.logDatabaseRestore(performedByUserAccountId, filename);
    }

    public void deleteBackup(String filename, int performedByUserAccountId) {
        Path path = resolveExisting(filename);

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete backup file.", e);
        }

        userAccessLogService.logEvent(performedByUserAccountId,
                UserAccessLogService.ACTION_DATABASE_BACKUP_DELETED, "Deleted backup: " + filename);
    }

    /**
     * Resolves a caller-supplied filename to a file in the backup directory, rejecting any name
     * this service did not generate.
     */
    private Path resolveExisting(String filename) {
        if (filename == null || !BACKUP_FILENAME.matcher(filename).matches()) {
            throw new ValidationException("Invalid backup filename.");
        }

        Path path = backupDirectory.resolve(filename).normalize();

        if (!path.getParent().equals(backupDirectory)) {
            throw new ValidationException("Invalid backup filename.");
        }

        if (!Files.isRegularFile(path)) {
            throw new EntityNotFoundException("Backup file not found.");
        }

        return path;
    }

    private List<String> listTables(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();

        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }

        return tables;
    }

    private void dumpTable(Connection conn, BufferedWriter writer, String table) throws SQLException, IOException {
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
                    String value = rs.getString(i);

                    if (value == null) {
                        writer.write("NULL");
                    } else {
                        writer.write("'" + escapeSql(value) + "'");
                    }

                    if (i < cols) {
                        writer.write(", ");
                    }
                }

                writer.write(");\n");
            }

            writer.write("\n");
        }
    }

    private String escapeSql(String value) {
        return value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\0", "\\0");
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

                    if (i + 1 < sql.length()) {
                        current.append(sql.charAt(++i));
                    }
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
                while (i < sql.length() && sql.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == ';') {
                String statement = current.toString().trim();

                if (!statement.isEmpty()) {
                    statements.add(statement);
                }

                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        String last = current.toString().trim();

        if (!last.isEmpty()) {
            statements.add(last);
        }

        return statements;
    }
}
