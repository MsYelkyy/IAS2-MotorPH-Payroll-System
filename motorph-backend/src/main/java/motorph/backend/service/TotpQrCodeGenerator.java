package motorph.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/** Creates a PNG QR code from the TOTP setup link for the desktop app to display. */
@Component
public class TotpQrCodeGenerator {

    private static final int MODULE_PIXELS = 8;
    private static final int QUIET_ZONE_MODULES = 4;

    public byte[] toPng(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                            EncodeHintType.MARGIN, 0));

            int size = (matrix.getWidth() + 2 * QUIET_ZONE_MODULES) * MODULE_PIXELS;
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_BINARY);

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int mx = x / MODULE_PIXELS - QUIET_ZONE_MODULES;
                    int my = y / MODULE_PIXELS - QUIET_ZONE_MODULES;
                    boolean dark = mx >= 0 && my >= 0 && mx < matrix.getWidth() && my < matrix.getHeight()
                            && matrix.get(mx, my);
                    image.setRGB(x, y, dark ? 0x000000 : 0xFFFFFF);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to generate the QR code.", e);
        }
    }
}
