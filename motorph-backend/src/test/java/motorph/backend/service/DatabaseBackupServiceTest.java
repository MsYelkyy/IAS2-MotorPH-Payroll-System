package motorph.backend.service;

import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Backup filenames now arrive from a remote caller as a URL segment, which
 * they never did on the desktop. These cover the guard that closes that off.
 */
class DatabaseBackupServiceTest {

    private static final String VALID_NAME = "motorph_backup_2026-08-10_09-30-00.sql";
    private static final int ACTOR_ID = 7;

    @TempDir Path backupDirectory;

    private UserAccessLogService userAccessLogService;
    private DatabaseBackupService databaseBackupService;

    @BeforeEach
    void setUp() {
        userAccessLogService = Mockito.mock(UserAccessLogService.class);
        databaseBackupService = new DatabaseBackupService(
                Mockito.mock(DataSource.class),
                userAccessLogService,
                backupDirectory.toString());
    }

    @ParameterizedTest(name = "rejects \"{0}\"")
    @DisplayName("Only names this service generates are accepted")
    @ValueSource(strings = {
            "../../../../etc/passwd",
            "..\\..\\windows\\system32\\config\\sam",
            "motorph_backup_2026-08-10_09-30-00.sql/../../secret.sql",
            "/etc/passwd",
            "C:\\Windows\\win.ini",
            "arbitrary.sql",
            "motorph_backup_not-a-timestamp.sql",
            "motorph_backup_2026-08-10_09-30-00.sql.exe",
            ""
    })
    void rejectsFilenamesOutsideTheGeneratedPattern(String filename) {
        assertThatThrownBy(() -> databaseBackupService.readBackup(filename, ACTOR_ID))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> databaseBackupService.deleteBackup(filename, ACTOR_ID))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> databaseBackupService.restoreBackup(filename, ACTOR_ID))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(userAccessLogService);
    }

    @Test
    @DisplayName("A well-formed name for a file that is not there is a not-found, not a validation error")
    void reportsMissingBackupSeparatelyFromAnInvalidName() {
        assertThatThrownBy(() -> databaseBackupService.readBackup(VALID_NAME, ACTOR_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("A stored backup can be listed and read back")
    void listsAndReadsStoredBackups() throws IOException {
        Files.writeString(backupDirectory.resolve(VALID_NAME), "-- dump --");

        List<String> names = databaseBackupService.listBackups().stream()
                .map(entry -> entry.fileName())
                .toList();

        assertThat(names).containsExactly(VALID_NAME);
        assertThat(databaseBackupService.readBackup(VALID_NAME, ACTOR_ID))
                .isEqualTo("-- dump --".getBytes(StandardCharsets.UTF_8));
        verify(userAccessLogService).logEvent(ACTOR_ID,
                UserAccessLogService.ACTION_DATABASE_BACKUP_DOWNLOADED, "Downloaded backup: " + VALID_NAME);
    }

    @Test
    @DisplayName("Deleting removes the file from the backup directory")
    void deletesStoredBackup() throws IOException {
        Path stored = backupDirectory.resolve(VALID_NAME);
        Files.writeString(stored, "-- dump --");

        databaseBackupService.deleteBackup(VALID_NAME, ACTOR_ID);

        assertThat(Files.exists(stored)).isFalse();
        verify(userAccessLogService).logEvent(ACTOR_ID,
                UserAccessLogService.ACTION_DATABASE_BACKUP_DELETED, "Deleted backup: " + VALID_NAME);
    }

    @Test
    @DisplayName("Listing an absent backup directory is empty, not an error")
    void listsEmptyWhenDirectoryMissing() {
        DatabaseBackupService service = new DatabaseBackupService(
                Mockito.mock(DataSource.class),
                Mockito.mock(UserAccessLogService.class),
                backupDirectory.resolve("not-created-yet").toString());

        assertThat(service.listBackups()).isEmpty();
    }
}
