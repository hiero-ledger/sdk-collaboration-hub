package org.hiero.keys.io;

/**
 * Import encodings for string representations of bytes.
 */
public enum ByteImportEncoding {
    HEX,
    BASE64;

    public byte[] decode(final String value) {
        return switch (this) {
            case HEX -> org.hiero.keys.impl.Hex.decode(value);
            case BASE64 -> java.util.Base64.getDecoder().decode(value);
        };
    }
}
