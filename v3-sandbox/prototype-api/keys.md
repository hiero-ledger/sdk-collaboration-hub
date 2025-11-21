# Keys API

This section defines the API for keys.

## Description

The keys API provides functionality to create and manage cryptographic keys.

## API Schema

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
@@throws(illegal-format) PrivateKey createPrivateKey(algorithm: KeyAlgorithm, bytes: bytes)
@@throws(illegal-format) PrivateKey createPrivateKey(algorithm: KeyAlgorithm, encoding: KeyEncoding, bytes: string)
@@throws(illegal-format) PublicKey createPublicKey(algorithm: KeyAlgorithm, bytes: bytes)
@@throws(illegal-format) PublicKey createPublicKey(algorithm: KeyAlgorithm, encoding: KeyEncoding, bytes: string)
```

## Questions & Comments

- [@rwalworth](https://github.com/rwalworth): What do you think about only exposing a `KeyPair` API and making `PublicKey` and `PrivateKey` internal?
This would consolidate all the key processing to one object, users wouldn't have to keep track of two different objects.
`KeyPair` objects could still be initialized from `PrivateKey` or `PublicKey` bytes/strings and all the same functionality would be kept but just in one object instead of two.
Thoughts?

We discussed the topic in the SDK Community call. Currently the suggestion is to provide 2 different ways to create a Key:

### Flexible method
```
@@throws(illegal-format) Key create(String input, KeyAlgorithm algorithm, KeyEncoding encoding)
```
The method provides the most flexible way to create a key by specifying the algorithm and encoding that must be used to interpret the input. If we want to support multiple algorithms and encodings this is the only way that is 100% correct from a technical perspective and supports all edge cases.

### Convenience method
```
@@throws(illegal-format) Key create(String input)
```
Since the usage of the method is complex (user needs to know about algorithms and encodings) we need to provide a more simple way to create a key. The given method only needs an input string to create a key. Since we can not extract any algorithm and encoding magically based on the string we must define some constraints here. In the community call we agreed that the most common used format is an ECDSA with HEX encoding. That is exactly the format of the basic private key that people will see in the [Hedera portal](https://portal.hedera.com). A check to support input strings that start with or without 0x can easily be added to the method. Today we have the 2 encoding types RAW and DER. Looks like RAW represents exactly the basic HEX encoded input we want to support with this method.

#### Additional comments on keys

The topic has been discussed in the SDK Community call (https://zoom.us/rec/share/oDRfe45YHrQy71lU0RvWs3dnERq2b4KeTRW10emcTXkEb-9gQJUfLa6Lzngm8TRI.ndb4Z4pBanr4DKr0 / https://zoom-lfx.platform.linuxfoundation.org/meeting/94709702244-1763391600000/summaries?password=bf9431fc-3a4d-4e1d-a81a-e44ef16d8abc).
The current result is that we can not support all possible key formats with a single method that has only a string as input.
Having a more configurable method based on enums must be the way to go.
We still believe that we should provide a convenience method that can be used to create a key based on a string.
Here we need to define exactly what input is allowed here.
In the meeting different encodings / algorithms / formats were discussed.
Here it is still not clear what the final format will be.