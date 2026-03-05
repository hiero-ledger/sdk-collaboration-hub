# SDK V3 Sandbox

This folder contains all documents regarding a possible V3 of the Hiero SDKs.

## Why V3?

The current Hiero SDKs were originally built for Hedera and the Hedera network.
With the move of the Hedera codebase to Hiero, the project has become much broader in scope.
The existing SDKs carry limitations that no longer align with where the project is heading.
A new generation of SDKs is needed to provide the developer experience that Hiero deserves.

## Problems with the Current SDKs

### Scoped to Hedera

The current SDKs were designed specifically for Hedera and the Hedera network.
They do not reflect the broader scope that Hiero now covers.

### Language Idioms Not Fully Embraced

Some of the SDKs are not implemented in the best possible way for their underlying programming language.
This leads to APIs that feel foreign to developers experienced in that language, making SDK usage harder to understand
and causing avoidable friction and issues.

### Outdated Architecture

Over the past years, Hiero has gone through significant changes, including the modularization of the consensus node and
the support for custom services and transactions.
The architecture and public API of the current SDKs have not kept pace with these developments and are outdated at
several points.

### Developer Experience

Since we want to provide a great developer experience for our users, the current SDKs are not suitable for the future.
Developers deserve SDKs that are intuitive, consistent across languages, and aligned with modern API design principles.

## Vision

SDK V3 is an opportunity to design the ideal SDK from the ground up, without the constraints of backward compatibility.
The goal is to define a public API that is:

- **Language-idiomatic** -- each SDK should feel natural in its respective programming language, leveraging
  language-specific features like generics, type inference, and idiomatic error handling.
- **Consistent across languages** -- while the concrete API adapts to each language, the overall structure, concepts,
  and developer workflows should be consistent across all SDKs.
- **Future-proof** -- the architecture should accommodate the evolving nature of Hiero, including custom services,
  modular consensus nodes, and new transaction types, without requiring breaking changes.
- **Accessible and well-documented** -- clear guidelines, comprehensive documentation, and example-driven API design
  should lower the barrier to entry for new developers.
- **Broader than Hedera** -- the SDKs should work with any Hiero-based network, not just Hedera.

### Beyond the Consensus Node

The current SDKs focus almost exclusively on interacting with the consensus node.
V3 aims to go further by also providing client APIs for the
[Hiero Mirror Node](https://github.com/hiero-ledger/sdk-collaboration-hub/pull/142) and the Hiero Block Node.
This raises important questions about modularization -- for example, whether the SDK should be split into separate
modules per node type or remain a single unified package.
These questions need to be discussed and resolved as part of the V3 design process.

### Framework Integration

Projects like [Hiero Enterprise Java](https://github.com/hiero-ledger/hiero-enterprise-java) and
[Hiero Enterprise JS](https://github.com/hiero-hackers/hiero-enterprise-js) demonstrate the need to integrate SDK
functionality deeply into application frameworks.
V3 must consider these use cases from the start, ensuring that the SDK architecture enables seamless integration into
frameworks and higher-level abstractions.

## Approach

### Language-Agnostic API Definition

The V3 API is defined using a [language-agnostic meta-language](../guides/api-guideline.md) that allows us to specify
the public API once and then translate it into concrete implementations for each supported language.
This approach ensures consistency across SDKs while allowing each implementation to follow the idioms and best practices
of its language.

Language-specific best practice guides define how the meta-language concepts map to concrete patterns in each language.

### Prototype-Driven Design

Rather than designing the API purely on paper, we develop prototypes in multiple languages to validate that the API
design works well in practice.
The [prototype-api](prototype-api) folder contains the draft of the V3 public API, and prototype implementations help
us test and refine these designs.

### No Backward Compatibility Constraints

V3 is explicitly designed as an ideal API without regard for backward compatibility with V2.
Migration paths from V2 to V3 will be addressed once concrete MVPs are available and the API design has stabilized.
This allows us to first focus on making the best possible design decisions without being constrained by legacy choices.

## Scope

The V3 effort covers all Hiero SDKs:

- Java
- JavaScript / TypeScript
- Go
- Rust
- Python
- C++
- Swift

## Current State

The V3 work is in the prototyping phase.
No API is fixed yet -- the draft is a work in progress.

## Content of This Folder

### Draft of Public API

The [prototype-api](prototype-api) folder contains the draft of a possible public API for future SDK versions.
The goal of the draft is to define an ideal API for working with a Hiero-based network.
The API is defined using the [API guidelines](../guides/api-guideline.md) meta-language, allowing us to specify the
public API in a language-independent way.

### Java PoC

The [prototype](prototype) folder contains a Java-based PoC that has been used as a base to get an understanding of a
possible V3 SDK.
The PoC has been used as a base for the draft of the public API but has not been updated.

### Java Key API Test Implementation

The [prototype-keys-java](prototype-keys-java) folder contains a test implementation of the new Key API in Java.
The purpose of this implementation is to validate how the API feels in practice and whether the design is feasible to
implement.

## How to Contribute

The SDK V3 effort is coordinated in this repository, the
[SDK Collaboration Hub](https://github.com/hiero-ledger/sdk-collaboration-hub).
Contributions to the API definition, language-specific best practices, and prototype implementations are welcome.