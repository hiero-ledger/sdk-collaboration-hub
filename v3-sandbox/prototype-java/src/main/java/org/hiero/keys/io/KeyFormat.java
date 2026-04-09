package org.hiero.keys.io;

import java.util.Objects;
import org.hiero.keys.KeyType;
import org.jspecify.annotations.NonNull;

/**
 * Combined container format and encoding. Mirrors the meta-language enum
 * {@code keys.io.KeyFormat}.
 */
public enum KeyFormat {

    PKCS8_WITH_DER(KeyContainer.PKCS8, KeyEncoding.DER),
    SPKI_WITH_DER(KeyContainer.SPKI, KeyEncoding.DER),
    PKCS8_WITH_PEM(KeyContainer.PKCS8, KeyEncoding.PEM),
    SPKI_WITH_PEM(KeyContainer.SPKI, KeyEncoding.PEM);

    private final KeyContainer container;

    private final KeyEncoding encoding;

    KeyFormat(@NonNull final KeyContainer container, @NonNull final KeyEncoding encoding) {
        this.container = container;
        this.encoding = encoding;
    }

    /**
     * @return the container of this format
     */
    @NonNull
    public KeyContainer getContainer() {
        return container;
    }

    /**
     * @return the encoding of this format
     */
    @NonNull
    public KeyEncoding getEncoding() {
        return encoding;
    }

    /**
     * @return the raw format produced by this format's encoding
     */
    @NonNull
    public RawFormat getRawFormat() {
        return encoding.getRawFormat();
    }

    /**
     * Returns whether this format can hold a key of the given type.
     *
     * @param type the key type to check
     * @return {@code true} if the underlying container supports the given key type
     */
    public boolean supportsType(@NonNull final KeyType type) {
        Objects.requireNonNull(type, "type must not be null");
        return container.supportsType(type);
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
        return encoding.decode(keyType, value);
    }
}
