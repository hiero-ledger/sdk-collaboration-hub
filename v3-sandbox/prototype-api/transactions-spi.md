# Transactions SPI API

This section defines the SPI API for transactions.

## Description

Since the consensus node is designed in a service-oriented manner, transactions are handled by a separate service.
The consensus node supports adding custom services next to the services that are part of the consensus node repository.
Since new and custom services can provide new transaction types, the SDKs must be able to handle these new transaction types.
The transactions SPI API defines the interface that must be implemented by the custom service that provides new transaction types.

### Relationship to ExecutableSpi

`TransactionSupport` and `ExecutableSpi` (from [requests-spi.md](requests-spi.md)) are complementary:

- **`TransactionSupport`** is the **data-layer SPI** — it handles proto serialization, deserialization, and gRPC method selection for a specific transaction type.
- **`ExecutableSpi`** is the **transport-layer SPI** — it handles building the wire request, sending it, mapping the response, and determining retryability.

`PackedTransaction<$$Data>` implements `ExecutableSpi` generically by delegating to the `TransactionSupport` instance that matches its `$$Data` type:

| ExecutableSpi method | PackedTransaction implementation |
|---|---|
| `buildRequest(node)` | Uses `TransactionSupport.updateBody()` to build the `TransactionBody`, sets `transactionId` and `nodeAccountId`, signs with collected signers, wraps in `SignedTransaction` |
| `send(node, request)` | Uses `TransactionSupport.getMethodDescriptor()` to select the gRPC method, invokes it on the consensus node |
| `mapResponse(response)` | Uses `TransactionSupport.convert(protoResponse)` to produce the SDK `TransactionResponse` |
| `shouldRetry(error)` | Checks for retryable consensus error codes (BUSY, PLATFORM_TRANSACTION_NOT_CREATED, etc.) |

This means adding a new transaction type requires only:
1. A new `Transaction` builder subclass (e.g. `FooTransaction extends Transaction`)
2. A new `TransactionSupport` implementation for the `FooTransactionData` type
No new `PackedTransaction` subclass is needed.

## API Schema

```
namespace transactions-spi
requires transactions, grpc, hiero-proto

// TransactionSupport is the interface that must be implemented per custom transaction type.
// It provides the data-layer mechanics that PackedTransaction delegates to
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
