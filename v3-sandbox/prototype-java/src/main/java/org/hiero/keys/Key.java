package org.hiero.keys;

import org.hiero.keys.io.KeyFormat;
import org.jspecify.annotations.NonNull;

/**
 * Abstract base for cryptographic keys. Mirrors the meta-language abstraction
 * {@code keys.Key}.
 *
 * <p>The meta-language defines three immutable attributes ({@code bytes},
 * {@code algorithm}, {@code type}). They are exposed as accessor methods on the
 * interface so concrete implementations can decide how to store the underlying
 * raw bytes.
 *
 * <p>The two {@code toBytes(EncodedKeyContainer)} / {@code toString(EncodedKeyContainer)}
 * overloads in the meta-language collapse to a single {@link KeyFormat} parameter in
 * Java — the meta-language definition for {@code keys.Key} contains both the older
 * {@code EncodedKeyContainer} and the newer {@link KeyFormat} naming side-by-side
 * (see REPORT.md).
 */
public interface Key {

    /**
     * Returns the raw bytes of the key in its native ({@code RAW}) encoding.
     *
     * @return a defensive copy of the raw key bytes
     */
    @NonNull
    byte[] toRawBytes();

    /**
     * @return the algorithm this key belongs to
     */
    @NonNull
    KeyAlgorithm algorithm();

    /**
     * @return whether this is a public or a private key
     */
    @NonNull
    KeyType type();

    /**
     * Encodes the key as bytes using the given format. The format's
     * {@link KeyFormat#getRawFormat()} must be {@link org.hiero.keys.io.RawFormat#BYTES},
     * otherwise an {@link IllegalArgumentException} is thrown (Java mapping of
     * {@code @@throws(illegal-format)}).
     *
     * @param container the key format to encode with
     * @return the encoded bytes
     */
    @NonNull
    byte[] toBytes(@NonNull KeyFormat container);

    /**
     * Encodes the key as a string using the given format. The format's
     * {@link KeyFormat#getRawFormat()} must be {@link org.hiero.keys.io.RawFormat#STRING},
     * otherwise an {@link IllegalArgumentException} is thrown (Java mapping of
     * {@code @@throws(illegal-format)}).
     *
     * @param container the key format to encode with
     * @return the encoded string
     */
    @NonNull
    String toString(@NonNull KeyFormat container);
}
