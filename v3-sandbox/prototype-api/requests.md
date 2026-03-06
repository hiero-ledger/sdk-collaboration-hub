# Requests API — Architecture Overview

This document describes the overall request hierarchy for v3 SDKs, covering all request types across consensus nodes, mirror nodes, and block nodes. For concrete type definitions, see the individual files linked below.

## Description

The requests API defines the foundational types for all SDK requests: transactions, queries, and streaming subscriptions. It uses a **3-axis model** where every concrete request type answers three orthogonal questions via its type declaration:

1. **Network** (class inheritance): Which network does this target? (`ConsensusRequest`, `MirrorRequest`, `BlockNodeRequest`)
2. **Execution** (interface): What execution pattern does it use? (`Executable<R>` or `Subscribable<I>`)
3. **Transport** (interface): What transport protocol does it use? (`GrpcRequest` or `RestRequest`)

This replaces the v2 `Executable` base class with a design that cleanly separates:

- **Shared configuration** (`RequestConfig` struct, `Request` base) from **execution behavior** (`Executable`, `Subscribable` interfaces)
- **Network targeting** (network-specific request bases) from **transport protocol** (`GrpcRequest`, `RestRequest` interfaces)
- **Request semantics** (transaction signing, query payment, etc.) from all of the above

Diamond inheritance is avoided by keeping all interfaces outside the class inheritance tree. Every type has exactly one class-inheritance path to `Request`.

### RequestConfig and Priority Chain

Retry and timeout configuration is defined in a standalone `RequestConfig` struct (see [requests-core.md](requests-core.md)). It is composed by:

- **`Request`** — every executable request type inherits config through the class hierarchy
- **`HieroClient`** — holds `defaultRequestConfig` for client-level defaults

When `execute()` or `subscribe()` is called, unset fields on the request are filled from the client's `defaultRequestConfig`, then from the hardcoded `@@default` values on `RequestConfig`.

**Priority chain:** per-request config > client defaults > hardcoded defaults.

### Keywords

This document uses two meta-language keywords for abstract types (see [API Guideline](../../guides/api-guideline.md)):

- **`interface`** — Only method signatures. Implement as interfaces (Java/Go/TS), protocols (Swift), traits (Rust), or ABCs with no concrete methods (Python).
- **`abstraction`** — Shared fields and implementation. Implement as abstract classes (Java/C++/Python/JS/TS/Swift) or embedded structs + generics (Go/Rust).

## Related Files

| File | Contents |
|---|---|
| [Requests Core Types](requests-core.md) | `RequestConfig` struct, execution interfaces (`Executable`, `Subscribable`), transport interfaces (`GrpcRequest`, `RestRequest`), root `Request` base, network-specific request bases |
| [Requests SPI](requests-spi.md) | Internal node/network types, `withRetry` execution loop, SPI methods distributed across 3 axes |
| [Transactions](transactions.md) | `Transaction` (single type with internal state machine), response types (`TransactionId`, `Receipt`, `Record`) |
| [Transactions SPI](transactions-spi.md) | `TransactionSupport` data-layer SPI |
| [Account Transactions](transactions-accounts.md) | `AccountCreateTransaction` — concrete transaction example |
| [Consensus Queries](consensus-queries.md) | `ConsensusQuery` base + concrete consensus query types |
| [Mirror Requests](mirror-requests.md) | Mirror gRPC/REST queries + `TopicMessageQuery` streaming |
| [Block Requests](block-requests.md) | Block node queries + `BlockStreamQuery` streaming |

## Hierarchy Diagram

`──>` = class inheritance (single chain to `Request`). `··>` = implements interface.

### Interfaces (no state, no inheritance relationship to Request)

```
Executable<Response>              Subscribable<Item>
  ··> Transaction                   ··> TopicMessageQuery
  ··> ConsensusQuery                ··> BlockStreamQuery
  ··> AddressBookQuery
  ··> MirrorNodeContractCallQuery
  ··> MirrorNodeContractEstimateGasQuery
  ··> ServerStatusQuery
  ··> BlockQuery

GrpcRequest                       RestRequest
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
RequestConfig                                              [standalone struct]

Request [composes RequestConfig]
├── ConsensusRequest                                       [+ nodeAccountIds]
│   ├── Transaction : Executable, GrpcRequest              [+ transactionId, maxTransactionFee, memo, validDuration, sign(), toBytes()]
│   │   ├── ChunkedTransaction                             [+ chunking logic]
│   │   │   ├── FileAppendTransaction
│   │   │   └── TopicMessageSubmitTransaction
│   │   ├── AccountCreateTransaction
│   │   ├── TransferTransaction
│   │   ├── TokenCreateTransaction
│   │   └── ... (50+ more)
│   └── ConsensusQuery<R> : Executable, GrpcRequest        [+ queryPayment, getCost()]
│       ├── AccountInfoQuery
│       ├── AccountBalanceQuery
│       ├── TransactionReceiptQuery
│       └── ... (17+ more)
│
├── MirrorRequest
│   ├── AddressBookQuery : Executable, GrpcRequest
│   ├── MirrorNodeContractCallQuery : Executable, RestRequest
│   ├── MirrorNodeContractEstimateGasQuery : Executable, RestRequest
│   └── TopicMessageQuery : Subscribable, GrpcRequest
│
└── BlockNodeRequest
    ├── ServerStatusQuery : Executable, GrpcRequest
    ├── BlockQuery : Executable, GrpcRequest
    └── BlockStreamQuery : Subscribable, GrpcRequest
```

## 3-Axis Summary Table

| Request | Network | Execution | Transport |
|---|---|---|---|
| `Transaction` | `ConsensusRequest` | `Executable` | `GrpcRequest` |
| `ConsensusQuery` | `ConsensusRequest` | `Executable` | `GrpcRequest` |
| `AddressBookQuery` | `MirrorRequest` | `Executable` | `GrpcRequest` |
| `MirrorNodeContractCallQuery` | `MirrorRequest` | `Executable` | `RestRequest` |
| `MirrorNodeContractEstimateGasQuery` | `MirrorRequest` | `Executable` | `RestRequest` |
| `TopicMessageQuery` | `MirrorRequest` | `Subscribable` | `GrpcRequest` |
| `ServerStatusQuery` | `BlockNodeRequest` | `Executable` | `GrpcRequest` |
| `BlockQuery` | `BlockNodeRequest` | `Executable` | `GrpcRequest` |
| `BlockStreamQuery` | `BlockNodeRequest` | `Subscribable` | `GrpcRequest` |

## Usage Example

```
// Create a transaction, set fields (including retry config inherited from Request)
AccountCreateTransaction tx = new AccountCreateTransaction()
tx.setKey(newAccountKey)
tx.setInitialBalance(Hbar.from(10))
tx.setMaxAttempts(5)

// Execute directly (auto-freezes, auto-signs with operator)
TransactionResponse response = tx.execute(client)

// Get the receipt
TransactionReceipt receipt = response.queryReceipt(client)
```

## Questions & Comments

- Should `RequestConfig` default values for `maxAttempts`, `maxBackoff`, `minBackoff` be standardized across all SDKs, or configurable per-SDK?
