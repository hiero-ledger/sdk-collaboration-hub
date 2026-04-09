package org.hiero.keys;

/**
 * Cryptographic key algorithms. Mirrors the meta-language enum
 * {@code keys.KeyAlgorithm}.
 */
public enum KeyAlgorithm {

    /** Edwards-curve Digital Signature Algorithm (Ed25519). */
    ED25519,

    /** Elliptic Curve Digital Signature Algorithm (secp256k1 curve). */
    ECDSA
}
