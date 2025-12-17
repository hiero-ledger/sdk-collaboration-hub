package org.hiero.keys;

import org.hiero.keys.impl.KeyFactory;

import static org.hiero.keys.EncodedKeyContainer.PKCS8_WITH_PEM;

public interface PrivateKey extends Key {
    byte[] sign(final byte[] message);

    PublicKey createPublicKey();

    static PrivateKey generate(final KeyAlgorithm algorithm) {
        return KeyFactory.generatePrivateKey(algorithm);
    }

    static PrivateKey create(final KeyAlgorithm algorithm, final byte[] rawBytes) {
        return KeyFactory.createPrivateKey(algorithm, rawBytes);
    }

    // Create from container + bytes
    static PrivateKey create(final EncodedKeyContainer container, final byte[] value) {
       return KeyFactory.createPrivateKey(container, value);
    }

    // Create from container + string
    static PrivateKey create(final EncodedKeyContainer container, final String value) {
       return KeyFactory.createPrivateKey(container, value);
    }

    // Create from algorithm + string (HEX/BASE64)
    static PrivateKey create(final KeyAlgorithm algorithm, final ByteImportEncoding encoding, final String value) {
        return KeyFactory.createPrivateKey(algorithm, encoding, value);
    }

    // Shortcuts default: string -> PKCS#8 PEM / SPKI PEM
    static PrivateKey create(final String value) {
        return create(PKCS8_WITH_PEM, value);
    }
}
