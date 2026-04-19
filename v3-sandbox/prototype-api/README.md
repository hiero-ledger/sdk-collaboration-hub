# Prototype API

This file defines the API for the prototype in the format defined at our [api-guideline](../../guides/api-guideline.md).

## Architecture

- [Requests Overview](requests.md) — Request hierarchy, design rationale, hierarchy diagrams
- [Requests Core Types](requests-core.md) — `RequestConfig` struct, execution contracts (`Executable`, `Subscribable`), transport contracts (`GrpcRequest`, `RestRequest`), `Request` root base, network-specific request bases
- [Requests SPI](requests-spi.md) — Internal node/network types, `withRetry` execution loop, SPI methods distributed across 3 axes

## Consensus Node

- [Transactions](transactions.md) — `TransactionBuilder` (mutable build phase) + `Transaction` (immutable sign/send phase), response types
- [Transactions SPI](transactions-spi.md) — `TransactionSupport` data-layer SPI
- [Account Transactions](transactions-accounts.md) — `AccountCreateTransactionBuilder` concrete example
- [Consensus Queries](consensus-queries.md) — `ConsensusQuery` base + concrete consensus query types

## Mirror Node

- [Mirror Requests](mirror-requests.md) — Mirror gRPC/REST queries + `TopicMessageQuery` streaming

## Block Node

- [Block Requests](block-requests.md) — Block node queries + `BlockStreamQuery` streaming

## Shared Dependencies

- [Common API](common.md) — Shared types (`Hbar`, `AccountId`, `Ledger`, etc.)
- [Configuration API](config.md) — Network configuration
- [Key API](keys.md) — Cryptographic key creation and serialization
- [Client API](client.md) — `HieroClient` and `OperatorAccount`

Every SDK will depend in its public API (only SPI part) on [our protobuf definitions](hiero-proto.md)
and [GRPC](grpc.md).
