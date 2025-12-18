package org.hiero.keys;

import org.hiero.keys.impl.Hex;

import java.util.Objects;

/**
 * Combination of a {@link KeyContainer} with a {@link KeyEncoding} and the raw {@link RawFormat} of the import/export value.
 */
public enum EncodedKeyContainer {
    PKCS8_WITH_DER(KeyContainer.PKCS8, KeyEncoding.DER, RawFormat.BYTES),
    SPKI_WITH_DER(KeyContainer.SPKI, KeyEncoding.DER, RawFormat.BYTES),
    PKCS8_WITH_PEM(KeyContainer.PKCS8, KeyEncoding.PEM, RawFormat.STRING),
    SPKI_WITH_PEM(KeyContainer.SPKI, KeyEncoding.PEM, RawFormat.STRING);

    private final KeyContainer container;
    private final KeyEncoding encoding;
    private final RawFormat format;

    EncodedKeyContainer(final KeyContainer container, final KeyEncoding encoding, final RawFormat format) {
        this.container = Objects.requireNonNull(container, "container must not be null");
        this.encoding = Objects.requireNonNull(encoding, "encoding must not be null");
        this.format = Objects.requireNonNull(format, "format must not be null");
    }

    public KeyContainer container() {
        return container;
    }

    public KeyEncoding encoding() {
        return encoding;
    }

    public RawFormat format() {
        return format;
    }

    public String encode(final byte[] bytes) {
        return Hex.encode(bytes);
    }

    public byte[] decode(final String encoded) {
        return Hex.decode(encoded);
    }

    public boolean supportsType(final KeyType type) {
        return container.supportsType(type);
    }
}
