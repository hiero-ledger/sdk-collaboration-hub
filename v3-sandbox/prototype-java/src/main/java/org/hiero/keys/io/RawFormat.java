package org.hiero.keys.io;

/**
 * The raw format of an encoded key value. Mirrors the meta-language enum
 * {@code keys.io.RawFormate} (note: the original meta-language definition contains
 * a typo {@code RawFormate} — the Java type uses the corrected spelling
 * {@code RawFormat}, see REPORT.md).
 */
public enum RawFormat {

    /** String representation of the bytes in the specified encoding. */
    STRING,

    /** Raw bytes. */
    BYTES
}
