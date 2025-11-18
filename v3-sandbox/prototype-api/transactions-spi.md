# Transactions SPI API

This section defines the SPI API for transactions.

## Description

Since the consensus node is designed in a service-oriented manner, transactions are handled by a separate service.
The consensus node supports adding custom services next to the services that are part of the consensus node repository.
Since new and custom services can provide new transaction types, the SDKs must be able to handle these new transaction types.
The transactions SPI API defines the interface that must be implemented by the custom service that provides new transaction types.

## API Schema

```
namespace transactions
requires transactions, grpc, hiero-proto

// TransactionSupport is the interface that must be implemented per custom transaction type
abstraction TransactionSupport {

    string getTransactionName() // defines the transaction
    
    MethodDescriptor getMethodDescriptor() // defines the gRPC method
    
    TransactionBody updateBody(transaction:Transaction, protoBody:TransactionBody) // updates a proto TransactionBody with the transaction details
    
    Transaction convert(protoBody:TransactionBody) // converts a proto TransactionBody to a Transaction
    
    Response convert(protoResponse:TransactionResponse) // converts a proto TransactionResponse to a Response
    
    Receipt convert(protoReceipt:TransactionReceipt) // converts a proto TransactionReceipt to a Receipt
    
    Record convert(protoRecord:TransactionRecord) // converts a proto TransactionResponse to a Response
}
```

## Questions & Comments

- [@hendrikebbers](https://github.com/hendrikebbers): Do we have classes like `TransactionBody.Builder` for every language in the generated proto files?
- [@hendrikebbers](https://github.com/hendrikebbers): Is `MethodDescriptor` specific for Java or is it the same for all languages?