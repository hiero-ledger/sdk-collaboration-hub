package org.hiero.sdk.simple.network.keys;

/**
 * Supported asymmetric key algorithms for the Hiero SDK.
 */
public enum KeyAlgorithm {
    /** Edwards-curve Digital Signature Algorithm using Curve25519. */
    ED25519,
    /** Elliptic Curve Digital Signature Algorithm (secp curves). */
    ECDSA;
}
