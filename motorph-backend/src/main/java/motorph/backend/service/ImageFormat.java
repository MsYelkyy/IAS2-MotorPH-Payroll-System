package motorph.backend.service;

import java.util.List;
import java.util.zip.CRC32;

/**
 * Image types allowed for profile photos. The type is read from the first bytes of the file, not
 * from the file name. If the header is wrong or cut off there is no type, so a text file or a
 * broken image renamed to .png is rejected.
 */
enum ImageFormat {

    PNG("png"),
    JPEG("jpg", "jpeg"),
    GIF("gif");

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    // 8-byte signature + IHDR chunk (length, type, 13 data bytes, CRC).
    private static final int PNG_HEADER_LENGTH = 33;

    // "GIF87a" or "GIF89a", then width, height, flags, background and aspect ratio.
    private static final int GIF_HEADER_LENGTH = 13;

    private final List<String> extensions;

    ImageFormat(String... extensions) {
        this.extensions = List.of(extensions);
    }

    /**
     * True if the extension (in lower case) is one of this format's extensions.
     */
    boolean matchesExtension(String extension) {
        return extensions.contains(extension);
    }

    /**
     * Returns the format, or null if the content is not a supported image or its header is broken
     * or cut off.
     */
    static ImageFormat detect(byte[] content) {
        if (content == null) {
            return null;
        }

        if (isPng(content)) {
            return PNG;
        }

        if (isJpeg(content)) {
            return JPEG;
        }

        if (isGif(content)) {
            return GIF;
        }

        return null;
    }

    private static boolean isPng(byte[] b) {
        if (b.length < PNG_HEADER_LENGTH || !startsWith(b, PNG_SIGNATURE)) {
            return false;
        }

        if (bigEndianInt(b, 8) != 13 || !hasAscii(b, 12, "IHDR")) {
            return false;
        }

        // PNG does not allow a width or height of 0 or more than 2^31 - 1.
        if (bigEndianInt(b, 16) <= 0 || bigEndianInt(b, 20) <= 0) {
            return false;
        }

        CRC32 crc = new CRC32();
        crc.update(b, 12, 17);

        return crc.getValue() == (bigEndianInt(b, 29) & 0xFFFFFFFFL);
    }

    /**
     * Goes through the segments after the start marker until it reaches the frame header. The header
     * must fit inside the file and have a width and height above zero.
     */
    private static boolean isJpeg(byte[] b) {
        if (b.length < 4 || (b[0] & 0xFF) != 0xFF || (b[1] & 0xFF) != 0xD8) {
            return false;
        }

        int pos = 2;

        while (pos + 4 <= b.length) {
            if ((b[pos] & 0xFF) != 0xFF) {
                return false;
            }

            int marker = b[pos + 1] & 0xFF;

            if (marker == 0xFF) {
                pos++;
                continue;
            }

            // These markers are not allowed before the frame header (RSTn, SOI, EOI, SOS and so on).
            if (marker == 0x00 || marker == 0x01 || (marker >= 0xD0 && marker <= 0xDA)) {
                return false;
            }

            int length = bigEndianShort(b, pos + 2);

            if (length < 2) {
                return false;
            }

            if (isStartOfFrame(marker)) {
                // Length, precision, height, width and component count need at least 8 bytes.
                return length >= 8
                        && pos + 2 + length <= b.length
                        && bigEndianShort(b, pos + 5) > 0
                        && bigEndianShort(b, pos + 7) > 0;
            }

            pos += 2 + length;
        }

        return false;
    }

    private static boolean isGif(byte[] b) {
        if (b.length < GIF_HEADER_LENGTH || !(hasAscii(b, 0, "GIF87a") || hasAscii(b, 0, "GIF89a"))) {
            return false;
        }

        return littleEndianShort(b, 6) > 0 && littleEndianShort(b, 8) > 0;
    }

    // SOF0 to SOF15, but not C4 (DHT), C8 (reserved) or CC (DAC).
    private static boolean isStartOfFrame(int marker) {
        return marker >= 0xC0 && marker <= 0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC;
    }

    private static boolean startsWith(byte[] content, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (content[i] != prefix[i]) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasAscii(byte[] content, int offset, String text) {
        if (offset + text.length() > content.length) {
            return false;
        }

        for (int i = 0; i < text.length(); i++) {
            if (content[offset + i] != (byte) text.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    private static int bigEndianInt(byte[] b, int offset) {
        return ((b[offset] & 0xFF) << 24)
                | ((b[offset + 1] & 0xFF) << 16)
                | ((b[offset + 2] & 0xFF) << 8)
                | (b[offset + 3] & 0xFF);
    }

    private static int bigEndianShort(byte[] b, int offset) {
        return ((b[offset] & 0xFF) << 8) | (b[offset + 1] & 0xFF);
    }

    private static int littleEndianShort(byte[] b, int offset) {
        return (b[offset] & 0xFF) | ((b[offset + 1] & 0xFF) << 8);
    }
}
