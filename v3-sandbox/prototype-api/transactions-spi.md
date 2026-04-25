# Transactions SPI API

This section defines the SPI API for transactions.

## Description

Since the consensus node is designed in a service-oriented manner, transactions are handled by a separate service.
The consensus node supports adding custom services next to the services that are part of the consensus node repository.
Since new and custom services can provide new transaction types, the SDKs must be able to handle these new transaction types.
The transactions SPI API defines the interface that must be implemented by the custom service that provides new transaction types.

In the `TransactionBuilder`/`Transaction` model, the domain-specific type is the builder (e.g. `AccountCreateTransactionBuilder`).
`Transaction` itself is universal and has no domain-specific subclasses. Therefore, the SPI converts between protobuf types and the concrete `TransactionBuilder` subclass.

### Relationship to GrpcTransport and Execution SPI Methods

`TransactionSupport` and the SPI methods on `GrpcTransport` and the execution SPI (from [requests-spi.md](requests-spi.md)) are complementary:

- **`TransactionSupport`** is the **data-layer SPI** — it handles proto serialization, deserialization, and gRPC method selection for a specific transaction type.
- **`GrpcTransport` SPI methods** (`buildRequest`, `isRetryable`) handle building the protobuf request and determining retryability.
- **Execution SPI methods** (`send`, `mapResponse`) handle sending the request and mapping the response.

`Transaction` implements the `GrpcTransport` and execution SPI methods generically by delegating to the `TransactionSupport` instance that matches its concrete builder type:

| SPI method | Source | Transaction implementation |
|---|---|---|
| `buildRequest(node)` | `GrpcTransport` | Uses `TransactionSupport.updateBody()` to build the `TransactionBody` from the stored builder state, sets `transactionId` and `nodeAccountId`, signs with collected signers, wraps in `SignedTransaction` |
| `send(node, request)` | execution SPI | Uses `TransactionSupport.getMethodDescriptor()` to select the gRPC method, invokes it on the consensus node |
| `mapResponse(response)` | execution SPI | Uses `TransactionSupport.convert(protoResponse)` to produce the SDK `Response<$$Receipt>` |
| `isRetryable(error)` | `GrpcTransport` | Checks gRPC status codes (UNAVAILABLE, RESOURCE_EXHAUSTED) plus consensus-specific codes (BUSY, PLATFORM_TRANSACTION_NOT_CREATED, etc.) |

This means adding a new transaction type requires only:
1. A new `TransactionBuilder` subclass (e.g. `FooTransactionBuilder extends TransactionBuilder<FooTransactionBuilder, Response<FooReceipt>>`)
2. A new `TransactionSupport` implementation for that builder type

No additional subclasses are needed for SPI purposes.

## API Schema

```
namespace transactionsSpi
requires transactions, grpc, hieroProto

// TransactionSupport is the interface that must be implemented per custom transaction type.
// It provides the data-layer mechanics that Transaction delegates to when implementing the execution SPI.
//
// $$Record is intentionally absent as a generic parameter: records are always the universal
// Record<$$Receipt> type with no named subtypes. The convert method below returns Record<$$Receipt>
// directly — see the "Design Rationale" section of transactions.md for the reasoning.
abstraction TransactionSupport<$$TransactionBuilder, $$Response, $$Receipt> {

    type getTransactionType() // defines the transaction builder type ($$TransactionBuilder) the concrete TransactionSupport implementation supports

    grpc.MethodDescriptor getMethodDescriptor() // defines the gRPC method to call for this transaction type

    hieroProto.TransactionBody updateBody(transactionBuilder:$$TransactionBuilder, protoBody:hieroProto.TransactionBody) // updates a proto TransactionBody with the builder's domain fields

    $$TransactionBuilder convert(protoBody:hieroProto.TransactionBody) // converts a proto TransactionBody to a TransactionBuilder

    $$Response convert(protoResponse:hieroProto.TransactionResponse) // converts a proto TransactionResponse to a Response

    $$Receipt convert(protoReceipt:hieroProto.TransactionReceipt) // converts a proto TransactionReceipt to a Receipt

    transactions.Record<$$Receipt> convert(protoRecord:hieroProto.TransactionRecord) // converts a proto TransactionRecord to a Record
}

// Factory methods that need to be implemented

@@throws(not-found-error) TransactionSupport getTransactionSupport(transactionType:type) // returns the TransactionSupport for the given transaction type
set<TransactionSupport> getAllTransactionSupports() // returns all TransactionSupport instances
```

## Questions & Comments

- [@hendrikebbers](https://github.com/hendrikebbers): Do we have classes like `TransactionBody.Builder` for every language in the generated proto files?
- [@hendrikebbers](https://github.com/hendrikebbers): Is `MethodDescriptor` specific for Java or is it the same for all languages?
- [@hendrikebbers](https://github.com/hendrikebbers): Can we provide all information needed for `MethodDescriptor` in a custom complex type and by doing so remove the dependency to `grpc` in the public API?
