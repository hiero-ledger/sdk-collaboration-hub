package org.hiero.sdk.simple.network.keys;

/**
 * Encoding formats supported when serializing keys.
 */
public enum KeyEncoding {
    /** Raw binary key material (algorithm-specific). */
    RAW,
    /** Distinguished Encoding Rules (X.690) ASN.1 format. */
    DER;
}
