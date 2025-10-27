package org.hiero.sdk.simple.network.keys;

import org.jspecify.annotations.NonNull;

/**
 * Represents a cryptographic key used in the Hiero network.
 */
public interface Key {

    /**
     * Converts the key to a byte array using the specified encoding.
     *
     * @param encoding the encoding to use
     * @return the byte array representation of the key
     */
    @NonNull
    byte[] toBytes(@NonNull KeyEncoding encoding);

    /**
     * Returns the algorithm used by this key.
     *
     * @return the key algorithm
     */
    @NonNull
    KeyAlgorithm algorithm();

    /**
     * Creates a view of this key as a {@link java.security.Key} with the given encoding format.
     *
     * @param encoding the encoding/format to use for the returned key
     * @return a Java Security Key view backed by this key
     */
    java.security.@NonNull Key toJavaKey(@NonNull final KeyEncoding encoding);
}
