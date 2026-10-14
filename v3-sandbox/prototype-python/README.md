# Hiero SDK V3 - Python PoC

Proof-of-concept implementation of the `keys` namespace from
[`v3-sandbox/prototype-api/keys.md`](../prototype-api/keys.md) in Python.

## Spec coverage

| Namespace | File | Status |
|---|---|---|
| `keys` | `prototype-api/keys.md` | Complete |
| `keys.io` | `prototype-api/keys.md` | Complete |

## Requirements

- Python 3.10+
- [cryptography](https://cryptography.io/) >= 42.0.0

## Setup

```bash
pip install -e ".[dev]"
```

## Running tests

```bash
pytest
```

## Structure

```
src/hiero_sdk_v3/keys/
  key_algorithm.py   # KeyAlgorithm enum (ED25519, ECDSA)
  key_type.py        # KeyType enum (PUBLIC, PRIVATE)
  key.py             # Key abstract base class
  private_key.py     # PrivateKey ABC + factory methods
  public_key.py      # PublicKey ABC + factory methods
  key_pair.py        # KeyPair frozen dataclass
  io/                # keys.io namespace (KeyFormat, KeyEncoding, etc.)
  _impl/             # Concrete implementations (not public API)
```

## Type mapping

| Meta-language | Python |
|---|---|
| `abstraction` | `ABC` |
| `@@immutable` type | `@dataclass(frozen=True)` for value types; `@property` without setter for others |
| `@@throws(illegal-format)` | `raise ValueError` |
| `enum` | `enum.Enum` |
