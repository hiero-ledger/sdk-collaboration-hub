# Prototype API

This file defines the API for the prototype in the format defined at our [api-guideline](../../guides/api-guideline.md).

## Keys

This section defines the API for keys.

```
namespace keys

//all supported algorithms
enum KeyAlgorithm {
    ED25519
    ECDSA
}

// all supported encodings
enum KeyEncoding {
    RAW //Raw binary key material (algorithm-specific)
    DER //Distinguished Encoding Rules (X.690) ASN.1 format
}

// abstract key definition
Key {
    @immutable bytes: bytes //the raw bytes of the key
    @immutable algorithm: KeyAlgorithm //the algorithm of the key
}

// a key pair
KeyPair {
    @immutable publicKey: PublicKey
    @immutable privateKey: PrivateKey
}

// public key definition
PublicKey extends Key {
    boolean verify(message: bytes, signature: bytes)
}

// private key definition
PrivateKey extends Key {
    bytes sign(message: bytes)
    PublicKey createPublicKey() // always returns a new PublicKey instance
}

// factory methods of keys that should be added to the namespace in the best language dependent way

PrivateKey generatePrivateKey(algorithm: KeyAlgorithm)
PrivateKey createPrivateKey(algorithm: KeyAlgorithm, bytes: bytes)
PrivateKey createPrivateKey(algorithm: KeyAlgorithm, encoding: KeyEncoding, bytes: string)
PublicKey createPublicKey(algorithm: KeyAlgorithm, bytes: bytes)
PublicKey createPublicKey(algorithm: KeyAlgorithm, encoding: KeyEncoding, bytes: string)
```