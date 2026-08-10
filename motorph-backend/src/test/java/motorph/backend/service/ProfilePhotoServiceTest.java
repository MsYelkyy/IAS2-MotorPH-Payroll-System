package motorph.backend.service;

import motorph.backend.domain.Employee;
import motorph.backend.domain.value.EmploymentStatus;
import motorph.backend.domain.value.Gender;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.EmployeeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The uploaded extension becomes part of a filename on the server, and the
 * stored photo_path is read back off disk, so both are treated as untrusted.
 */
class ProfilePhotoServiceTest {

    private static final int EMPLOYEE_ID = 42;
    private static final byte[] IMAGE = new byte[]{1, 2, 3, 4};

    @TempDir Path photoDirectory;

    private EmployeeRepository employeeRepository;
    private ProfilePhotoService profilePhotoService;

    @BeforeEach
    void setUp() {
        employeeRepository = Mockito.mock(EmployeeRepository.class);
        when(employeeRepository.findActiveById(anyInt())).thenReturn(employee(null));
        profilePhotoService = new ProfilePhotoService(employeeRepository, photoDirectory.toString());
    }

    @Test
    @DisplayName("An uploaded photo is stored under a name derived from the employee id")
    void storesPhotoUnderGeneratedName() {
        profilePhotoService.storePhoto(EMPLOYEE_ID, IMAGE, "png");

        assertThat(photoDirectory.resolve("employee-42.png")).exists();
        verify(employeeRepository).updatePhotoPath(EMPLOYEE_ID, "employee-42.png");
    }

    @Test
    @DisplayName("Replacing a photo does not leave the previous one behind")
    void replacingPhotoRemovesTheOldFile() {
        profilePhotoService.storePhoto(EMPLOYEE_ID, IMAGE, "png");
        profilePhotoService.storePhoto(EMPLOYEE_ID, IMAGE, "jpg");

        assertThat(photoDirectory.resolve("employee-42.png")).doesNotExist();
        assertThat(photoDirectory.resolve("employee-42.jpg")).exists();
    }

    @ParameterizedTest(name = "rejects extension \"{0}\"")
    @DisplayName("Only image extensions on the allowlist are accepted")
    @ValueSource(strings = {"exe", "sh", "jsp", "php", "", "png.exe", "../png"})
    void rejectsExtensionsOutsideTheAllowlist(String extension) {
        assertThatThrownBy(() -> profilePhotoService.storePhoto(EMPLOYEE_ID, IMAGE, extension))
                .isInstanceOf(ValidationException.class);

        assertThat(photoDirectory).isEmptyDirectory();
    }

    @Test
    @DisplayName("An empty upload is rejected")
    void rejectsEmptyUpload() {
        assertThatThrownBy(() -> profilePhotoService.storePhoto(EMPLOYEE_ID, new byte[0], "png"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("An upload over the size cap is rejected")
    void rejectsOversizedUpload() {
        byte[] tooBig = new byte[5 * 1024 * 1024 + 1];

        assertThatThrownBy(() -> profilePhotoService.storePhoto(EMPLOYEE_ID, tooBig, "png"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("A stored photo reads back byte for byte")
    void readsStoredPhoto() {
        profilePhotoService.storePhoto(EMPLOYEE_ID, IMAGE, "png");
        when(employeeRepository.findAnyById(EMPLOYEE_ID)).thenReturn(employee("employee-42.png"));

        assertThat(profilePhotoService.readPhoto(EMPLOYEE_ID)).isEqualTo(IMAGE);
    }

    @Test
    @DisplayName("A photo_path pointing outside the photo directory reads as no photo")
    void refusesToReadOutsideThePhotoDirectory() throws IOException {
        Path outside = photoDirectory.getParent().resolve("outside-secret.png");
        Files.write(outside, IMAGE);

        when(employeeRepository.findAnyById(EMPLOYEE_ID))
                .thenReturn(employee("../" + outside.getFileName()));

        assertThatThrownBy(() -> profilePhotoService.readPhoto(EMPLOYEE_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("A legacy full path resolves by filename once the file lives in the photo directory")
    void resolvesLegacyStoredPathByFilename() {
        profilePhotoService.storePhoto(EMPLOYEE_ID, IMAGE, "png");
        when(employeeRepository.findAnyById(EMPLOYEE_ID))
                .thenReturn(employee("src/main/resources/images/profile-images/employee-42.png"));

        assertThat(profilePhotoService.readPhoto(EMPLOYEE_ID)).isEqualTo(IMAGE);
    }

    @Test
    @DisplayName("An employee with no photo on file reports not found")
    void reportsMissingPhoto() {
        when(employeeRepository.findAnyById(EMPLOYEE_ID)).thenReturn(employee(null));

        assertThatThrownBy(() -> profilePhotoService.readPhoto(EMPLOYEE_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private Employee employee(String photoPath) {
        return new Employee(
                EMPLOYEE_ID, "Juan", "Cruz", LocalDate.of(1990, 1, 1), Gender.MALE,
                "Manila", "Single", "0917-000-0000", "juan@personal.com",
                EmploymentStatus.REGULAR, 1, 1, LocalDate.of(2020, 1, 1), null, true, photoPath);
    }
}
