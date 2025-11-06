# Transactions

This section defines the API for transactions.

```
namespace transactions
requires common,keys,client

// Defines the status of a transaction. Since we can have custom transaction types based on custom services in the consensus node we can not use an enum here anymore
@abstraction
TransactionStatus {
  @immutable code:int32 // the status code that should be unique based on the consensus node. 
}

enum BasicTransactionStatus extends TransactionStatus {
    OK
    INVALID_TRANSACTION
    PAYER_ACCOUNT_NOT_FOUND
    ...          // other status codes should be defined here TO_BE_DEFINED_IN_FUTURE_VERSIONS
    GRPC_WEB_PROXY_NOT_SUPPORTED
}

// Id of a transaction
@abstraction
TransactionId {
  @immutable accountId:AccountId // the account that is the payer of the transaction
  @immutable validStart:zonedDateTime // the start time of the transaction
  @immutable @optional nonce:int32 // nonce of an internal transaction
  
  string toString() // returns id in format TO_BE_DEFINED_IN_FUTURE_VERSIONS
  string toStringWithChecksum() // returns id in format TO_BE_DEFINED_IN_FUTURE_VERSIONS
}

// Helper to allow external signing of transactions
@abstraction
TransactionSigner {
  bytes signTransaction(transactionBytes: bytes); // returns the signature as byte array
}

// Basic definition of a transaction
@abstraction
Transaction {
  @optional fee:HBar // the maximal fee to be paid for this transaction
  @optional validDuration:long // in milliseconds, a better lang specific type can be used
  @optional memo:string // a memo to be attached to the transaction

  PackedTransaction packTransaction(client:HieroClient) // returns a new packed instance of the transaction (previouisly this was named frozen transaction)
}

// A packed transaction that can not change any parameters after it was created
@abstraction
PackedTransaction {
  @immutable transactionId:TransactionId // the id of the transaction

  Transaction unpack() // returns a new basic transaction instance based on this packed transaction

  // Question: Should we provide another complexe type like "SignedTransaction" that is created here and contains the send api? Is there any scenario where we want to send a transaction that is not signed?
  void sign(keyPair:KeyPair) // sign the transaction, if the lang supports it, we should provide a fluent API (return this)
  void sign(publicKey:PublicKey, transactionSigner:TransactionSigner) // sign the transaction, if the lang supports it, we should provide a fluent API (return this)

  @async Response send() // send the transaction, we should provide async and sync versions in best case
  Response sendAndWait(long timeout) // send the transaction, in milliseconds, a better lang specific type can be used
}

// The response of a transaction send request
@abstraction
Response {
  @immutable transactionId:TransactionId // the id of the transaction

  @async Receipt queryReceipt() // query for the receipt of the transaction, we should provide async and sync versions in best case
  Receipt queryReceiptAndWait(long timeout) // query for the receipt of the transaction, in milliseconds, a better lang specific type can be used
  
  @async Record queryRecord() // query for the record of the transaction, we should provide async and sync versions in best case
  Record queryRecordAndWait(long timeout) // query for the record of the transaction, in milliseconds, a better lang specific type can be used
}

// The receipt of a transaction
@abstraction
Receipt {
  @immutable transactionId:TransactionId // the transaction id
  @immutable status:TransactionStatus // the status of the transaction
  @immutable exchangeRate:HBarExchangeRate // the exchange rate at the time of the transaction
  @immutable nextExchangeRate:HBarExchangeRate // the next exchange rate
}

// The record of a transaction
@abstraction
Record {
  @immutable transactionId:TransactionId // the transaction id
  @immutable consensusTimestamp:zonedDateTime // the consensus time of the transaction
  @immutable receipt:Receipt // the receipt of the transaction
}

// factory methods of keys that should be added to the namespace in the best language dependent way

TransactionId generateTransactionId(accountId: AccountId)
TransactionId fromString(transactionId: string)

```
