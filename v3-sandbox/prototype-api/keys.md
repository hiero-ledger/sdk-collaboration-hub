# Keys API

This section defines the API for keys.

## Description

The keys API provides functionality to create and manage cryptographic keys.
A cryptographic key is defined by a byte sequence and a cryptographic algorithm.
Here it is independent if the key is a public or private key.
A private key can be used to sign messages and a public key can be used to verify signatures.
A private key is normally generated as a random key for a specific algorithm.
A public key can be derived from a private key.

To read or write keys different formats are supported.
The most easy way is to read or write the raw bytes of the key.
Todo so the algorithm must be known.

To make import and export of keys more convenient, so called key containers exist.
Like algorithms those containers are well defined and standardized.
A container normally contains the raw bytes of the key and the algorithm.
To import and export a key in a container an encoding must be specified.
Here not all container formats support all encodings by its spec and the encoding can end in a byte array result or a
string result.

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
    
    // returns true if the container format supports the given key type
    boolean supportsType(KeyType type) 
    
    // returns true if the container format supports the given encoding
    // PKCS8 and SPKI support DER and PEM encodings
    boolean supportsEncoding(KeyEncoding encoding) 
                                                   
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
    
    // Constructor with validation
    // Throws invalid-format if container doesn't support the specified encoding
    @@throws(invalid-format) new EncodedKeyContainer(
        container: KeyContainer,
        encoding: KeyEncoding,
        format: RawFormat
    )
    
    // returns true if the internal container format supports the given key type
    bool supportsType(type: KeyType) 
}

// abstract key definition
abstraction Key {
    @@immutable bytes: bytes //the raw bytes of the key
    @@immutable algorithm: KeyAlgorithm //the algorithm of the key
    @@immutable type: KeyType //the type of the key
    
    // Get raw bytes of the key
    bytes toRawBytes() // returns the key in the RAW encoding
    
    // Convert to bytes using specified container format
    // Throws illegal-format if container.format is not BYTES or doesn't support this key type
    @@throws(illegal-format) bytes toBytes(container: EncodedKeyContainer) 
    
    // Convert to string using specified container format
    // Throws illegal-format if container.format is not STRING or doesn't support this key type
    @@throws(illegal-format) string toString(container: EncodedKeyContainer) 
}

// a key pair
KeyPair {
    @@immutable publicKey: PublicKey // the public key of the key pair
    @@immutable privateKey: PrivateKey // the private key of the key pair
}

// public key definition
PublicKey extends Key {
    
    // Verify a signature using this public key
    // returns true if the signature is valid for the message and the public key
    bool verify(message: bytes, signature: bytes)
}

// private key definition
PrivateKey extends Key {
    
    // Sign a message with this private key
    // returns the signature for the message
    bytes sign(message: bytes) 
    
    // Derive the corresponding public key
    // always returns a new PublicKey instance
    PublicKey createPublicKey() 
}

// factory methods of keys that should be added to the namespace in the best language dependent way

PrivateKey generatePrivateKey(algorithm: KeyAlgorithm)
PublicKey generatePublicKey(algorithm: KeyAlgorithm)

// Factory methods for key creation from raw bytes
@@throws(illegal-format) PrivateKey createPrivateKey(algorithm: KeyAlgorithm, rawBytes: bytes)
@@throws(illegal-format) PublicKey createPublicKey(algorithm: KeyAlgorithm, rawBytes: bytes)

// Factory methods for key creation from encoded strings
@@throws(illegal-format) PrivateKey createPrivateKey(algorithm: KeyAlgorithm, encoding: ByteImportEncoding, value: string)
@@throws(illegal-format) PublicKey createPublicKey(algorithm: KeyAlgorithm, encoding: ByteImportEncoding, value: string)

// Factory methods for key creation from encoded containers
@@throws(illegal-format) PrivateKey createPrivateKey(container: EncodedKeyContainer, value: string) // if container.format is not STRING an illegal format error is thrown
@@throws(illegal-format) PublicKey createPublicKey(container: EncodedKeyContainer, value: string) // if container.format is not STRING an illegal format error is thrown

@@throws(illegal-format) PrivateKey createPrivateKey(container: EncodedKeyContainer, value: bytes) // if container.format is not BYTES an illegal format error is thrown
@@throws(illegal-format) PublicKey createPublicKey(container: EncodedKeyContainer, value: bytes) // if container.format is not BYTES an illegal format error is thrown

// Convenience methods for PEM format
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

The topic has been discussed in the SDK Community
call (https://zoom.us/rec/share/oDRfe45YHrQy71lU0RvWs3dnERq2b4KeTRW10emcTXkEb-9gQJUfLa6Lzngm8TRI.ndb4Z4pBanr4DKr0 / https://zoom-lfx.platform.linuxfoundation.org/meeting/94709702244-1763391600000/summaries?password=bf9431fc-3a4d-4e1d-a81a-e44ef16d8abc).
The current result is that we can not support all possible key formats with a single method that has only a string as
input.
Having a more configurable method based on enums must be the way to go.
We still believe that we should provide a convenience method that can be used to create a key based on a string.
Here we need to define exactly what input is allowed here.
In the meeting different encodings / algorithms / formats were discussed.
Here it is still not clear what the final format will be.

[@rwalworth](https://github.com/rwalworth) did a presentation on key handling in our SDKs in a community call:
https://zoom.us/rec/play/U0G1BHuOxUng4sDMDIJbSaDyNzlUnMn94EKmqoP8J4YDJNaVnnqTFFX8w-NdDuGvP6IMvAOsb9ACH4cd.xv_0-I8kvoSYx3nY?eagerLoadZvaPages=sidemenu.billing.plan_management&accessLevel=meeting&canPlayFromShare=true&from=share_recording_detail&continueMode=true&componentName=rec-play&originRequestUrl=https%3A%2F%2Fzoom.us%2Frec%2Fshare%2FJyGOh5v4BUuxU3cKyp-fcwJ33m7djPDKlA3Jv6AXIFsL7T8uzsmPtXN3AvS7IBeJ.2XaTBZXvMgCQP4Ec
Slides can be found
here: https://docs.google.com/presentation/d/1ID2__-pkBc6mmE_kFoL1hwKugdzJdnrGq5nMB_QPFos/edit?usp=sharing
