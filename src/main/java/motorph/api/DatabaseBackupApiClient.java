package motorph.api;

import motorph.api.dto.system.BackupFileDTO;

import java.util.List;

/**
 * Thin wrapper around the backend's /api/v1/system/backups endpoints. Backup
 * files live on the backend host now, so restore and delete take a filename
 * and downloading is an explicit fetch rather than reading a local folder.
 */
public final class DatabaseBackupApiClient {

    private static final String BASE_PATH = "/api/v1/system/backups";

    private final HttpApiClient httpApiClient;

    public DatabaseBackupApiClient(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    public List<BackupFileDTO> listBackups() {
        return httpApiClient.getList(BASE_PATH, BackupFileDTO.class);
    }

    /** Returns the listing entry for the backup that was just written. */
    public BackupFileDTO createBackup() {
        return httpApiClient.post(BASE_PATH, null, BackupFileDTO.class);
    }

    public byte[] downloadBackup(String fileName) {
        return httpApiClient.getBytes(BASE_PATH + "/" + fileName + "/download");
    }

    public void deleteBackup(String fileName) {
        httpApiClient.delete(BASE_PATH + "/" + fileName);
    }

    public void restoreBackup(String fileName) {
        httpApiClient.postNoContent(BASE_PATH + "/" + fileName + "/restore", null);
    }
}
