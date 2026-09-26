# Hiero SDK V3 TypeScript Keys PoC

This proof of concept implements the `keys` namespace from `v3-sandbox/prototype-api/keys.md`.

## Covered Spec Sections

- `KeyAlgorithm`
- `KeyType`
- `Key`
- `PrivateKey`
- `PublicKey`
- `KeyPair`
- ED25519 and ECDSA secp256k1 key generation
- Sign and verify round trips
- PKCS#8/SPKI PEM import and export round trips

## Build and Test

```bash
npm install
npm test
npm run build
```

## Notes

The PoC uses Node.js `crypto` for key generation, PEM import/export, signing, and verification. The noble ED25519 and secp256k1 packages are declared as dependencies for follow-up work that needs lower-level raw key encoding support.
