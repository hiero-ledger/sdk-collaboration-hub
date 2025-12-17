package org.hiero.keys.impl;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.hiero.keys.*;

import java.io.IOException;
import java.util.Objects;

public final class KeyFactory {

    private KeyFactory() {}

    // Generation
    public static PrivateKey generatePrivateKey(final KeyAlgorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        return switch (algorithm) {
            case ED25519 -> new Ed25519PrivateKey(randomEd25519Seed());
            case ECDSA -> EcdsaPrivateKey.generate();
        };
    }

    // Create from raw bytes
    public static PrivateKey createPrivateKey(final KeyAlgorithm algorithm, final byte[] rawBytes) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        Objects.requireNonNull(rawBytes, "rawBytes must not be null");
        return switch (algorithm) {
            case ED25519 -> new Ed25519PrivateKey(requireLen(rawBytes, 32));
            case ECDSA -> EcdsaPrivateKey.fromRaw(requireLen(rawBytes, 32));
        };
    }

    public static PublicKey createPublicKey(final KeyAlgorithm algorithm, final byte[] rawBytes) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        Objects.requireNonNull(rawBytes, "rawBytes must not be null");
        return switch (algorithm) {
            case ED25519 -> new Ed25519PublicKey(requireLen(rawBytes, 32));
            case ECDSA -> new EcdsaPublicKey(rawBytes);
        };
    }

    // Create from algorithm + string (HEX/BASE64)
    public static PrivateKey createPrivateKey(final KeyAlgorithm algorithm, final ByteImportEncoding encoding, final String value) {
        return createPrivateKey(algorithm, decodeBytes(encoding, value));
    }

    public static PublicKey createPublicKey(final KeyAlgorithm algorithm, final ByteImportEncoding encoding, final String value) {
        return createPublicKey(algorithm, decodeBytes(encoding, value));
    }

    // Create from container + bytes
    public static PrivateKey createPrivateKey(final EncodedKeyContainer container, final byte[] value) {
        Objects.requireNonNull(container, "container must not be null");
        Objects.requireNonNull(value, "value must not be null");
        if (container.format() != RawFormat.BYTES) {
            throw new IllegalArgumentException("Expected BYTES for container " + container);
        }
        return switch (container) {
            case PKCS8_WITH_DER -> parsePkcs8Der(value);
            case PKCS8_WITH_PEM, SPKI_WITH_DER, SPKI_WITH_PEM -> throw new IllegalArgumentException("Container not valid for private key: " + container);
        };
    }

    public static PublicKey createPublicKey(final EncodedKeyContainer container, final byte[] value) {
        Objects.requireNonNull(container, "container must not be null");
        Objects.requireNonNull(value, "value must not be null");
        if (container.format() != RawFormat.BYTES) {
            throw new IllegalArgumentException("Expected BYTES for container " + container);
        }
        return switch (container) {
            case SPKI_WITH_DER -> parseSpkiDer(value);
            case SPKI_WITH_PEM, PKCS8_WITH_DER, PKCS8_WITH_PEM -> throw new IllegalArgumentException("Container not valid for public key: " + container);
        };
    }

    // Create from container + string
    public static PrivateKey createPrivateKey(final EncodedKeyContainer container, final String value) {
        Objects.requireNonNull(container, "container must not be null");
        Objects.requireNonNull(value, "value must not be null");

        if(!container.supportsType(KeyType.PRIVATE)) {
            throw new IllegalArgumentException("Container not valid for private key: " + container);
        }

        return switch (container) {
            case PKCS8_WITH_PEM -> parsePkcs8Der(PemUtil.fromPem("PRIVATE KEY", value));
            case PKCS8_WITH_DER -> parsePkcs8Der(container.decode(value));
            default -> throw new IllegalArgumentException("Container not supported for private key: " + container);
        };
    }

    public static PublicKey createPublicKey(final EncodedKeyContainer container, final String value) {
        Objects.requireNonNull(container, "container must not be null");
        Objects.requireNonNull(value, "value must not be null");
        if (container.format() != RawFormat.STRING) {
            throw new IllegalArgumentException("Expected STRING for container " + container);
        }
        return switch (container) {
            case SPKI_WITH_PEM -> parseSpkiDer(PemUtil.fromPem("PUBLIC KEY", value));
            case PKCS8_WITH_PEM, PKCS8_WITH_DER, SPKI_WITH_DER -> throw new IllegalArgumentException("Container not valid for public key: " + container);
        };
    }

    // Helpers
    private static byte[] requireLen(final byte[] data, final int len) {
        if (data.length != len) throw new IllegalArgumentException("Expected length " + len + ", was " + data.length);
        return data;
    }

    private static byte[] decodeBytes(final ByteImportEncoding encoding, final String value) {
        Objects.requireNonNull(encoding, "encoding must not be null");
        Objects.requireNonNull(value, "value must not be null");
        return switch (encoding) {
            case HEX -> Hex.decode(value);
            case BASE64 -> Base64Util.decode(value);
        };
    }

    private static PrivateKey parsePkcs8Der(final byte[] der) {
        try {
            final PrivateKeyInfo info = PrivateKeyInfo.getInstance(der);
            final String algOid = info.getPrivateKeyAlgorithm().getAlgorithm().getId();
            if (org.hiero.keys.impl.Oids.ID_ED25519.getId().equals(algOid)) {
                final byte[] octets = ((org.bouncycastle.asn1.ASN1OctetString) info.parsePrivateKey()).getOctets();
                return new Ed25519PrivateKey(requireLen(octets, 32));
            }
            if (org.hiero.keys.impl.Oids.ID_EC_PUBLIC_KEY.getId().equals(algOid)) {
                return EcdsaPrivateKey.fromPkcs8Der(der);
            }
            throw new IllegalArgumentException("Unsupported PKCS#8 algorithm OID: " + algOid);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid PKCS#8 DER", e);
        }
    }

    private static PublicKey parseSpkiDer(final byte[] der) {
        try {
            final SubjectPublicKeyInfo spki = SubjectPublicKeyInfo.getInstance(der);
            final String algOid = spki.getAlgorithm().getAlgorithm().getId();
            if (org.hiero.keys.impl.Oids.ID_ED25519.getId().equals(algOid)) {
                return new Ed25519PublicKey(spki.getPublicKeyData().getBytes());
            }
            if (org.hiero.keys.impl.Oids.ID_EC_PUBLIC_KEY.getId().equals(algOid)) {
                return EcdsaPublicKey.fromSpkiDer(der);
            }
            throw new IllegalArgumentException("Unsupported SPKI algorithm OID: " + algOid);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SPKI DER", e);
        }
    }

    private static byte[] randomEd25519Seed() {
        final java.security.SecureRandom rnd = new java.security.SecureRandom();
        final byte[] seed = new byte[32];
        rnd.nextBytes(seed);
        return seed;
    }
}
