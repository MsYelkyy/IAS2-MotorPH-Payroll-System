package motorph.backend.dto.system;

import java.time.Instant;

/** One .sql backup file on the backend host. Formatting is the client's job. */
public record BackupFileDTO(
        String fileName,
        long sizeBytes,
        Instant createdAt
) {
}
