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
namespace transactionsSpi
requires transactions, grpc, hieroProto

// TransactionSupport is the interface that must be implemented per custom transaction type.
// $$Record is intentionally absent as a generic parameter: records are always the universal
// Record<$$Receipt> type with no named subtypes. The convert method below returns Record<$$Receipt>
// directly — see the "Design Rationale" section of transactions.md for the reasoning.
abstraction TransactionSupport<$$TransactionBuilder, $$Response, $$Receipt> {

    type getTransactionType() // defines the transaction builder type ($$TransactionBuilder) the concrete TransactionSupport implementation supports

    grpc.MethodDescriptor getMethodDescriptor() // defines the gRPC method

    hieroProto.TransactionBody updateBody(transactionBuilder:$$TransactionBuilder, protoBody:hieroProto.TransactionBody) // updates a proto TransactionBody with the builder's domain fields

    $$TransactionBuilder convert(protoBody:hieroProto.TransactionBody) // converts a proto TransactionBody to a TransactionBuilder

    $$Response convert(protoResponse:hieroProto.TransactionResponse) // converts a proto TransactionResponse to a Response

    $$Receipt convert(protoReceipt:hieroProto.TransactionReceipt) // converts a proto TransactionReceipt to a Receipt

    transactions.Record<$$Receipt> convert(protoRecord:hieroProto.TransactionRecord) // converts a proto TransactionRecord to a Record
}

// factory methods that need to be implemented

@@throws(not-found-error) TransactionSupport getTransactionSupport(transactionType:type) // returns the TransactionSupport for the given transaction type
set<TransactionSupport> getAllTransactionSupports() // returns all TransactionSupport instances
```

## Questions & Comments

- [@hendrikebbers](https://github.com/hendrikebbers): Do we have classes like `TransactionBody.Builder` for every language in the generated proto files?

- [@hendrikebbers](https://github.com/hendrikebbers): Is `MethodDescriptor` specific for Java or is it the same for all languages?

  **Decision:** `grpc.MethodDescriptor` is cross-language. It is defined as a portable value type
  with `serviceName: string` and `methodName: string`. See [grpc.md](grpc.md) for the full rationale.

- [@hendrikebbers](https://github.com/hendrikebbers): Can we provide all information needed for `MethodDescriptor` in a custom complex type and by doing so remove the dependency to `grpc` in the public API?

  **Decision:** Yes — this is exactly what `grpc.MethodDescriptor` does. The public SPI uses only
  this custom type. Language-specific gRPC runtime types (e.g. `io.grpc.MethodDescriptor` in Java)
  are constructed inside the transport layer from these two string fields and are not part of the
  public API.
