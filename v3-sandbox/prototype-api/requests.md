# Requests API — Architecture Overview

This document describes the overall request hierarchy for v3 SDKs, covering all request types across consensus nodes, mirror nodes, and block nodes. For concrete type definitions, see the individual files linked below.

## Description

The requests API defines the foundational types for all SDK requests: transactions, queries, and streaming subscriptions. Every concrete request type answers two questions via its type declaration:

1. **Network + execution pattern** (class inheritance): Which network does this target, and does it return a single response or a stream? (`ConsensusCall`, `MirrorCall`, `MirrorStream`, `BlockNodeCall`, `BlockNodeStream`)
2. **Transport** (interface): What transport protocol does it use? (`GrpcTransport` or `RestTransport`)

This replaces the v2 `Executable` base class with a design that cleanly separates:

- **Shared configuration** (`RetryPolicy` struct, `Request` base) from **execution behavior** (`execute()` / `subscribe()` on the concrete bases)
- **Network + execution pattern** (concrete bases in the class hierarchy) from **transport protocol** (`GrpcTransport`, `RestTransport` interfaces)
- **Request semantics** (transaction signing, query payment, etc.) from all of the above

Network and execution pattern are unified in a single class hierarchy layer rather than split across class inheritance and interfaces. This keeps `execute()` and `subscribe()` as sealed concrete implementations — no concrete type needs to re-implement or can accidentally override execution logic.

### RetryPolicy and Priority Chain

Retry and timeout configuration is defined in a standalone `RetryPolicy` struct (see [requests-core.md](requests-core.md)). It is composed by:

- **`Request`** — every request type inherits config through the class hierarchy
- **`HieroClient`** — holds `defaultRetryPolicy` for client-level defaults

When `execute()` or `subscribe()` is called, unset fields on the request are filled from the client's `defaultRetryPolicy`, then from the hardcoded `@@default` values on `RetryPolicy`.

**Priority chain:** per-request config > client defaults > hardcoded defaults.

### Keywords

This document uses two meta-language keywords for abstract types (see [API Guideline](../../guides/api-guideline.md)):

- **`interface`** — Only method signatures. Implement as interfaces (Java/Go/TS), protocols (Swift), traits (Rust), or ABCs with no concrete methods (Python).
- **`abstraction`** — Shared fields and implementation. Implement as abstract classes (Java/C++/Python/JS/TS/Swift) or embedded structs + generics (Go/Rust).

## Related Files

| File | Contents |
|---|---|
| [Requests Core Types](requests-core.md) | `RetryPolicy` struct, transport interfaces (`GrpcTransport`, `RestTransport`), root `Request` base, concrete bases (`ConsensusCall`, `MirrorCall`, `MirrorStream`, `BlockNodeCall`, `BlockNodeStream`) |
| [Requests SPI](requests-spi.md) | Internal node/network types, `withRetry` loop, `executeImpl`/`subscribeImpl` helpers, SPI methods across the hierarchy |
| [Transactions](transactions.md) | `TransactionBuilder` (mutable build phase), `Transaction` (immutable sign/send phase), response types (`TransactionId`, `Receipt`, `Record`) |
| [Transactions SPI](transactions-spi.md) | `TransactionSupport` data-layer SPI |
| [Account Transactions](transactions-accounts.md) | `AccountCreateTransactionBuilder` — concrete transaction example |
| [Consensus Queries](consensus-queries.md) | `ConsensusQuery` base + concrete consensus query types |
| [Mirror Requests](mirror-requests.md) | Mirror gRPC/REST queries + `TopicMessageQuery` streaming |
| [Block Requests](block-requests.md) | Block node queries + `BlockStreamQuery` streaming |

## Hierarchy Diagram

`──>` = class inheritance (single chain to `Request`). `··>` = implements interface.

### Interfaces (no state, no inheritance relationship to Request)

```
GrpcTransport                     RestTransport
  ··> Transaction                   ··> MirrorNodeContractCallQuery
  ··> ConsensusQuery                ··> MirrorNodeContractEstimateGasQuery
  ··> AddressBookQuery
  ··> TopicMessageQuery
  ··> ServerStatusQuery
  ··> BlockQuery
  ··> BlockStreamQuery
```

### Class Inheritance Tree (single-inheritance, rooted at Request)

```
RetryPolicy                                                [standalone struct]

Request [composes RetryPolicy]
├── ConsensusCall<$$R>
│   ├── Transaction : GrpcTransport                       [+ sign(), toBytes(), execute()]
│   │   ├── ChunkedTransaction                            [+ chunking logic]
│   │   │   ├── FileAppendTransaction
│   │   │   └── TopicMessageSubmitTransaction
│   │   └── ... (50+ more concrete types via TransactionBuilder)
│   └── ConsensusQuery<$$R> : GrpcTransport               [+ nodeAccountIds, queryPayment, getCost()]
│       ├── AccountInfoQuery
│       ├── AccountBalanceQuery
│       ├── TransactionReceiptQuery
│       └── ... (17+ more)
│
├── MirrorCall<$$R>
│   ├── AddressBookQuery : GrpcTransport
│   ├── MirrorNodeContractCallQuery : RestTransport
│   └── MirrorNodeContractEstimateGasQuery : RestTransport
│
├── MirrorStream<$$I>
│   └── TopicMessageQuery : GrpcTransport
│
├── BlockNodeCall<$$R>
│   ├── ServerStatusQuery : GrpcTransport
│   └── BlockQuery : GrpcTransport
│
└── BlockNodeStream<$$I>
    └── BlockStreamQuery : GrpcTransport
```

## Summary Table

| Request | Base (network + execution) | Transport |
|---|---|---|
| `Transaction` | `ConsensusCall` | `GrpcTransport` |
| `ConsensusQuery` | `ConsensusCall` | `GrpcTransport` |
| `AddressBookQuery` | `MirrorCall` | `GrpcTransport` |
| `MirrorNodeContractCallQuery` | `MirrorCall` | `RestTransport` |
| `MirrorNodeContractEstimateGasQuery` | `MirrorCall` | `RestTransport` |
| `TopicMessageQuery` | `MirrorStream` | `GrpcTransport` |
| `ServerStatusQuery` | `BlockNodeCall` | `GrpcTransport` |
| `BlockQuery` | `BlockNodeCall` | `GrpcTransport` |
| `BlockStreamQuery` | `BlockNodeStream` | `GrpcTransport` |

## Usage Example

```
// Build a transaction (mutable phase)
AccountCreateTransactionBuilder builder = new AccountCreateTransactionBuilder()
builder.setKey(newAccountKey)
builder.setInitialBalance(Hbar.from(10))

// Build and execute in one call (auto-signs with operator)
Response<AccountCreateReceipt> response = builder.buildAndExecute(client)

// Get the receipt
AccountCreateReceipt receipt = response.queryReceipt()
```

## Questions & Comments

- Should `RetryPolicy` default values for `maxAttempts`, `maxBackoff`, `minBackoff` be standardized across all SDKs, or configurable per-SDK?
