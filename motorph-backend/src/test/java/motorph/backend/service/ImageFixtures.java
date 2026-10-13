package motorph.backend.service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Real PNG, JPEG and GIF files made with the JDK's own encoders, so the checks run against real
 * images and not hand-made bytes.
 */
public final class ImageFixtures {

    private ImageFixtures() {
    }

    public static byte[] png() {
        return encode("png");
    }

    public static byte[] jpeg() {
        return encode("jpg");
    }

    public static byte[] gif() {
        return encode("gif");
    }

    /** A progressive JPEG. Its frame header uses a different marker from a normal JPEG. */
    public static byte[] progressiveJpeg() {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image(), null, null), param);
            stream.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            writer.dispose();
        }
    }

    private static byte[] encode(String format) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            if (!ImageIO.write(image(), format, out)) {
                throw new IllegalStateException("No encoder for " + format);
            }

            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static BufferedImage image() {
        BufferedImage image = new BufferedImage(8, 6, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, (x * 30) << 16 | (y * 40) << 8 | 0x80);
            }
        }

        return image;
    }
}
