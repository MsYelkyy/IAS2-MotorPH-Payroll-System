package motorph.backend.controller.v1;

import motorph.backend.domain.Employee;
import motorph.backend.domain.value.EmploymentStatus;
import motorph.backend.domain.value.Gender;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.security.AuthenticatedPrincipal;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.EmployeeService;
import motorph.backend.service.ImageFixtures;
import motorph.backend.service.PermissionService;
import motorph.backend.service.ProfilePhotoService;
import motorph.backend.service.UserAccessLogService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The upload endpoint and {@link ProfilePhotoService} should accept and reject the same files. The
 * controller used to have its own shorter list that rejected GIF, even though the service and the
 * desktop file chooser allow it. The rules are now only in the service. These tests run the real
 * controller with the real service.
 */
class EmployeePhotoUploadTest {

    private static final int EMPLOYEE_ID = 42;
    private static final int USER_ACCOUNT_ID = 9;

    @TempDir Path photoDirectory;

    private EmployeeController controller;

    @BeforeEach
    void setUp() {
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        when(employeeRepository.findActiveById(anyInt())).thenReturn(employee());

        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        when(currentUserProvider.getCurrentPrincipal())
                .thenReturn(new AuthenticatedPrincipal(USER_ACCOUNT_ID, EMPLOYEE_ID, "jti", Instant.now().plusSeconds(60)));

        ProfilePhotoService profilePhotoService = new ProfilePhotoService(
                employeeRepository, mock(UserAccessLogService.class), photoDirectory.toString());

        controller = new EmployeeController(
                mock(EmployeeService.class), mock(PermissionService.class), currentUserProvider, profilePhotoService);
    }

    @Test
    @DisplayName("A GIF upload is accepted, so the file chooser's *.gif filter no longer leads to a rejection")
    void gifUploadIsAccepted() throws IOException {
        byte[] gif = ImageFixtures.gif();

        controller.uploadOwnPhoto("gif", gif);

        assertThat(Files.readAllBytes(photoDirectory.resolve("employee-42.gif"))).isEqualTo(gif);
    }

    @Test
    @DisplayName("PNG and JPEG uploads are accepted")
    void pngAndJpegUploadsAreAccepted() {
        assertThatCode(() -> controller.uploadOwnPhoto("png", ImageFixtures.png())).doesNotThrowAnyException();
        assertThat(photoDirectory.resolve("employee-42.png")).exists();

        assertThatCode(() -> controller.uploadOwnPhoto("jpeg", ImageFixtures.jpeg())).doesNotThrowAnyException();
        assertThat(photoDirectory.resolve("employee-42.jpeg")).exists();
        assertThat(photoDirectory.resolve("employee-42.png")).doesNotExist();
    }

    @ParameterizedTest(name = "text file renamed to .{0}")
    @DisplayName("A text file renamed to an image extension is rejected by the endpoint")
    @ValueSource(strings = {"png", "jpg", "jpeg", "gif"})
    void textFileRenamedAsImageIsRejected(String extension) {
        byte[] text = "just some text".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> controller.uploadOwnPhoto(extension, text))
                .isInstanceOf(ValidationException.class);

        assertThat(photoDirectory).isEmptyDirectory();
    }

    @Test
    @DisplayName("An image whose format differs from the claimed extension is rejected by the endpoint")
    void mismatchedExtensionIsRejected() {
        byte[] gif = ImageFixtures.gif();

        assertThatThrownBy(() -> controller.uploadOwnPhoto("png", gif))
                .isInstanceOf(ValidationException.class);

        assertThat(photoDirectory).isEmptyDirectory();
    }

    @ParameterizedTest(name = "extension \"{0}\"")
    @DisplayName("A missing, blank or unsupported extension is rejected")
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "exe", "svg", "webp", "png.exe"})
    void unsupportedExtensionIsRejected(String extension) {
        byte[] png = ImageFixtures.png();

        assertThatThrownBy(() -> controller.uploadOwnPhoto(extension, png))
                .isInstanceOf(ValidationException.class);

        assertThat(photoDirectory).isEmptyDirectory();
    }

    @Test
    @DisplayName("An empty or oversized upload is still rejected")
    void emptyAndOversizedUploadsAreRejected() {
        byte[] empty = new byte[0];
        byte[] oversized = Arrays.copyOf(ImageFixtures.png(), 5 * 1024 * 1024 + 1);

        assertThatThrownBy(() -> controller.uploadOwnPhoto("png", empty))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> controller.uploadOwnPhoto("png", oversized))
                .isInstanceOf(ValidationException.class);

        assertThat(photoDirectory).isEmptyDirectory();
    }

    private Employee employee() {
        return new Employee(
                EMPLOYEE_ID, "Juan", "Cruz", LocalDate.of(1990, 1, 1), Gender.MALE,
                "Manila", "Single", "0917-000-0000", "juan@personal.com",
                EmploymentStatus.REGULAR, 1, 1, LocalDate.of(2020, 1, 1), null, true, null);
    }
}
