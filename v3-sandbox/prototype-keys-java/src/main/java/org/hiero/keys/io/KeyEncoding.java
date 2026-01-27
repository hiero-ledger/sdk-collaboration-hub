package org.hiero.keys.io;

import org.hiero.keys.KeyType;
import org.hiero.keys.impl.DerUtil;
import org.hiero.keys.impl.PemUtil;

/**
 * Supported encodings that can be used together with a container format.
 */
public enum KeyEncoding {
    DER(RawFormat.BYTES),
    PEM(RawFormat.STRING);

    private final RawFormat format;

    KeyEncoding(RawFormat format) {
        this.format = format;
    }

    public RawFormat getFormat() {
        return format;
    }

    public byte[] decode(final KeyType keyType, final String value) {
        return switch (this) {
            case DER -> DerUtil.decode(value);
            case PEM -> PemUtil.fromPem(keyType, value);
        };
    }
}
