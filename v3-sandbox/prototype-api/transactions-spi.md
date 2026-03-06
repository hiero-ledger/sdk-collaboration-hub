# Transactions SPI API

This section defines the SPI API for transactions.

## Description

Since the consensus node is designed in a service-oriented manner, transactions are handled by a separate service.
The consensus node supports adding custom services next to the services that are part of the consensus node repository.
Since new and custom services can provide new transaction types, the SDKs must be able to handle these new transaction types.
The transactions SPI API defines the interface that must be implemented by the custom service that provides new transaction types.

### Relationship to Executable and GrpcRequest SPI Methods

`TransactionSupport` and the SPI methods on `Executable` / `GrpcRequest` (from [requests-spi.md](requests-spi.md)) are complementary:

- **`TransactionSupport`** is the **data-layer SPI** — it handles proto serialization, deserialization, and gRPC method selection for a specific transaction type.
- **`GrpcRequest` SPI methods** (`buildRequest`, `shouldRetry`) handle building the protobuf request and determining retryability.
- **`Executable` SPI methods** (`send`, `mapResponse`) handle sending the request and mapping the response.

`Transaction` implements the `GrpcRequest` and `Executable` SPI methods generically by delegating to the `TransactionSupport` instance that matches its concrete data type:

| SPI method | Source | Transaction implementation |
|---|---|---|
| `buildRequest(node)` | `GrpcRequest` | Uses `TransactionSupport.updateBody()` to build the `TransactionBody`, sets `transactionId` and `nodeAccountId`, signs with collected signers, wraps in `SignedTransaction` |
| `send(node, request)` | `Executable` | Uses `TransactionSupport.getMethodDescriptor()` to select the gRPC method, invokes it on the consensus node |
| `mapResponse(response)` | `Executable` | Uses `TransactionSupport.convert(protoResponse)` to produce the SDK `TransactionResponse` |
| `shouldRetry(error)` | `GrpcRequest` | Checks gRPC status codes (UNAVAILABLE, RESOURCE_EXHAUSTED) plus consensus-specific codes (BUSY, PLATFORM_TRANSACTION_NOT_CREATED, etc.) |

This means adding a new transaction type requires only:
1. A new `Transaction` subclass (e.g. `FooTransaction extends Transaction`)
2. A new `TransactionSupport` implementation for the `FooTransactionData` type

No additional subclasses are needed for SPI purposes.

## API Schema

```
namespace transactions-spi
requires transactions, grpc, hiero-proto

// TransactionSupport is the interface that must be implemented per custom transaction type.
// It provides the data-layer mechanics that Transaction delegates to
// when implementing ExecutableSpi.
abstraction TransactionSupport<$$Data, $$Response, $$Receipt, $$Record> {

    type getTransactionType() // defines the transaction data type ($$Data) this support handles

    grpc.MethodDescriptor getMethodDescriptor() // defines the gRPC method to call for this transaction type

    hiero-proto.TransactionBody updateBody(data:$$Data, protoBody:hiero-proto.TransactionBody) // populates a proto TransactionBody from the transaction data

    $$Data convert(protoBody:hiero-proto.TransactionBody) // converts a proto TransactionBody back to transaction data

    $$Response convert(protoResponse:hiero-proto.TransactionResponse) // converts a proto TransactionResponse to an SDK Response

    $$Receipt convert(protoReceipt:hiero-proto.TransactionReceipt) // converts a proto TransactionReceipt to an SDK Receipt

    $$Record convert(protoRecord:hiero-proto.TransactionRecord) // converts a proto TransactionRecord to an SDK Record
}

// Factory methods that need to be implemented

@@throws(not-found-error) TransactionSupport getTransactionSupport(transactionType:type) // returns the TransactionSupport for the given transaction type
set<TransactionSupport> getAllTransactionSupports() // returns all TransactionSupport instances
```

## Questions & Comments

- [@hendrikebbers](https://github.com/hendrikebbers): Do we have classes like `TransactionBody.Builder` for every language in the generated proto files?
- [@hendrikebbers](https://github.com/hendrikebbers): Is `MethodDescriptor` specific for Java or is it the same for all languages?
- [@hendrikebbers](https://github.com/hendrikebbers): Can we provide all information needed for `MethodDescriptor` in a custom complex type and by doing so remove the dependency to `grpc` in the public API?
