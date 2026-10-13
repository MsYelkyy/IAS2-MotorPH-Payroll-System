package motorph.backend.service;

import motorph.backend.domain.Employee;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.EmployeeRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

/**
 * Stores and serves employee profile photos. Photos are kept in a configured directory on the
 * backend host, and {@code photo_path} holds only the filename. Uploaded extensions are checked
 * against an allowlist, the file's header bytes must match the claimed type, and reads are
 * restricted to the photo directory.
 */
@Service
public class ProfilePhotoService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("png", "jpg", "jpeg", "gif");
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;
    private static final String FILE_PREFIX = "employee-";

    private final EmployeeRepository employeeRepository;
    private final UserAccessLogService userAccessLogService;
    private final Path photoDirectory;

    public ProfilePhotoService(EmployeeRepository employeeRepository,
                               UserAccessLogService userAccessLogService,
                               @Value("${motorph.photo.directory:profile-photos}") String photoDirectory) {
        this.employeeRepository = employeeRepository;
        this.userAccessLogService = userAccessLogService;
        this.photoDirectory = Paths.get(photoDirectory).toAbsolutePath().normalize();
    }

    /**
     * Stores the employee's profile photo and records the update in the audit log.
     */
    public void storePhoto(int employeeId, byte[] content, String extension, int actorUserAccountId) {
        requireActiveEmployee(employeeId);

        if (content == null || content.length == 0) {
            throw new ValidationException("Photo file is empty.");
        }

        if (content.length > MAX_PHOTO_BYTES) {
            throw new ValidationException("Photo must be 5 MB or smaller.");
        }

        String normalized = normalizeExtension(extension);
        requireContentMatchingExtension(content, normalized);
        String filename = FILE_PREFIX + employeeId + "." + normalized;

        try {
            Files.createDirectories(photoDirectory);
            deleteExistingPhotos(employeeId);

            Files.copy(new java.io.ByteArrayInputStream(content),
                    photoDirectory.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save profile photo.", e);
        }

        employeeRepository.updatePhotoPath(employeeId, filename);

        userAccessLogService.logEvent(actorUserAccountId, UserAccessLogService.ACTION_PROFILE_PHOTO_UPDATED,
                "Updated profile photo of employee ID " + employeeId,
                UserAccessLogService.TARGET_EMPLOYEE, employeeId);
    }

    public byte[] readPhoto(int employeeId) {
        Employee employee = employeeRepository.findAnyById(employeeId);

        if (employee == null) {
            throw new EntityNotFoundException("Employee not found.");
        }

        Path path = resolveStoredPhoto(employee.getPhotoPath());

        if (path == null) {
            throw new EntityNotFoundException("This employee has no profile photo.");
        }

        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read profile photo.", e);
        }
    }

    /**
     * Returns the media type to use when serving the employee's photo.
     */
    public String contentTypeFor(int employeeId) {
        Employee employee = employeeRepository.findAnyById(employeeId);
        String stored = employee == null ? null : employee.getPhotoPath();
        String extension = stored == null ? "" : extensionOf(stored);

        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            default -> "image/png";
        };
    }

    /**
     * Resolves a stored {@code photo_path} to a file inside the photo directory. Only the filename
     * part is used, so older rows that still hold a full relative path resolve correctly once the
     * file has been moved here.
     */
    private Path resolveStoredPhoto(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }

        Path filename = Paths.get(storedPath).getFileName();

        if (filename == null) {
            return null;
        }

        Path candidate = photoDirectory.resolve(filename.toString()).normalize();

        if (!candidate.startsWith(photoDirectory) || !Files.isRegularFile(candidate)) {
            return null;
        }

        return candidate;
    }

    /**
     * Deletes any existing photo for the employee, whatever its extension.
     */
    private void deleteExistingPhotos(int employeeId) throws IOException {
        if (!Files.exists(photoDirectory)) {
            return;
        }

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(photoDirectory, FILE_PREFIX + employeeId + ".*")) {
            for (Path existing : stream) {
                Files.deleteIfExists(existing);
            }
        }
    }

    private void requireActiveEmployee(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }

        if (employeeRepository.findActiveById(employeeId) == null) {
            throw new EntityNotFoundException("Employee not found.");
        }
    }

    private String normalizeExtension(String extension) {
        String normalized = extension == null ? "" : extensionOf(extension);

        if (!ALLOWED_EXTENSIONS.contains(normalized)) {
            throw new ValidationException("Profile photo must be a PNG, JPG or GIF image.");
        }

        return normalized;
    }

    /**
     * Checks the uploaded bytes themselves. The content must have a well-formed PNG, JPEG or GIF
     * header, and that format must be the one the extension claims.
     */
    private void requireContentMatchingExtension(byte[] content, String normalizedExtension) {
        ImageFormat detected = ImageFormat.detect(content);

        if (detected == null) {
            throw new ValidationException("Profile photo must be a valid PNG, JPG or GIF image.");
        }

        if (!detected.matchesExtension(normalizedExtension)) {
            throw new ValidationException("Profile photo content does not match its file type.");
        }
    }

    /**
     * Returns the extension from either a bare extension or a filename.
     */
    private String extensionOf(String value) {
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        int dot = trimmed.lastIndexOf('.');

        return dot >= 0 && dot < trimmed.length() - 1 ? trimmed.substring(dot + 1) : trimmed;
    }
}
