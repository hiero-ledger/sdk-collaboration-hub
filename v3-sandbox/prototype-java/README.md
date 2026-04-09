# Java Prototype of the V3 Public API

This module is a Java prototype that implements the language-agnostic V3 public API
defined in [`v3-sandbox/prototype-api`](../prototype-api/README.md). The goal of this
prototype is to validate that the meta-language definitions translate cleanly into
idiomatic Java following the rules of [api-best-practices-java.md](../../guides/api-best-practices-java.md)
and [api-guideline.md](../../guides/api-guideline.md).

## Scope

The prototype focuses on the **public API surface**. Where the meta-language only
declares the public contract (e.g., for the client, transactions and SPI namespaces),
the implementation in this module is intentionally minimal — just enough to compile,
make the API ergonomically usable in unit tests, and prove that the meta-language is
self-consistent.

The keys namespace re-uses the existing [`prototype-keys-java`](../prototype-keys-java/README.md)
implementations (Ed25519 / ECDSA secp256k1, PKCS#8/SPKI containers, DER/PEM encodings).

## Layout

| Meta-language namespace                       | Java package                       |
|-----------------------------------------------|------------------------------------|
| `common`                                      | `org.hiero.common`                 |
| `config`                                      | `org.hiero.config`                 |
| `keys`                                        | `org.hiero.keys`                   |
| `keys.io`                                     | `org.hiero.keys.io`                |
| `client`                                      | `org.hiero.client`                 |
| `transactions`                                | `org.hiero.transactions`           |
| `transactions-accounts`                       | `org.hiero.transactions.accounts`  |
| `transactions-spi`                            | `org.hiero.transactions.spi`       |
| (`@ThreadSafe` annotation, see best-practice) | `org.hiero.sdk.annotation`         |

Internal implementation classes live in `impl` sub-packages and are intentionally not
re-exported through any package documentation.

## Reports / open questions

A report of all the gaps, ambiguities and items that could not be implemented from
the meta-language alone is in [REPORT.md](REPORT.md).
