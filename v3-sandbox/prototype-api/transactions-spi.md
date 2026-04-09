# Transactions SPI API

This section defines the SPI API for transactions.

## Description

Since the consensus node is designed in a service-oriented manner, transactions are handled by a separate service.
The consensus node supports adding custom services next to the services that are part of the consensus node repository.
Since new and custom services can provide new transaction types, the SDKs must be able to handle these new transaction types.
The transactions SPI API defines the interface that must be implemented by the custom service that provides new transaction types.

In the `TransactionBuilder`/`Transaction` model, the domain-specific type is the builder (e.g. `AccountCreateTransactionBuilder`).
`Transaction` itself is universal and has no domain-specific subclasses. Therefore, the SPI converts between protobuf types and the concrete `TransactionBuilder` subclass.

## API Schema

```
namespace transactions-spi
requires transactions, grpc, hiero-proto

// TransactionSupport is the interface that must be implemented per custom transaction type.
// $$Record is intentionally absent as a generic parameter: records are always the universal
// Record<$$Receipt> type with no named subtypes. The convert method below returns Record<$$Receipt>
// directly — see the "Design Rationale" section of transactions.md for the reasoning.
abstraction TransactionSupport<$$TransactionBuilder, $$Response, $$Receipt> {

    type getTransactionType() // defines the transaction builder type ($$TransactionBuilder) the concrete TransactionSupport implementation supports

    grpc.MethodDescriptor getMethodDescriptor() // defines the gRPC method

    hiero-proto.TransactionBody updateBody(transactionBuilder:$$TransactionBuilder, protoBody:hiero-proto.TransactionBody) // updates a proto TransactionBody with the builder's domain fields

    $$TransactionBuilder convert(protoBody:hiero-proto.TransactionBody) // converts a proto TransactionBody to a TransactionBuilder

    $$Response convert(protoResponse:hiero-proto.TransactionResponse) // converts a proto TransactionResponse to a Response

    $$Receipt convert(protoReceipt:hiero-proto.TransactionReceipt) // converts a proto TransactionReceipt to a Receipt

    transactions.Record<$$Receipt> convert(protoRecord:hiero-proto.TransactionRecord) // converts a proto TransactionRecord to a Record
}

// factory methods that need to be implemented

@@throws(not-found-error) TransactionSupport getTransactionSupport(transactionType:type) // returns the TransactionSupport for the given transaction type
set<TransactionSupport> getAllTransactionSupports() // returns all TransactionSupport instances
```

## Questions & Comments

- [@hendrikebbers](https://github.com/hendrikebbers): Do we have classes like `TransactionBody.Builder` for every language in the generated proto files?
- [@hendrikebbers](https://github.com/hendrikebbers): Is `MethodDescriptor` specific for Java or is it the same for all languages?
- [@hendrikebbers](https://github.com/hendrikebbers): Can we provide all information needed for `MethodDescriptor` in a custom complex type and by doing so remove the dependency to `grpc` in the public API?
