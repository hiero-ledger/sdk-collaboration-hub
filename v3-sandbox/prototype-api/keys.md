# Keys API

This section defines the API for keys.

## Description

The keys API provides functionality to create and manage cryptographic keys.

## API Schema

```
namespace keys

// all key types
enum KeyType {
    PUBLIC, // a public key
    PRIVATE // a private key
}

//all supported algorithms
enum KeyAlgorithm {
    ED25519 // Edwards-curve Digital Signature Algorithm 
    ECDSA // Elliptic Curve Digital Signature Algorithm (secp256k1 curve)
}

// all supported encodings that can be used to import/export a container format
enum KeyEncoding {
    DER, // Distinguished Encoding Rules
    PEM // Privacy Enhanced Mail
}

// all supported container formats
enum KeyContainer {
    PKCS8, // PKCS#8 Private Key Specification
    SPKI // Subject Public Key Info
}

enum ByteImportEncoding {
    HEX, // hex string representation of the bytes
    BASE64 // base64 string representation of the bytes
}

enum RawFormate {
    STRING, // string representation of the bytes in the specified encoding
    BYTES // raw bytes
}

enum EncodedKeyContainer {
    PKCS8_WITH_DER,
    SPKI_WITH_DER,
    PKCS8_WITH_PEM,
    SPKI_WITH_PEM
    
    @immutable KeyContainer container // the container format
    @immutable KeyEncoding encoding // the encoding
    @immutable RawFormate format // the raw format of the import / export
    
    boolean supportsType(KeyType type) // returns true if the container format supports the given key type
}

// abstract key definition
abstraction Key {
    @@immutable bytes: bytes //the raw bytes of the key
    @@immutable algorithm: KeyAlgorithm //the algorithm of the key
    @@immutable type: KeyType //the type of the key
    
    @@throws(illegal-format) bytes toBytes(container: EncodedKeyContainer) // if container.format is not BYTES an illegal format error is thrown

    @@throws(illegal-format) string toString(container: EncodedKeyContainer) // if container.format is not STRING an illegal format error is thrown

    bytes toRawBytes() // returns the key in the RAW encoding
}

// a key pair
KeyPair {
    @@immutable publicKey: PublicKey // the public key of the key pair
    @@immutable privateKey: PrivateKey // the private key of the key pair
}

// public key definition
PublicKey extends Key {
    boolean verify(message: bytes, signature: bytes) // returns true if the signature is valid for the message and the public key
}

// private key definition
PrivateKey extends Key {
    bytes sign(message: bytes) // returns the signature for the message
    PublicKey createPublicKey() // always returns a new PublicKey instance
}

// factory methods of keys that should be added to the namespace in the best language dependent way

PrivateKey generatePrivateKey(algorithm: KeyAlgorithm)
PublicKey generatePublicKey(algorithm: KeyAlgorithm)

@@throws(illegal-format) PrivateKey createPrivateKey(algorithm: KeyAlgorithm, encoding: ByteImportEncoding, value: string) // calls createPrivateKey(algorithm: KeyAlgorithm, rawBytes: bytes)
@@throws(illegal-format) PublicKey createPublicKey(algorithm: KeyAlgorithm, encoding: ByteImportEncoding, value: string) // calls createPublicKey(algorithm: KeyAlgorithm, rawBytes: bytes)

@@throws(illegal-format) PrivateKey createPrivateKey(algorithm: KeyAlgorithm, rawBytes: bytes)
@@throws(illegal-format) PublicKey createPublicKey(algorithm: KeyAlgorithm, rawBytes: bytes)

@@throws(illegal-format) PrivateKey createPrivateKey(container: EncodedKeyContainer, value: string) // if container.format is not STRING an illegal format error is thrown
@@throws(illegal-format) PublicKey createPublicKey(container: EncodedKeyContainer, value: string) // if container.format is not STRING an illegal format error is thrown

@@throws(illegal-format) PrivateKey createPrivateKey(container: EncodedKeyContainer, value: bytes) // if container.format is not BYTES an illegal format error is thrown
@@throws(illegal-format) PublicKey createPublicKey(container: EncodedKeyContainer, value: bytes) // if container.format is not BYTES an illegal format error is thrown

@@throws(illegal-format) PrivateKey createPrivateKey(value: string) // reads string as PKCS#8 PEM
@@throws(illegal-format) PublicKey createPublicKey(value: string) // reads string as SPKI PEM
```

