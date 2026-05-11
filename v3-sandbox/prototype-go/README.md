# Hiero V3 SDK — Go PoC

Proof-of-concept implementation of the V3 API defined in [`v3-sandbox/prototype-api/`](../prototype-api/).

## Namespaces

| Namespace | Spec file | Status |
|---|---|---|
| `keys` | [`keys.md`](../prototype-api/keys.md) | Implemented |

## Key design decisions

- `@@immutable` fields → unexported struct fields with getter methods
- `@@throws` errors → `error` second return value (no panics on normal paths)
- `abstraction Key` → embedded `Key` struct (Go has no abstract classes)
- ED25519 uses `golang.org/x/crypto/ed25519` (standard library compatible)
- ECDSA uses `github.com/decred/dcrd/dcrec/secp256k1/v4` (secp256k1 curve)
- PEM export is supported for ED25519; ECDSA keys round-trip via raw compressed point bytes

## Build and test

```bash
go mod tidy
go test ./keys/
```

Requires Go 1.21+.
