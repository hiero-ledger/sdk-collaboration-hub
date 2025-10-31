package org.hiero.sdk.simple.network.keys;

import org.hiero.sdk.simple.internal.network.key.KeyFactory;
import org.jspecify.annotations.NonNull;

/**
 * Represents a public key in the Hiero network.
 */
public interface PublicKey extends Key {

    /**
     * Verifies a signature against a message using this public key.
     *
     * @param message   the original message
     * @param signature the signature to verify
     * @return true if the signature is valid for the message, false otherwise
     */
    boolean verify(@NonNull byte[] message, @NonNull byte[] signature);

    /**
     * Creates a view of this key as a {@link java.security.PublicKey} with the given encoding format.
     *
     * @param encoding the encoding/format to use for the returned key
     * @return a Java Security PublicKey view backed by this key
     */
    default java.security.@NonNull PublicKey toJavaKey(@NonNull final KeyEncoding encoding) {
        return KeyFactory.toJavaPublicKey(this, encoding);
    }

    /**
     * Creates a Hiero {@code PublicKey} from a Java Security {@link java.security.PublicKey}.
     *
     * @param publicKey the Java Security public key
     * @return the corresponding Hiero public key
     * @throws IllegalArgumentException if the given key cannot be encoded
     */
    static PublicKey from(final java.security.@NonNull PublicKey publicKey) {
        return KeyFactory.createPublicKey(publicKey);
    }

    /**
     * Creates a {@code PublicKey} from a hexadecimal string.
     *
     * @param publicKey the hex-encoded public key (with or without 0x prefix)
     * @return the decoded public key
     */
    //TODO: Do we really want to have that method without the definition of the KeyAlgorithm and KeyEncoding?
    @NonNull
    static PublicKey from(KeyAlgorithm algorithm, KeyEncoding encoding, @NonNull final String publicKey) {
        final byte[] bytes = KeyFactory.decode(publicKey, encoding);
        return from(algorithm, bytes);
    }

    /**
     * Creates a {@code PublicKey} from its binary representation.
     *
     * @param publicKey the encoded public key bytes
     * @return the decoded public key
     */
    //TODO: Do we really want to have that method without the definition of the KeyAlgorithm and KeyEncoding?
    @NonNull
    static PublicKey from(KeyAlgorithm algorithm, @NonNull byte[] publicKey) {
        return KeyFactory.createPublicKey(algorithm, publicKey);
    }
}