## KeyContainer rules

Not all combinations of container and encoding are valid.
For example, a PKCS8 container format can only be used with DER encoding.
Here you can find the complete list of rules as a basic implementation of the enum:

```
enum KeyContainer {
PKCS8, 
SPKI,  
MULTICODEC, 
JWK;        


    /**
     * Returns true if this container format supports the given key type.
     */
    boolean supportsType(KeyType type) {
        switch (this) {
            case PKCS8:
                // PKCS#8 is ONLY for private keys
                return type == KeyType.PRIVATE;

            case SPKI:
                // SPKI is ONLY for public keys
                return type == KeyType.PUBLIC;

            case MULTICODEC:
                // Multicodec (DID-key style) is standardized ONLY for public keys
                return type == KeyType.PUBLIC;

            case JWK:
                // JWK can contain:
                // - public keys only
                // - private keys only
                // - both (if "d" is present)
                return true;

            default:
                return false;
        }
    }


    /**
     * Returns true if this container format supports the given encoding.
     */
    boolean supportsEncoding(KeyEncoding encoding) {
        switch (this) {
            case PKCS8:
            case SPKI:
                // Standard encodings for ASN.1:
                // - DER (binary)
                // - PEM (Base64 with header/footer)
                return encoding == KeyEncoding.DER
                    || encoding == KeyEncoding.PEM;

            case MULTICODEC:
                // Multicodec is self-describing, but requires:
                // - MULTIBASE (Base58btc, Base64url, Hex, etc.)
                // - BASE64 (optional export without multibase-prefix)
                return encoding == KeyEncoding.MULTIBASE
                    || encoding == KeyEncoding.BASE64;

            case JWK:
                // JWK is always JSON text.
                return encoding == KeyEncoding.JSON;

            default:
                return false;
        }
    }
}
```

### Key examples

The following examples show the different key formats as String representations.

#### PKCS#8 + DER (Private Key)

The string is a hex dump of the DER bytes.
```
30 2E 02 01 00 30 05 06 03 2B 65 70 04 22 04 20D3 67 1A 1E 98 BB 22 F0 11 C0 E4 BC F5 12 55 90
E1 5D 8F 21 A7 01 73 09 BB 55 88 52 03 9B C7 5C
```

#### PKCS#8 + PEM (Private Key)

```
-----BEGIN PRIVATE KEY-----
MC4CAQAwBQYDK2VwBCIEIDNnGh6YuyLwEcDkvPUTVZDhXY8hpwFzCb
tViFIDm8dc=
-----END PRIVATE KEY-----
```

#### SPKI + DER (Public Key)

The string is a hex dump of the DER bytes.
```
30 2A 30 05 06 03 2B 65 70 03 21 00
1D 0F 36 11 67 7D 11 EC 59 85 55 9B
0A 7C 22 7A D9 88 CB 0B E6 C8 27 67
E8 96 F6 9E E3
```

#### SPKI + PEM (Public Key)

```
-----BEGIN PUBLIC KEY-----
MCowBQYDK2VwAyEAHQ82EWd9EexZhVWbCnwie
tmIywvmyCdn6Jb2nuM=
-----END PUBLIC KEY-----
```

#### MULTICODEC + MULTIBASE (Public Key)

```
z6Mksjxie2jA44kVrY8Wj63zqPQAk8SjP2v1tdyASz93Z5mG
```

#### MULTICODEC + BASE64 (Public Key)

```
7QESBB0PNhFnfRHsWYVVmwp8Ih62YjLC+bIJ2folv6e4w==
```

#### JWK + JSON (Public Key)

```
{
    "kty": "OKP",
    "crv": "Ed25519",
    "x": "HQ82EWd9EexZhVWbCnwie2mIywvmyCdn6Jb2nuM"
}
```

#### JWK + JSON (Private Key)

```
{
    "kty": "OKP",
    "crv": "Ed25519",
    "d": "M2caHpi7IvARwOS89RNVkOFdjynAXMJu1WIUgObx1w"
}
```

> [!NOTE]  
> Private JWKs MAY omit the "x" (public key).
> SDKs will support it to read a private key from a JWK.
> When exporting a private key to JWK, the SDK MUST include the "x" property.

## Questions & Comments

