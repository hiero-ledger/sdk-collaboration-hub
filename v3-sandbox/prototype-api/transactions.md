# Transactions API

This section defines the API for transactions.

## Description

The transactions API defines the basic building blocks for transactions.
It does not define specific transaction types but everything that is common to all transaction types.

Transactions use a **two-phase lifecycle**:

1. **`Transaction`** — A mutable builder for setting transaction parameters. It extends `ConsensusRequest` (which extends `Request`), inheriting retry/timeout config and `nodeAccountIds`. It does NOT implement `Executable` — users must call `pack()` before executing. Concrete transaction types (e.g. `AccountCreateTransaction`) extend this.
2. **`PackedTransaction<$$Data>`** — An immutable, signable, executable object produced by `Transaction.pack(client)`. It also extends `ConsensusRequest` and implements `Executable<TransactionResponse>`, participating in the shared `withRetry` retry loop. `pack()` transfers inherited `Request` config and `nodeAccountIds` from the builder to the packed form. See [requests.md](requests.md) for the full hierarchy.

`PackedTransaction` is generic over `$$Data` — the per-type transaction data. There is one `PackedTransaction` class for all transaction types. Per-type differences in proto serialization and gRPC method selection are handled by the `TransactionSupport` SPI (see [transactions-spi.md](transactions-spi.md)). Languages with type aliases may expose concrete names like `PackedAccountCreateTransaction = PackedTransaction<AccountCreateTransactionData>`.

## API Schema

```
namespace transactions
requires common, keys, client, requests

// Defines the status of a transaction. Since we can have custom transaction types based on custom services in the consensus node we can not use an enum here anymore
abstraction TransactionStatus {
  @@immutable code:int32
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
  @@immutable accountId:common.AccountId // the account that is the payer of the transaction
  @@immutable validStart:zonedDateTime // the start time of the transaction
  @@immutable @@nullable nonce:int32 // nonce of an internal transaction

  string toString() // returns id in format TO_BE_DEFINED_IN_FUTURE_VERSIONS
  string toStringWithChecksum() // returns id in format TO_BE_DEFINED_IN_FUTURE_VERSIONS
}

// Helper to allow external signing of transactions
abstraction TransactionSigner {
  bytes signTransaction(transactionBytes: bytes)
}

// ============================================================================
// TRANSACTION BUILDER
// ============================================================================

// Mutable builder for transaction parameters.
// Class chain: Transaction -> ConsensusRequest -> Request
// Inherits retry/timeout config from Request and nodeAccountIds from ConsensusRequest.
// Does NOT implement Executable — users must call pack() to get an executable form.
abstraction Transaction extends ConsensusRequest {
   // The maximal fee to be paid for this transaction.
  @@nullable maxTransactionFee: common.Hbar

  // In milliseconds, a better lang specific type can be used.
  @@nullable validDuration: long

  // A memo to be attached to the transaction.
  @@nullable memo: string

  // Returns a new packed instance of the transaction (previously this was named frozen transaction).
  PackedTransaction<$$Data> pack(client:HieroClient)
}

// Mutable builder for transactions that split large payloads into chunks.
abstraction ChunkedTransaction extends Transaction {
  @@async
  PackedChunkedTransaction<$$Data> pack(client:HieroClient)
}

// ============================================================================
// PACKED TRANSACTION (inside the Request hierarchy)
// ============================================================================

// Immutable, signed, executable transaction. Produced by Transaction.pack().
// Class chain: PackedTransaction -> ConsensusRequest -> Request
// Contract: implements Executable<TransactionResponse>
//
// Parameterized by $$Data — the per-type transaction data. One generic class
// for all transaction types; TransactionSupport SPI handles per-type differences.
abstraction PackedTransaction<$$Data> extends ConsensusRequest, Executable<TransactionResponse> {
  // The id of the transaction.
  @@immutable transactionId: TransactionId

  // Returns a new basic transaction instance based on this packed transaction.
  Transaction unpack()

  // Sign the transaction, if the lang supports it, we should provide a fluent API.
  void sign(keyPair: keys.KeyPair)
  void sign(publicKey: keys.PublicKey, transactionSigner: TransactionSigner)

  // Convert the transaction to a byte array.
  bytes toBytes()

  // Send the transaction, we should provide async and sync versions in best case.
  @@async
  TransactionResponse send()
}

// Packed form of chunked transactions. Handles multi-chunk signing and execution.
abstraction PackedChunkedTransaction<$$Data> extends PackedTransaction<$$Data> {
}

// ============================================================================
// RESPONSE TYPES
// ============================================================================

// The response of a transaction execution
TransactionResponse extends Response<TransactionReceipt, TransactionRecord> {
}

// Generic response from executing a transaction
Response<$$Receipt extends Receipt, $$Record extends Record> {
  // The id of the transaction.
  @@immutable transactionId:TransactionId

  // Query for the receipt of the transaction, we should provide async and sync versions in best case.
  @@async
  $$Receipt queryReceipt(client:HieroClient)

  // Query for the record of the transaction, we should provide async and sync versions in best case.
  @@async
  $$Record queryRecord(client:HieroClient)
}

// The receipt of a transaction
Receipt {
  // The transaction ID.
  @@immutable transactionId: TransactionId 

  // The status of the transaction.
  @@immutable status: TransactionStatus
  
  // The exchange rate at the time of the transaction.
  @@immutable exchangeRate: common.HBarExchangeRate 

  // The next exchange rate.
  @@immutable nextExchangeRate: common.HBarExchangeRate 
}

// The record of a transaction
Record<$$Receipt extends Receipt> {
  @@immutable transactionId:TransactionId
  @@immutable consensusTimestamp:zonedDateTime
  @@immutable receipt:$$Receipt
}

// ============================================================================
// FACTORY METHODS
// ============================================================================

TransactionId generateTransactionId(accountId: common.AccountId)
@@throws(illegal-format) TransactionId fromString(transactionId: string)

// Deserialize a PackedTransaction from bytes (produced by PackedTransaction.toBytes()).
// TransactionSupport SPI is used to identify the transaction type from the proto body.
@@throws(illegal-format) PackedTransaction fromBytes(bytes: bytes)

```

## Example

```
HieroClient client = ...
KeyPair keyPair = ...

FooTransaction transaction = new FooTransaction()
transaction.setBar("baz")

TransactionResponse response = transaction.pack(client)
           .sign(keyPair)
           .execute(client)

FooReceipt receipt = response.queryReceipt(client)

FooRecord record = response.queryRecord(client)
```

## Questions & Comments

- [@rwalworth](https://github.com/rwalworth): I can see use cases where it would be beneficial to switch the operator for a HieroClient (e.g. testing), as well as the network it connects to. I don't necessarily see a benefit in enforcing @@immutable here for these types.
- [@rwalworth](https://github.com/rwalworth) / [@0xivanov](https://github.com/0xivanov): Should maxTransactionFee and validDuration have default values?
