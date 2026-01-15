package org.hiero.keys;

import org.hiero.keys.impl.KeyFactory;

import static org.hiero.keys.EncodedKeyContainer.SPKI_WITH_PEM;

public interface PublicKey extends Key {

    boolean verify(final byte[] message, final byte[] signature);

    static PublicKey create(final KeyAlgorithm algorithm, final byte[] rawBytes) {
        return KeyFactory.createPublicKey(algorithm, rawBytes);
    }

    static PublicKey create(final KeyAlgorithm algorithm, final ByteImportEncoding encoding, final String value) {
        return KeyFactory.createPublicKey(algorithm, encoding, value);
    }

    static PublicKey create(final EncodedKeyContainer container, final byte[] value) {
        return KeyFactory.createPublicKey(container, value);
    }

    static PublicKey create(final EncodedKeyContainer container, final String value) {
        return KeyFactory.createPublicKey(container, value);
    }

    static PublicKey create(final String value) {
        return create(SPKI_WITH_PEM, value);
    }


}
