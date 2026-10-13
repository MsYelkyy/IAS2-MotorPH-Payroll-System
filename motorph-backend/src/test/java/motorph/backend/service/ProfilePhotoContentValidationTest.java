package motorph.backend.service;

import motorph.backend.domain.Employee;
import motorph.backend.domain.value.EmploymentStatus;
import motorph.backend.domain.value.Gender;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.EmployeeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Photos are checked by their bytes, not their name. The header must be a valid PNG, JPEG or GIF
 * header and match the extension. The size limit and the server-made filename still apply. A
 * rejected upload changes nothing on disk or in the database.
 */
class ProfilePhotoContentValidationTest {

    private static final int EMPLOYEE_ID = 42;
    private static final int ACTOR_ACCOUNT_ID = 9;
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;

    @TempDir Path photoDirectory;

    private EmployeeRepository employeeRepository;
    private UserAccessLogService userAccessLogService;
    private ProfilePhotoService profilePhotoService;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        userAccessLogService = mock(UserAccessLogService.class);
        when(employeeRepository.findActiveById(anyInt())).thenReturn(employee());
        profilePhotoService = new ProfilePhotoService(
                employeeRepository, userAccessLogService, photoDirectory.toString());
    }

    // Supported formats

    @Test
    @DisplayName("A valid PNG is accepted and stored unchanged")
    void acceptsValidPng() throws IOException {
        byte[] png = ImageFixtures.png();

        profilePhotoService.storePhoto(EMPLOYEE_ID, png, "png", ACTOR_ACCOUNT_ID);

        assertThat(Files.readAllBytes(photoDirectory.resolve("employee-42.png"))).isEqualTo(png);
        verify(employeeRepository).updatePhotoPath(EMPLOYEE_ID, "employee-42.png");
    }

    @ParameterizedTest(name = "stored as .{0}")
    @DisplayName("A valid JPEG is accepted under either JPEG extension")
    @ValueSource(strings = {"jpg", "jpeg"})
    void acceptsValidJpeg(String extension) throws IOException {
        byte[] jpeg = ImageFixtures.jpeg();

        profilePhotoService.storePhoto(EMPLOYEE_ID, jpeg, extension, ACTOR_ACCOUNT_ID);

        assertThat(Files.readAllBytes(photoDirectory.resolve("employee-42." + extension))).isEqualTo(jpeg);
    }

    @Test
    @DisplayName("A progressive JPEG, which uses a different frame marker, is accepted")
    void acceptsProgressiveJpeg() {
        assertThatCode(() ->
                profilePhotoService.storePhoto(EMPLOYEE_ID, ImageFixtures.progressiveJpeg(), "jpg", ACTOR_ACCOUNT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A valid GIF is accepted, as the desktop file chooser and the content-type mapping allow")
    void acceptsValidGif() throws IOException {
        byte[] gif = ImageFixtures.gif();

        profilePhotoService.storePhoto(EMPLOYEE_ID, gif, "gif", ACTOR_ACCOUNT_ID);

        assertThat(Files.readAllBytes(photoDirectory.resolve("employee-42.gif"))).isEqualTo(gif);
    }

    @Test
    @DisplayName("The older GIF87a header is accepted as well as GIF89a")
    void acceptsGif87a() {
        byte[] gif87a = ImageFixtures.gif();
        gif87a[4] = '7';

        assertThatCode(() -> profilePhotoService.storePhoto(EMPLOYEE_ID, gif87a, "gif", ACTOR_ACCOUNT_ID))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "claimed as \"{0}\"")
    @DisplayName("The claimed extension is matched case-insensitively and may be a whole filename")
    @ValueSource(strings = {"PNG", "Png", "photo.PNG", " png "})
    void extensionIsNormalisedBeforeItIsMatched(String claimed) {
        assertThatCode(() -> profilePhotoService.storePhoto(EMPLOYEE_ID, ImageFixtures.png(), claimed, ACTOR_ACCOUNT_ID))
                .doesNotThrowAnyException();

        assertThat(photoDirectory.resolve("employee-42.png")).exists();
    }

    // Files that are not images

    static Stream<Arguments> notImagesRenamedAsImages() {
        Stream.Builder<Arguments> cases = Stream.builder();

        for (String extension : new String[]{"png", "jpg", "jpeg", "gif"}) {
            cases.add(Arguments.of("plain text", text("This is a note, not a picture."), extension));
            cases.add(Arguments.of("HTML with script", text("<html><script>alert(1)</script></html>"), extension));
            cases.add(Arguments.of("Windows executable", windowsExecutable(), extension));
            cases.add(Arguments.of("ELF executable", elfExecutable(), extension));
            cases.add(Arguments.of("PDF", text("%PDF-1.7\n1 0 obj\n<<>>\nendobj\n"), extension));
            cases.add(Arguments.of("zip archive", new byte[]{'P', 'K', 3, 4, 20, 0, 0, 0, 8, 0}, extension));
        }

        return cases.build();
    }

    @ParameterizedTest(name = "{0} renamed to .{2}")
    @DisplayName("A text, script or executable file renamed to an image extension is rejected")
    @MethodSource("notImagesRenamedAsImages")
    void rejectsNonImageContent(String description, byte[] content, String extension) {
        assertRejectedAndNothingChanged(content, extension);
    }

    @Test
    @DisplayName("The rejection message is fixed and does not repeat any of the uploaded content")
    void rejectionMessageDoesNotEchoContent() {
        byte[] content = text("SECRET-MARKER-should-never-be-echoed");

        assertThatThrownBy(() -> profilePhotoService.storePhoto(EMPLOYEE_ID, content, "png", ACTOR_ACCOUNT_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Profile photo must be a valid PNG, JPG or GIF image.");
    }

    // Signature and extension disagree

    static Stream<Arguments> imagesWithTheWrongExtension() {
        return Stream.of(
                Arguments.of("PNG", ImageFixtures.png(), "jpg"),
                Arguments.of("PNG", ImageFixtures.png(), "jpeg"),
                Arguments.of("PNG", ImageFixtures.png(), "gif"),
                Arguments.of("JPEG", ImageFixtures.jpeg(), "png"),
                Arguments.of("JPEG", ImageFixtures.jpeg(), "gif"),
                Arguments.of("GIF", ImageFixtures.gif(), "png"),
                Arguments.of("GIF", ImageFixtures.gif(), "jpg"),
                Arguments.of("GIF", ImageFixtures.gif(), "jpeg")
        );
    }

    @ParameterizedTest(name = "{0} content claimed as .{2}")
    @DisplayName("A real image whose format differs from its extension is rejected")
    @MethodSource("imagesWithTheWrongExtension")
    void rejectsSignatureAndExtensionMismatch(String actualFormat, byte[] content, String extension) {
        assertThatThrownBy(() -> profilePhotoService.storePhoto(EMPLOYEE_ID, content, extension, ACTOR_ACCOUNT_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Profile photo content does not match its file type.");

        assertNothingChanged();
    }

    // Truncated or malformed headers

    static Stream<Arguments> malformedHeaders() {
        byte[] png = ImageFixtures.png();
        byte[] jpeg = ImageFixtures.jpeg();
        byte[] gif = ImageFixtures.gif();

        return Stream.of(
                Arguments.of("PNG signature only", Arrays.copyOf(png, 8), "png"),
                Arguments.of("PNG cut inside IHDR", Arrays.copyOf(png, 20), "png"),
                Arguments.of("PNG cut one byte before the IHDR CRC ends", Arrays.copyOf(png, 32), "png"),
                Arguments.of("PNG with a corrupted IHDR CRC", flipByte(png, 30), "png"),
                Arguments.of("PNG whose first chunk is not IHDR", pngWithValidCrc(withBytes(png, 12, "IDAT")), "png"),
                Arguments.of("PNG with zero width", pngWithValidCrc(withBytes(png, 16, new byte[]{0, 0, 0, 0})), "png"),
                Arguments.of("PNG with zero height", pngWithValidCrc(withBytes(png, 20, new byte[]{0, 0, 0, 0})), "png"),
                Arguments.of("PNG signature then garbage", concat(Arrays.copyOf(png, 8), new byte[40]), "png"),

                Arguments.of("JPEG start marker only", new byte[]{(byte) 0xFF, (byte) 0xD8}, "jpg"),
                Arguments.of("JPEG start marker and one more byte", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, "jpg"),
                Arguments.of("JPEG cut inside the first segment", Arrays.copyOf(jpeg, 10), "jpg"),
                Arguments.of("JPEG cut before the frame header", Arrays.copyOf(jpeg, indexOfFrameHeader(jpeg)), "jpeg"),
                Arguments.of("JPEG cut inside the frame header", Arrays.copyOf(jpeg, indexOfFrameHeader(jpeg) + 6), "jpg"),
                Arguments.of("JPEG with zero width", withBytes(jpeg, indexOfFrameHeader(jpeg) + 7, new byte[]{0, 0}), "jpg"),
                Arguments.of("JPEG with zero height", withBytes(jpeg, indexOfFrameHeader(jpeg) + 5, new byte[]{0, 0}), "jpg"),
                Arguments.of("JPEG whose segment length runs past the end",
                        new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x7F, (byte) 0xFF, 'J', 'F'}, "jpg"),
                Arguments.of("JPEG with a segment length below the minimum",
                        new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 1, 0, 0}, "jpg"),
                Arguments.of("JPEG that starts scan data before any frame header",
                        new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xDA, 0, 8, 1, 1, 0, 0, 0, 0}, "jpg"),
                Arguments.of("JPEG start marker then ordinary text", concat(new byte[]{(byte) 0xFF, (byte) 0xD8}, text("hello world")), "jpg"),

                Arguments.of("GIF signature only", Arrays.copyOf(gif, 6), "gif"),
                Arguments.of("GIF cut inside the screen descriptor", Arrays.copyOf(gif, 10), "gif"),
                Arguments.of("GIF with an unknown version", withBytes(gif, 3, "90a"), "gif"),
                Arguments.of("GIF with zero width", withBytes(gif, 6, new byte[]{0, 0}), "gif"),
                Arguments.of("GIF with zero height", withBytes(gif, 8, new byte[]{0, 0}), "gif")
        );
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("A truncated or malformed image header is rejected")
    @MethodSource("malformedHeaders")
    void rejectsTruncatedOrMalformedHeaders(String description, byte[] content, String extension) {
        assertRejectedAndNothingChanged(content, extension);
    }

    // Existing restrictions still hold

    @Test
    @DisplayName("An empty upload is still rejected")
    void emptyUploadIsStillRejected() {
        assertThatThrownBy(() -> profilePhotoService.storePhoto(EMPLOYEE_ID, new byte[0], "png", ACTOR_ACCOUNT_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Photo file is empty.");
    }

    @Test
    @DisplayName("An image over the 5 MB cap is still rejected, even with a valid header")
    void oversizedImageIsStillRejected() {
        byte[] png = ImageFixtures.png();
        byte[] tooBig = Arrays.copyOf(png, (int) MAX_PHOTO_BYTES + 1);

        assertThatThrownBy(() -> profilePhotoService.storePhoto(EMPLOYEE_ID, tooBig, "png", ACTOR_ACCOUNT_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Photo must be 5 MB or smaller.");

        assertNothingChanged();
    }

    @Test
    @DisplayName("An image of exactly 5 MB is still accepted")
    void imageAtTheCapIsStillAccepted() {
        byte[] atCap = Arrays.copyOf(ImageFixtures.png(), (int) MAX_PHOTO_BYTES);

        assertThatCode(() -> profilePhotoService.storePhoto(EMPLOYEE_ID, atCap, "png", ACTOR_ACCOUNT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("The stored filename always comes from the server, never from the uploaded name")
    void filenameIsServerControlled() throws IOException {
        profilePhotoService.storePhoto(EMPLOYEE_ID, ImageFixtures.png(), "../../evil.png", ACTOR_ACCOUNT_ID);

        try (Stream<Path> stored = Files.list(photoDirectory)) {
            assertThat(stored.map(path -> path.getFileName().toString())).containsExactly("employee-42.png");
        }
        assertThat(photoDirectory.getParent().resolve("evil.png")).doesNotExist();
        verify(employeeRepository).updatePhotoPath(EMPLOYEE_ID, "employee-42.png");
    }

    @Test
    @DisplayName("A rejected upload leaves the employee's existing photo in place")
    void rejectedUploadKeepsTheExistingPhoto() throws IOException {
        byte[] original = ImageFixtures.png();
        profilePhotoService.storePhoto(EMPLOYEE_ID, original, "png", ACTOR_ACCOUNT_ID);

        byte[] notAnImage = text("not an image");
        byte[] gif = ImageFixtures.gif();

        assertThatThrownBy(() -> profilePhotoService.storePhoto(EMPLOYEE_ID, notAnImage, "png", ACTOR_ACCOUNT_ID))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> profilePhotoService.storePhoto(EMPLOYEE_ID, gif, "jpg", ACTOR_ACCOUNT_ID))
                .isInstanceOf(ValidationException.class);

        assertThat(Files.readAllBytes(photoDirectory.resolve("employee-42.png"))).isEqualTo(original);
        verify(employeeRepository, times(1)).updatePhotoPath(anyInt(), anyString());
        verify(userAccessLogService, times(1)).logEvent(
                anyInt(), anyString(), anyString(), anyString(), any());
    }

    // Helpers

    private void assertRejectedAndNothingChanged(byte[] content, String extension) {
        assertThatThrownBy(() -> profilePhotoService.storePhoto(EMPLOYEE_ID, content, extension, ACTOR_ACCOUNT_ID))
                .isInstanceOf(ValidationException.class);

        assertNothingChanged();
    }

    private void assertNothingChanged() {
        assertThat(photoDirectory).isEmptyDirectory();
        verify(employeeRepository, never()).updatePhotoPath(anyInt(), anyString());
        verifyNoInteractions(userAccessLogService);
    }

    private static byte[] text(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] windowsExecutable() {
        byte[] exe = new byte[128];
        exe[0] = 'M';
        exe[1] = 'Z';
        exe[2] = (byte) 0x90;
        exe[60] = (byte) 0x80;
        return exe;
    }

    private static byte[] elfExecutable() {
        byte[] elf = new byte[64];
        elf[0] = 0x7F;
        elf[1] = 'E';
        elf[2] = 'L';
        elf[3] = 'F';
        elf[4] = 2;
        return elf;
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] joined = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, joined, first.length, second.length);
        return joined;
    }

    private static byte[] withBytes(byte[] source, int offset, String ascii) {
        return withBytes(source, offset, ascii.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] withBytes(byte[] source, int offset, byte[] replacement) {
        byte[] copy = source.clone();
        System.arraycopy(replacement, 0, copy, offset, replacement.length);
        return copy;
    }

    private static byte[] flipByte(byte[] source, int index) {
        byte[] copy = source.clone();
        copy[index] ^= 0x01;
        return copy;
    }

    /**
     * Recalculates the IHDR CRC, so a test fails because of the field it changed and not because the
     * CRC no longer matches.
     */
    private static byte[] pngWithValidCrc(byte[] png) {
        CRC32 crc = new CRC32();
        crc.update(png, 12, 17);
        long value = crc.getValue();

        return withBytes(png, 29, new byte[]{
                (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value});
    }

    private static int indexOfFrameHeader(byte[] jpeg) {
        for (int i = 2; i < jpeg.length - 1; i++) {
            if ((jpeg[i] & 0xFF) == 0xFF && (jpeg[i + 1] & 0xFF) == 0xC0) {
                return i;
            }
        }

        throw new IllegalStateException("No baseline frame header in the fixture");
    }

    private Employee employee() {
        return new Employee(
                EMPLOYEE_ID, "Juan", "Cruz", LocalDate.of(1990, 1, 1), Gender.MALE,
                "Manila", "Single", "0917-000-0000", "juan@personal.com",
                EmploymentStatus.REGULAR, 1, 1, LocalDate.of(2020, 1, 1), null, true, null);
    }
}
