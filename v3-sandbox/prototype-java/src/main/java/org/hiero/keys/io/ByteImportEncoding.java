package org.hiero.keys.io;

import java.util.Base64;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Encoding information for byte import. Mirrors the meta-language enum
 * {@code keys.io.ByteImportEncoding}.
 */
public enum ByteImportEncoding {

    /** Hex string representation of the bytes. */
    HEX,

    /** Base64 string representation of the bytes. */
    BASE64;

    /**
     * Decodes the given encoded string value into raw bytes.
     *
     * @param value the encoded string
     * @return the decoded bytes
     */
    @NonNull
    public byte[] decode(@NonNull final String value) {
        Objects.requireNonNull(value, "value must not be null");
        return switch (this) {
            case HEX -> decodeHex(value);
            case BASE64 -> Base64.getDecoder().decode(value);
        };
    }

    private static byte[] decodeHex(@NonNull final String hex) {
        final String s = (hex.startsWith("0x") || hex.startsWith("0X"))
                ? hex.substring(2)
                : hex;
        final String cleaned = s.replace(" ", "").replace("\n", "").replace("\r", "");
        final int len = cleaned.length();
        if ((len & 1) != 0) {
            throw new IllegalArgumentException("Hex string has odd length: " + len);
        }
        final byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            final int hi = Character.digit(cleaned.charAt(i), 16);
            final int lo = Character.digit(cleaned.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) {
                throw new IllegalArgumentException("Invalid hex char at position " + i);
            }
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
    }
}
