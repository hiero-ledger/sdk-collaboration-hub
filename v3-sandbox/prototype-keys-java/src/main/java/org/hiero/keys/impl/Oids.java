package org.hiero.keys.impl;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

public final class Oids {
    private Oids() {}

    // id-Ed25519 1.3.101.112
    public static final ASN1ObjectIdentifier ID_ED25519 = new ASN1ObjectIdentifier("1.3.101.112");

    // id-ecPublicKey 1.2.840.10045.2.1
    public static final ASN1ObjectIdentifier ID_EC_PUBLIC_KEY = new ASN1ObjectIdentifier("1.2.840.10045.2.1");

    // secp256k1 1.3.132.0.10
    public static final ASN1ObjectIdentifier ID_SECP256K1 = new ASN1ObjectIdentifier("1.3.132.0.10");
}
