package org.hiero.keys;

import java.util.Objects;
import org.hiero.keys.impl.KeyFactoryImpl;
import org.hiero.keys.io.ByteImportEncoding;
import org.hiero.keys.io.KeyFormat;
import org.jspecify.annotations.NonNull;

/**
 * Private key abstraction. Mirrors the meta-language type {@code keys.PrivateKey}.
 */
public interface PrivateKey extends Key {

    /**
     * Signs the given message with this private key.
     *
     * @param message the message to sign
     * @return the signature bytes
     */
    @NonNull
    byte[] sign(@NonNull byte[] message);

    /**
     * Derives the corresponding public key. Always returns a new {@link PublicKey}
     * instance — the private key never caches the derived public key.
     *
     * @return the corresponding public key
     */
    @NonNull
    PublicKey createPublicKey();

    // ---- Factory methods (mapping of namespace-level functions) ----

    /**
     * Generates a new random private key for the given algorithm.
     */
    @NonNull
    static PrivateKey generatePrivateKey(@NonNull final KeyAlgorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        return KeyFactoryImpl.generatePrivateKey(algorithm);
    }

    /**
     * Creates a {@link PrivateKey} from raw bytes for a given algorithm.
     */
    @NonNull
    static PrivateKey createPrivateKey(@NonNull final KeyAlgorithm algorithm, @NonNull final byte[] rawBytes) {
        return KeyFactoryImpl.createPrivateKey(algorithm, rawBytes);
    }

    /**
     * Creates a {@link PrivateKey} from a string in the given byte-import encoding.
     */
    @NonNull
    static PrivateKey createPrivateKey(
            @NonNull final KeyAlgorithm algorithm,
            @NonNull final ByteImportEncoding encoding,
            @NonNull final String value) {
        return KeyFactoryImpl.createPrivateKey(algorithm, encoding, value);
    }

    /**
     * Creates a {@link PrivateKey} from a binary container value.
     */
    @NonNull
    static PrivateKey createPrivateKey(@NonNull final KeyFormat container, @NonNull final byte[] value) {
        return KeyFactoryImpl.createPrivateKey(container, value);
    }

    /**
     * Creates a {@link PrivateKey} from a string container value.
     */
    @NonNull
    static PrivateKey createPrivateKey(@NonNull final KeyFormat container, @NonNull final String value) {
        return KeyFactoryImpl.createPrivateKey(container, value);
    }

    /**
     * Convenience: creates a {@link PrivateKey} from a PKCS#8 PEM string. Mapping of
     * the meta-language convenience factory {@code createPrivateKey(value: string)}.
     */
    @NonNull
    static PrivateKey createPrivateKey(@NonNull final String value) {
        return createPrivateKey(KeyFormat.PKCS8_WITH_PEM, value);
    }
}
