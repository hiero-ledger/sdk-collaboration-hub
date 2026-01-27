package org.hiero.keys;

import org.hiero.keys.impl.KeyFactory;
import org.hiero.keys.io.ByteImportEncoding;
import org.hiero.keys.io.KeyFormat;

import static org.hiero.keys.io.KeyFormat.PKCS8_WITH_PEM;

public interface PrivateKey extends Key {
    byte[] sign(final byte[] message);

    PublicKey createPublicKey();

    static PrivateKey generate(final KeyAlgorithm algorithm) {
        return KeyFactory.generatePrivateKey(algorithm);
    }

    static PrivateKey create(final KeyAlgorithm algorithm, final byte[] rawBytes) {
        return KeyFactory.createPrivateKey(algorithm, rawBytes);
    }

    static PrivateKey create(final KeyAlgorithm algorithm, final ByteImportEncoding encoding, final String value) {
        return KeyFactory.createPrivateKey(algorithm, encoding, value);
    }

    static PrivateKey create(final KeyFormat format, final byte[] value) {
        return KeyFactory.createPrivateKey(format, value);
    }

    static PrivateKey create(final KeyFormat format, final String value) {
        return KeyFactory.createPrivateKey(format, value);
    }

    static PrivateKey create(final String value) {
        return create(PKCS8_WITH_PEM, value);
    }
}
