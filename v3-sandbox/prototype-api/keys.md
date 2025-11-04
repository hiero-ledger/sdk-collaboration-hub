# Keys

This section defines the API for keys.

```
namespace keys

//all supported algorithms
enum KeyAlgorithm {
    ED25519 // Edwards-curve Digital Signature Algorithm 
    ECDSA // Elliptic Curve Digital Signature Algorithm (secp256k1 curve)
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
    @immutable publicKey: PublicKey // the public key of the key pair
    @immutable privateKey: PrivateKey // the private key of the key pair
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
PrivateKey createPrivateKey(algorithm: KeyAlgorithm, bytes: bytes)
PrivateKey createPrivateKey(algorithm: KeyAlgorithm, encoding: KeyEncoding, bytes: string)
PublicKey createPublicKey(algorithm: KeyAlgorithm, bytes: bytes)
PublicKey createPublicKey(algorithm: KeyAlgorithm, encoding: KeyEncoding, bytes: string)
```

### Comments

@rwalworth: What do you think about only exposing a `KeyPair` API and making `PublicKey` and `PrivateKey` internal?
This would consolidate all the key processing to one object, users wouldn't have to keep track of two different objects.
`KeyPair` objects could still be initialized from `PrivateKey` or `PublicKey` bytes/strings and all the same functionality would be kept but just in one object instead of two.
Thoughts?
