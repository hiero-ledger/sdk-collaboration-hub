package org.hiero.keys;

import java.util.Objects;

/**
 * Combination of a {@link KeyContainer} with a {@link KeyEncoding} and the raw {@link RawFormat} of the import/export value.
 */
public enum KeyFormat {
    PKCS8_WITH_DER(KeyContainer.PKCS8, KeyEncoding.DER),
    SPKI_WITH_DER(KeyContainer.SPKI, KeyEncoding.DER),
    PKCS8_WITH_PEM(KeyContainer.PKCS8, KeyEncoding.PEM),
    SPKI_WITH_PEM(KeyContainer.SPKI, KeyEncoding.PEM);

    private final KeyContainer container;
    private final KeyEncoding encoding;

    KeyFormat(final KeyContainer container, final KeyEncoding encoding) {
        this.container = Objects.requireNonNull(container, "container must not be null");
        this.encoding = Objects.requireNonNull(encoding, "encoding must not be null");
    }

    public KeyContainer container() {
        return container;
    }

    public KeyEncoding encoding() {
        return encoding;
    }

    public byte[] decode(final KeyType keyType, final String value) {
        return encoding.decode(keyType, value);
    }

    public boolean supportsType(final KeyType type) {
        return container.supportsType(type);
    }
}
