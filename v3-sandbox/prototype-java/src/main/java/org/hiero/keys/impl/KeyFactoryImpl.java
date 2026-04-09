package org.hiero.keys.impl;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Objects;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.hiero.keys.KeyAlgorithm;
import org.hiero.keys.KeyType;
import org.hiero.keys.PrivateKey;
import org.hiero.keys.PublicKey;
import org.hiero.keys.io.ByteImportEncoding;
import org.hiero.keys.io.KeyFormat;
import org.hiero.keys.io.RawFormat;
import org.jspecify.annotations.NonNull;

/**
 * Internal factory used by the static helpers on {@link PrivateKey} / {@link PublicKey}.
 * Adapted from {@code prototype-keys-java}.
 */
public final class KeyFactoryImpl {

    private KeyFactoryImpl() {
    }

    @NonNull
    public static PrivateKey generatePrivateKey(@NonNull final KeyAlgorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        return switch (algorithm) {
            case ED25519 -> new Ed25519PrivateKey(randomSeed32());
            case ECDSA -> EcdsaPrivateKey.generate();
        };
    }

    @NonNull
    public static PrivateKey createPrivateKey(@NonNull final KeyAlgorithm algorithm, @NonNull final byte[] rawBytes) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        Objects.requireNonNull(rawBytes, "rawBytes must not be null");
        return switch (algorithm) {
            case ED25519 -> new Ed25519PrivateKey(requireLen(rawBytes, 32));
            case ECDSA -> EcdsaPrivateKey.fromRaw(requireLen(rawBytes, 32));
        };
    }

    @NonNull
    public static PublicKey createPublicKey(@NonNull final KeyAlgorithm algorithm, @NonNull final byte[] rawBytes) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        Objects.requireNonNull(rawBytes, "rawBytes must not be null");
        return switch (algorithm) {
            case ED25519 -> new Ed25519PublicKey(requireLen(rawBytes, 32));
            case ECDSA -> new EcdsaPublicKey(rawBytes);
        };
    }

    @NonNull
    public static PrivateKey createPrivateKey(
            @NonNull final KeyAlgorithm algorithm,
            @NonNull final ByteImportEncoding encoding,
            @NonNull final String value) {
        Objects.requireNonNull(encoding, "encoding must not be null");
        return createPrivateKey(algorithm, encoding.decode(value));
    }

    @NonNull
    public static PublicKey createPublicKey(
            @NonNull final KeyAlgorithm algorithm,
            @NonNull final ByteImportEncoding encoding,
            @NonNull final String value) {
        Objects.requireNonNull(encoding, "encoding must not be null");
        return createPublicKey(algorithm, encoding.decode(value));
    }

    @NonNull
    public static PrivateKey createPrivateKey(@NonNull final KeyFormat format, @NonNull final byte[] value) {
        Objects.requireNonNull(format, "format must not be null");
        Objects.requireNonNull(value, "value must not be null");
        if (!format.supportsType(KeyType.PRIVATE)) {
            throw new IllegalArgumentException("Format not valid for private key: " + format);
        }
        if (format.getRawFormat() != RawFormat.BYTES) {
            return createPrivateKey(format, new String(value));
        }
        if (format == KeyFormat.PKCS8_WITH_DER) {
            return parsePkcs8Der(value);
        }
        throw new IllegalArgumentException("Format not supported for private key: " + format);
    }

    @NonNull
    public static PublicKey createPublicKey(@NonNull final KeyFormat format, @NonNull final byte[] value) {
        Objects.requireNonNull(format, "format must not be null");
        Objects.requireNonNull(value, "value must not be null");
        if (!format.supportsType(KeyType.PUBLIC)) {
            throw new IllegalArgumentException("Format not valid for public key: " + format);
        }
        if (format.getRawFormat() != RawFormat.BYTES) {
            return createPublicKey(format, new String(value));
        }
        if (format == KeyFormat.SPKI_WITH_DER) {
            return parseSpkiDer(value);
        }
        throw new IllegalArgumentException("Format not supported for public key: " + format);
    }

    @NonNull
    public static PrivateKey createPrivateKey(@NonNull final KeyFormat format, @NonNull final String value) {
        Objects.requireNonNull(format, "format must not be null");
        Objects.requireNonNull(value, "value must not be null");
        if (!format.supportsType(KeyType.PRIVATE)) {
            throw new IllegalArgumentException("Format not valid for private key: " + format);
        }
        if (format.getRawFormat() != RawFormat.STRING) {
            return createPrivateKey(format, format.decode(KeyType.PRIVATE, value));
        }
        if (format == KeyFormat.PKCS8_WITH_PEM) {
            return parsePkcs8Der(PemUtil.fromPem(KeyType.PRIVATE, value));
        }
        throw new IllegalArgumentException("Format not supported for private key: " + format);
    }

    @NonNull
    public static PublicKey createPublicKey(@NonNull final KeyFormat format, @NonNull final String value) {
        Objects.requireNonNull(format, "format must not be null");
        Objects.requireNonNull(value, "value must not be null");
        if (!format.supportsType(KeyType.PUBLIC)) {
            throw new IllegalArgumentException("Format not supported for public key: " + format);
        }
        if (format.getRawFormat() != RawFormat.STRING) {
            return createPublicKey(format, format.decode(KeyType.PUBLIC, value));
        }
        if (format == KeyFormat.SPKI_WITH_PEM) {
            return parseSpkiDer(PemUtil.fromPem(KeyType.PUBLIC, value));
        }
        throw new IllegalArgumentException("Format not supported for public key: " + format);
    }

    private static byte[] requireLen(final byte[] data, final int len) {
        if (data.length != len) {
            throw new IllegalArgumentException("Expected length " + len + ", was " + data.length);
        }
        return data;
    }

    private static PrivateKey parsePkcs8Der(final byte[] der) {
        try {
            final PrivateKeyInfo info = PrivateKeyInfo.getInstance(der);
            final String algOid = info.getPrivateKeyAlgorithm().getAlgorithm().getId();
            if (Oids.ID_ED25519.getId().equals(algOid)) {
                final byte[] octets = ((ASN1OctetString) info.parsePrivateKey()).getOctets();
                return new Ed25519PrivateKey(requireLen(octets, 32));
            }
            if (Oids.ID_EC_PUBLIC_KEY.getId().equals(algOid)) {
                return EcdsaPrivateKey.fromPkcs8Der(der);
            }
            throw new IllegalArgumentException("Unsupported PKCS#8 algorithm OID: " + algOid);
        } catch (final IOException e) {
            throw new IllegalArgumentException("Invalid PKCS#8 DER", e);
        }
    }

    private static PublicKey parseSpkiDer(final byte[] der) {
        try {
            final SubjectPublicKeyInfo spki = SubjectPublicKeyInfo.getInstance(der);
            final String algOid = spki.getAlgorithm().getAlgorithm().getId();
            if (Oids.ID_ED25519.getId().equals(algOid)) {
                return new Ed25519PublicKey(spki.getPublicKeyData().getBytes());
            }
            if (Oids.ID_EC_PUBLIC_KEY.getId().equals(algOid)) {
                return EcdsaPublicKey.fromSpkiDer(der);
            }
            throw new IllegalArgumentException("Unsupported SPKI algorithm OID: " + algOid);
        } catch (final RuntimeException e) {
            throw new IllegalArgumentException("Invalid SPKI DER", e);
        }
    }

    private static byte[] randomSeed32() {
        final SecureRandom rnd = new SecureRandom();
        final byte[] seed = new byte[32];
        rnd.nextBytes(seed);
        return seed;
    }
}
