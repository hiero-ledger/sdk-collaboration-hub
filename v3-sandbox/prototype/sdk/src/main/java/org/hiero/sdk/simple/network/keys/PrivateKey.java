package org.hiero.sdk.simple.network.keys;

import org.bouncycastle.util.encoders.Hex;
import org.hiero.sdk.simple.internal.network.key.KeyFactory;
import org.jspecify.annotations.NonNull;

/**
 * Represents a private cryptographic key used in the Hiero network.
 */
public interface PrivateKey extends Key {

    /**
     * Creates a key pair from this private key.
     *
     * @return the key pair containing this private key and a corresponding public key
     */
    @NonNull
    default KeyPair createKeyPair() {
        return KeyPair.of(this);
    }

    /**
     * Creates a public key from this private key.
     *
     * @return the public key
     */
    @NonNull
    PublicKey createPublicKey();

    /**
     * Signs a message using this private key.
     *
     * @param message the message to sign
     * @return the signature of the message
     */
    @NonNull
    byte[] sign(@NonNull byte[] message);

    /**
     * Creates a view of this key as a {@link java.security.PrivateKey} with the given encoding format.
     *
     * @param encoding the encoding/format to use for the returned key
     * @return a Java Security PrivateKey view backed by this key
     */
    default java.security.@NonNull PrivateKey toJavaKey(@NonNull final KeyEncoding encoding) {
        return KeyFactory.toJavaPrivateKey(this, encoding);
    }

    /**
     * Generates a new private key using the specified algorithm.
     *
     * @param algorithm the key algorithm to use
     * @return a new private key
     */
    @NonNull
    static PrivateKey generate(KeyAlgorithm algorithm) {
        return KeyFactory.createPrivateKey(algorithm);
    }

    /**
     * Creates a private key from a hexadecimal string.
     *
     * @param privateKey the hex-encoded private key (with or without 0x prefix)
     * @return the private key
     */
    //TODO: Do we really want to have that method without the definition of the KeyAlgorithm and KeyEncoding?
    @NonNull
    static PrivateKey from(String privateKey) {
        final byte[] privateKeyBytes = Hex.decode(
                privateKey.startsWith("0x") ? privateKey.substring(2) : privateKey);
        return from(privateKeyBytes);
    }

    /**
     * Creates a private key from its binary representation.
     *
     * @param privateKey the encoded private key bytes
     * @return the private key
     */
    //TODO: Do we really want to have that method without the definition of the KeyAlgorithm and KeyEncoding?
    @NonNull
    static PrivateKey from(byte[] privateKey) {
        return KeyFactory.createPrivateKey(privateKey);
    }

    /**
     * Creates a Hiero {@code PrivateKey} from a Java Security {@link java.security.PrivateKey}.
     *
     * @param privateKey the Java Security private key
     * @return the corresponding Hiero private key
     * @throws IllegalArgumentException if the given key cannot be encoded
     */
    @NonNull
    static PrivateKey from(java.security.@NonNull PrivateKey privateKey) {
        return KeyFactory.createPrivateKey(privateKey);
    }

}
