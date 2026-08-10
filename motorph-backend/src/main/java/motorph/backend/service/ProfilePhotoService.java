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
 * Stores and serves employee profile photos.
 *
 * The desktop version copied a file the user picked into a path relative to
 * the client's working directory and saved that whole path in photo_path,
 * which only ever worked because the client and the database were the same
 * machine. Photos now live in a configured directory on the backend host and
 * photo_path holds a bare filename, so any client can fetch a photo over the
 * API instead of needing the file locally.
 *
 * The uploaded extension becomes part of a filename, so it is checked against
 * an allowlist rather than taken as given, and reads resolve strictly inside
 * the photo directory.
 */
@Service
public class ProfilePhotoService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("png", "jpg", "jpeg", "gif");
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;
    private static final String FILE_PREFIX = "employee-";

    private final EmployeeRepository employeeRepository;
    private final Path photoDirectory;

    public ProfilePhotoService(EmployeeRepository employeeRepository,
                               @Value("${motorph.photo.directory:profile-photos}") String photoDirectory) {
        this.employeeRepository = employeeRepository;
        this.photoDirectory = Paths.get(photoDirectory).toAbsolutePath().normalize();
    }

    // stores an employee's own uploaded photo, replacing whatever they had
    public void storePhoto(int employeeId, byte[] content, String extension) {
        requireActiveEmployee(employeeId);

        if (content == null || content.length == 0) {
            throw new ValidationException("Photo file is empty.");
        }

        if (content.length > MAX_PHOTO_BYTES) {
            throw new ValidationException("Photo must be 5 MB or smaller.");
        }

        String normalized = normalizeExtension(extension);
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
    }

    // reads an employee's stored photo
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

    // maps a stored filename to the media type the download should advertise
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

    // resolves a stored photo_path to a real file inside the photo directory.
    // Only the filename is honoured, so legacy rows still holding the desktop's
    // full relative path resolve to the right file once it has been moved here.
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

    // removes any previously stored photo for this employee, whatever its extension
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

    // accepts either a bare extension or a filename to take it from
    private String extensionOf(String value) {
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        int dot = trimmed.lastIndexOf('.');

        return dot >= 0 && dot < trimmed.length() - 1 ? trimmed.substring(dot + 1) : trimmed;
    }
}
