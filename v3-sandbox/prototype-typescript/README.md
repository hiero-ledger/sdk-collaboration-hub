# Hiero V3 SDK — TypeScript PoC

Proof-of-concept implementation of the V3 API defined in [`v3-sandbox/prototype-api/`](../prototype-api/).

## Namespaces

| Namespace | Spec file | Status |
|---|---|---|
| `keys` | [`keys.md`](../prototype-api/keys.md) | Implemented |
| `common` | [`common.md`](../prototype-api/common.md) | Implemented |
| `config` | [`config.md`](../prototype-api/config.md) | Implemented |
| `client` | [`client.md`](../prototype-api/client.md) | Implemented |

## Key design decisions

- `@@immutable` fields → `readonly`
- `@@nullable T` → `T | null`
- `@@throws` → runtime `Error` with prefix matching the error type (`illegal-format:`, `not-found-error:`)
- `abstraction` → `abstract class`
- Keys use Node.js built-in `crypto` (no external deps)
- `HbarUnit` tinybars values use `bigint` (values exceed 2^53)

## Build and test

```bash
npm install
npm test
npm run build
```

Requires Node.js 18+.
