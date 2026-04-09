package org.hiero.keys.io;

import java.util.Objects;
import org.hiero.keys.KeyType;
import org.jspecify.annotations.NonNull;

/**
 * Supported encodings. Mirrors the meta-language enum {@code keys.io.KeyEncoding}.
 */
public enum KeyEncoding {

    /** Distinguished Encoding Rules. Produces raw bytes. */
    DER(RawFormat.BYTES),

    /** Privacy Enhanced Mail. Produces a string. */
    PEM(RawFormat.STRING);

    private final RawFormat rawFormat;

    KeyEncoding(@NonNull final RawFormat rawFormat) {
        this.rawFormat = rawFormat;
    }

    /**
     * Returns the raw format produced by this encoding.
     *
     * @return the raw format
     */
    @NonNull
    public RawFormat getRawFormat() {
        return rawFormat;
    }

    /**
     * Decodes the given string value into raw bytes for the given key type.
     *
     * @param keyType the key type the value belongs to
     * @param value   the encoded string value
     * @return the decoded raw bytes
     */
    @NonNull
    public byte[] decode(@NonNull final KeyType keyType, @NonNull final String value) {
        Objects.requireNonNull(keyType, "keyType must not be null");
        Objects.requireNonNull(value, "value must not be null");
        return switch (this) {
            // For DER we delegate to the hex helper since DER bytes are typically
            // exchanged as hex when transported as a string.
            case DER -> ByteImportEncoding.HEX.decode(value);
            case PEM -> org.hiero.keys.impl.PemUtil.fromPem(keyType, value);
        };
    }
}
