# Transactions API

This section defines the API for transactions.

## Description

The transactions API defines the basic building blocks for transactions.
It does not define specific transaction types but everything that is common to all transaction types.

## API Schema

```
namespace transactions
requires common, keys, client

// Defines the status of a transaction. Since we can have custom transaction types based on custom services in the consensus node we can not use an enum here anymore
abstraction TransactionStatus {
  @@immutable code:int32 // the status code that should be unique based on the consensus node. 
}

// Defines the status codes that are currently used by services that are part of the consensus node repository
enum BasicTransactionStatus extends TransactionStatus {
    OK
    INVALID_TRANSACTION
    PAYER_ACCOUNT_NOT_FOUND
    ...          // other status codes should be defined here TO_BE_DEFINED_IN_FUTURE_VERSIONS
    GRPC_WEB_PROXY_NOT_SUPPORTED
}

// Id of a transaction
abstraction TransactionId {
  @@immutable accountId:AccountId // the account that is the payer of the transaction
  @@immutable validStart:zonedDateTime // the start time of the transaction
  @@immutable @nullable nonce:int32 // nonce of an internal transaction
  
  string toString() // returns id in format TO_BE_DEFINED_IN_FUTURE_VERSIONS
  string toStringWithChecksum() // returns id in format TO_BE_DEFINED_IN_FUTURE_VERSIONS
}

// Helper to allow external signing of transactions
abstraction TransactionSigner {
  bytes signTransaction(transactionBytes: bytes); // returns the signature as byte array
}

// Basic definition of a transaction
abstraction Transaction {
  @@nullable maxTransactionFee:HBar // the maximal fee to be paid for this transaction
  @@nullable validDuration:long // in milliseconds, a better lang specific type can be used
  @@nullable memo:string // a memo to be attached to the transaction

  PackedTransaction packTransaction(client:HieroClient) // returns a new packed instance of the transaction (previously this was named frozen transaction)
}

// A packed transaction that can not change any parameters after it was created
PackedTransaction {
  @@immutable transactionId:TransactionId // the id of the transaction

  Transaction unpack() // returns a new basic transaction instance based on this packed transaction

  void sign(keyPair:KeyPair) // sign the transaction, if the lang supports it, we should provide a fluent API (return this)
  void sign(publicKey:PublicKey, transactionSigner:TransactionSigner) // sign the transaction, if the lang supports it, we should provide a fluent API (return this)

  @@async Response send() // send the transaction, we should provide async and sync versions in best case
  @@throws(timeout-error, io-error) Response sendAndWait(long timeout) // send the transaction, in milliseconds, a better lang specific type can be used
}

// The response of a transaction send request
Response {
  @immutable transactionId:TransactionId // the id of the transaction

  @@async Receipt queryReceipt() // query for the receipt of the transaction, we should provide async and sync versions in best case
  @@throws(timeout-error, io-error) Receipt queryReceiptAndWait(long timeout) // query for the receipt of the transaction, in milliseconds, a better lang specific type can be used
  
  @async Record queryRecord() // query for the record of the transaction, we should provide async and sync versions in best case
  @@throws(timeout-error, io-error) Record queryRecordAndWait(long timeout) // query for the record of the transaction, in milliseconds, a better lang specific type can be used
}

// The receipt of a transaction
Receipt {
  @@immutable transactionId:TransactionId // the transaction id
  @@immutable status:TransactionStatus // the status of the transaction
  @@immutable exchangeRate:HBarExchangeRate // the exchange rate at the time of the transaction
  @@immutable nextExchangeRate:HBarExchangeRate // the next exchange rate
}

// The record of a transaction
Record {
  @@immutable transactionId:TransactionId // the transaction id
  @@immutable consensusTimestamp:zonedDateTime // the consensus time of the transaction
  @@immutable receipt:Receipt // the receipt of the transaction
}

// factory methods of TransactionId that should be added to the namespace in the best language dependent way

TransactionId generateTransactionId(accountId: AccountId)
@@throws(illegal-format) TransactionId fromString(transactionId: string)

```

## Example

In the given example we assume that a concrete transaction type `FooTransaction` is defined.
The example shows how to create a new transaction instance and how to send it.

```
HieroClient client = ...
KeyPair keyPair = ...

FooTransaction transaction = new FooTransaction();
transaction.setBar("baz");

Response response = transaction.packTransaction(client)
           .sign(keyPair)
           .sendAndWait(30_000);

FooReceipt receipt = response.queryReceiptAndWait(30_000);

FooRecord record = response.queryRecordAndWait(30_000);
```

## Questions & Comments

- [@rwalworth](https://github.com/rwalworth): I can see use cases where it would be beneficial to switch the operator for a HieroClient (e.g. testing), as well as the network it connects to. I don't necessarily see a benefit in enforcing @@immutable here for these types.
- [@rwalworth](https://github.com/rwalworth) / [@0xivanov](https://github.com/0xivanov): Should maxTransactionFee and validDuration have default values?

