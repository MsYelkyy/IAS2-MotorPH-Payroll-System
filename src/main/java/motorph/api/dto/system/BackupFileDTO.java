package motorph.api.dto.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Wire copy of the backend's BackupFileDTO. Size and date formatting used to
 * live on the desktop's BackupEntry; it stays client-side, just here now.
 */
public record BackupFileDTO(
        String fileName,
        long sizeBytes,
        Instant createdAt
) {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");

    @JsonCreator
    public BackupFileDTO(@JsonProperty("fileName") String fileName,
                         @JsonProperty("sizeBytes") long sizeBytes,
                         @JsonProperty("createdAt") Instant createdAt) {
        this.fileName = fileName;
        this.sizeBytes = sizeBytes;
        this.createdAt = createdAt;
    }

    public String getFormattedSize() {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }

        if (sizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeBytes / 1024.0);
        }

        return String.format("%.2f MB", sizeBytes / (1024.0 * 1024));
    }

    public String getFormattedDate() {
        return createdAt == null
                ? "—"
                : DATE_FMT.format(createdAt.atZone(ZoneId.systemDefault()).toLocalDateTime());
    }
}
