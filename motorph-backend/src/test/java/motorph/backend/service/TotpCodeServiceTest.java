package motorph.backend.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;

class TotpCodeServiceTest {

    /** The RFC 6238 appendix B secret for SHA-1: the ASCII bytes of "12345678901234567890". */
    private static final byte[] RFC_SECRET = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    private final MutableClock clock = new MutableClock(Instant.ofEpochSecond(59));
    private final TotpCodeService totp = new TotpCodeService(clock);

    @ParameterizedTest(name = "at {0}s the 6-digit code is {1}")
    @DisplayName("Codes match the RFC 6238 SHA-1 test vectors (their 8-digit values, last six digits)")
    @CsvSource({
            "59,          287082",
            "1111111109,  081804",
            "1111111111,  050471",
            "1234567890,  005924",
            "2000000000,  279037"
    })
    void matchesRfc6238TestVectors(long epochSecond, String expectedCode) {
        clock.advance(Duration.ofSeconds(epochSecond - 59));

        long step = totp.currentTimeStep();

        assertThat(totp.codeForStep(RFC_SECRET, step)).isEqualTo(expectedCode);
        assertThat(totp.findMatchingTimeStep(RFC_SECRET, expectedCode, null)).hasValue(step);
    }

    @Test
    @DisplayName("A code from one step earlier or later is accepted, two steps away is not")
    void acceptsOnlyOneStepOfDrift() {
        clock.advance(Duration.ofSeconds(1_000_000 - 59));
        long current = totp.currentTimeStep();

        assertThat(totp.findMatchingTimeStep(RFC_SECRET, totp.codeForStep(RFC_SECRET, current - 1), null))
                .hasValue(current - 1);
        assertThat(totp.findMatchingTimeStep(RFC_SECRET, totp.codeForStep(RFC_SECRET, current + 1), null))
                .hasValue(current + 1);
        assertThat(totp.findMatchingTimeStep(RFC_SECRET, totp.codeForStep(RFC_SECRET, current - 2), null)).isEmpty();
        assertThat(totp.findMatchingTimeStep(RFC_SECRET, totp.codeForStep(RFC_SECRET, current + 2), null)).isEmpty();
    }

    @Test
    @DisplayName("A time step that was already accepted is refused, a later one is not")
    void refusesStepsAtOrBeforeTheLastUsedStep() {
        clock.advance(Duration.ofSeconds(1_000_000 - 59));
        long current = totp.currentTimeStep();
        String code = totp.codeForStep(RFC_SECRET, current);

        assertThat(totp.findMatchingTimeStep(RFC_SECRET, code, current)).isEmpty();
        assertThat(totp.findMatchingTimeStep(RFC_SECRET, code, current + 1)).isEmpty();
        assertThat(totp.findMatchingTimeStep(RFC_SECRET, code, current - 1)).hasValue(current);
    }

    @ParameterizedTest(name = "\"{0}\" is not a valid code")
    @CsvSource(value = {"12345", "1234567", "abcdef", "12 456", "''"}, ignoreLeadingAndTrailingWhitespace = false)
    void rejectsMalformedCodes(String code) {
        OptionalLong match = totp.findMatchingTimeStep(RFC_SECRET, code, null);

        assertThat(match).isEmpty();
    }

    @Test
    @DisplayName("Secrets are 160 bits, random, and encode to unpadded Base32")
    void generatesRandomTwentyByteSecrets() {
        byte[] first = totp.generateSecret();
        byte[] second = totp.generateSecret();

        assertThat(first).hasSize(20);
        assertThat(first).isNotEqualTo(second);
        assertThat(totp.toBase32(first)).matches("[A-Z2-7]{32}");
    }

    @Test
    @DisplayName("The otpauth URI carries issuer MotorPH, the company email, 6 digits, SHA1 and a 30 second period")
    void buildsStandardOtpAuthUri() {
        String uri = totp.buildOtpAuthUri("juan.cruz@motorph.com", RFC_SECRET);

        assertThat(uri).startsWith("otpauth://totp/MotorPH:juan.cruz%40motorph.com?");
        assertThat(uri).contains("secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
                .contains("issuer=MotorPH")
                .contains("algorithm=SHA1")
                .contains("digits=6")
                .contains("period=30");
    }

    @Test
    @DisplayName("The QR code is a PNG that decodes back to the otpauth URI")
    void qrCodeDecodesToTheUri() throws Exception {
        String uri = totp.buildOtpAuthUri("juan.cruz@motorph.com", RFC_SECRET);

        byte[] png = new TotpQrCodeGenerator().toPng(uri);

        assertThat(png).startsWith(new byte[] {(byte) 0x89, 'P', 'N', 'G'});

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        var bitmap = new BinaryBitmap(new HybridBinarizer(
                new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels)));

        // PURE_BARCODE: the image is an exact, noise-free rendering, which ZXing's camera-oriented detector
        // can fail to locate at some sizes even though the code itself is valid.
        assertThat(new QRCodeReader().decode(bitmap, Map.of(DecodeHintType.PURE_BARCODE, Boolean.TRUE)).getText())
                .isEqualTo(uri);
    }
}
