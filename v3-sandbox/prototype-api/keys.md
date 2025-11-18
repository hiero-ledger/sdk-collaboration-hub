# Keys API

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
    RAW //Hex encoded string (We need to make clear that this must not be algorithm-specific)
    DER //Distinguished Encoding Rules (X.690) ASN.1 format
}

// abstract key definition
abstraction Key {
    @@immutable bytes: bytes //the raw bytes of the key
    @@immutable algorithm: KeyAlgorithm //the algorithm of the key
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
PrivateKey createPrivateKey(algorithm: KeyAlgorithm, bytes: bytes)
PrivateKey createPrivateKey(algorithm: KeyAlgorithm, encoding: KeyEncoding, bytes: string)
PublicKey createPublicKey(algorithm: KeyAlgorithm, bytes: bytes)
PublicKey createPublicKey(algorithm: KeyAlgorithm, encoding: KeyEncoding, bytes: string)
```

### Comments

- [@rwalworth](https://github.com/rwalworth): What do you think about only exposing a `KeyPair` API and making `PublicKey` and `PrivateKey` internal?
This would consolidate all the key processing to one object, users wouldn't have to keep track of two different objects.
`KeyPair` objects could still be initialized from `PrivateKey` or `PublicKey` bytes/strings and all the same functionality would be kept but just in one object instead of two.
Thoughts?


We discussed the topic in the SDK Community call. Currently the suggestion is to provide 2 different ways to create a Key:

#### Flexible method
```
Key create(String input, KeyAlgorithm algorithm, KeyEncoding encoding)
```
The method provides the most flexible way to create a key by specifying the algorithm and encoding that must be used to interpret the input. If we want to support multiple algorithms and encodings this is the only way that is 100% correct from a technical perspective and supports all edge cases.

#### Convenience method
```
Key create(String input)
```
Since the usage of the method is complex (user needs to know about algorithms and encodings) we need to provide a more simple way to create a key. The given method only needs an input string to create a key. Since we can not extract any algorithm and encoding magically based on the string we must define some constraints here. In the community call we agreed that the most common used format is an ECDSA with HEX encoding. That is exactly the format of the basic private key that people will see in the [Hedera portal](https://portal.hedera.com). A check to support input strings that start with or without 0x can easily be added to the method. Today we have the 2 encoding types RAW and DER. Looks like RAW represents exactly the basic HEX encoded input we want to support with this method.