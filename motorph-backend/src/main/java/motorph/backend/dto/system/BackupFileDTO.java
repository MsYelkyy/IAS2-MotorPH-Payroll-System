package motorph.backend.dto.system;

import java.time.Instant;

/** A {@code .sql} backup file on the backend host. Values are raw; formatting is left to the client. */
public record BackupFileDTO(
        String fileName,
        long sizeBytes,
        Instant createdAt
) {
}
