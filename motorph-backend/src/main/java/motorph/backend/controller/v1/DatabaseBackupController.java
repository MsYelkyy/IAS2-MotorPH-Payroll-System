package motorph.backend.controller.v1;

import motorph.backend.domain.value.PermissionKey;
import motorph.backend.dto.system.BackupFileDTO;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.DatabaseBackupService;
import motorph.backend.service.PermissionService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Database backup administration, every endpoint gated BACKUP_DATABASE.
 * Backups live on the backend host, so restore takes a filename rather than
 * an upload. Filenames arriving in the URL are validated against the exact
 * generated pattern inside the service before any filesystem access.
 */
@RestController
@RequestMapping("/api/v1/system/backups")
public class DatabaseBackupController {

    private final DatabaseBackupService databaseBackupService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    public DatabaseBackupController(DatabaseBackupService databaseBackupService,
                                    PermissionService permissionService,
                                    CurrentUserProvider currentUserProvider) {
        this.databaseBackupService = databaseBackupService;
        this.permissionService = permissionService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<BackupFileDTO> listBackups() {
        permissionService.requirePermission(PermissionKey.BACKUP_DATABASE);
        return databaseBackupService.listBackups();
    }

    @PostMapping
    public BackupFileDTO createBackup() {
        permissionService.requirePermission(PermissionKey.BACKUP_DATABASE);
        return databaseBackupService.createBackup(currentUserProvider.getCurrentUserAccountId());
    }

    @GetMapping(value = "/{filename}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> downloadBackup(@PathVariable String filename) {
        permissionService.requirePermission(PermissionKey.BACKUP_DATABASE);

        byte[] content = databaseBackupService.readBackup(filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    @DeleteMapping("/{filename}")
    public void deleteBackup(@PathVariable String filename) {
        permissionService.requirePermission(PermissionKey.BACKUP_DATABASE);
        databaseBackupService.deleteBackup(filename);
    }

    @PostMapping("/{filename}/restore")
    public void restoreBackup(@PathVariable String filename) {
        permissionService.requirePermission(PermissionKey.BACKUP_DATABASE);
        databaseBackupService.restoreBackup(filename, currentUserProvider.getCurrentUserAccountId());
    }
}
