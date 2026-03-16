# Transactions API

This section defines the API for transactions.

## Description

The transactions API defines the basic building blocks for transactions.
It does not define specific transaction types but everything that is common to all transaction types.

The transaction lifecycle has two phases, each with its own type:

1. **Build phase** — `TransactionBuilder` is mutable and exposes domain-specific setters (e.g. `setKey`, `setAmount`). This is where the transaction body is constructed.
2. **Sign/send phase** — `Transaction` is immutable (in terms of the transaction body) and exposes `sign()`, `execute()`, serialization, and network configuration. This is where the transaction is signed, serialized, and submitted.

`build()` transitions from phase 1 to phase 2. `buildAndExecute()` is a convenience that handles both phases in one call for simple single-signer flows.

## API Schema

```
namespace transactions
requires common, keys, client

// Defines the status of a transaction. Since we can have custom transaction types based on custom
// services in the consensus node we can not use an enum here anymore.
abstraction TransactionStatus {
  @@immutable code:int32
}

// Defines the status codes that are currently used by services that are part of the consensus node repository
enum BasicTransactionStatus extends TransactionStatus {
    OK
    INVALID_TRANSACTION
    PAYER_ACCOUNT_NOT_FOUND
    ...
    GRPC_WEB_PROXY_NOT_SUPPORTED
}

// Id of a transaction
abstraction TransactionId {
  @@immutable accountId:common.AccountId
  @@immutable validStart:zonedDateTime
  @@immutable @@nullable nonce:int32

  string toString()
  string toStringWithChecksum()
}

// Helper to allow external signing of transactions
abstraction TransactionSigner {
  bytes signTransaction(transactionBytes: bytes)
}

// Mutable builder for constructing a transaction body. Concrete transaction builders extend this
// with domain-specific setters. The generic parameter is self-referential (CRTP) to enable fluent
// setter chains that return the concrete builder type.
abstraction TransactionBuilder<$$Transaction extends TransactionBuilder> {
  @@nullable maxTransactionFee:common.Hbar
  @@nullable validDuration:long
  @@nullable memo:string

  // Transitions from build phase to sign/send phase. If a client is provided, transactionId and
  // nodeAccountIds are auto-generated from the client. If no client is provided, they are left
  // unset (for flows like HIP-745 where incomplete transactions are serialized).
  Transaction build(@@nullable client:client.HieroClient)

  // Convenience for simple single-signer flows. Requires a client. Internally does:
  // build(client) -> sign(client.operator) -> execute(client)
  Response buildAndExecute(client:client.HieroClient)
}

// An immutable transaction ready for signing, serialization, and submission. The transaction body
// cannot be modified after build — only network execution config and signatures can be added.
Transaction {
  @@immutable @@nullable transactionId:TransactionId
  @@immutable @@nullable nodeAccountIds:list<common.AccountId>

  // Network execution config — does not affect the signed transaction body
  @@nullable maxAttempts:int32
  @@nullable maxBackoff:long
  @@nullable minBackoff:long
  @@nullable attemptTimeout:long

  void sign(keyPair:keys.KeyPair)
  void sign(publicKey:keys.PublicKey, transactionSigner:TransactionSigner)

  @@async Response execute(client:client.HieroClient)

  bytes toBytes()

  // Returns a mutable builder pre-populated with this transaction's body. Signatures are stripped
  // since any modification would invalidate them.
  TransactionBuilder unbuild()
}

// Factory methods for Transaction
Transaction fromBytes(transactionBytes:bytes)

// The response of a transaction execution
Response<$$Receipt extends Receipt, $$Record extends Record> {
  @@immutable transactionId:TransactionId

  @@async $$Receipt queryReceipt()
  @@async $$Record queryRecord()
}

// The receipt of a transaction
Receipt {
  @@immutable transactionId:TransactionId
  @@immutable status:TransactionStatus
  @@immutable exchangeRate:common.HBarExchangeRate
  @@immutable nextExchangeRate:common.HBarExchangeRate
}

// The record of a transaction
Record<$$Receipt extends Receipt> {
  @@immutable transactionId:TransactionId
  @@immutable consensusTimestamp:zonedDateTime
  @@immutable receipt:$$Receipt
}

// Factory methods for TransactionId
TransactionId generateTransactionId(accountId:common.AccountId)
@@throws(illegal-format) TransactionId fromString(transactionId:string)

```

## Examples

### Simple flow (single signer)

The most common case. `buildAndExecute` handles build, signing with the client operator, and execution in one call.

```
HieroClient client = ...

Response response = new FooTransactionBuilder()
    .setBar("baz")
    .buildAndExecute(client);

FooReceipt receipt = response.queryReceipt();
```

### Multi-party signing

For transactions requiring multiple signatures. `build(client)` auto-generates `transactionId` and `nodeAccountIds`, then the transaction is serialized and sent to other parties for signing.

```
HieroClient client = ...

// Alice builds and signs
Transaction tx = new TransferTransactionBuilder()
    .addTransfer(alice, Hbar.from(-5))
    .addTransfer(bob, Hbar.from(5))
    .build(client);

tx.sign(aliceKey);
bytes txBytes = tx.toBytes();
// send txBytes to Bob

// Bob receives, signs, executes
Transaction tx2 = Transaction.fromBytes(txBytes);
tx2.sign(bobKey);
Response response = tx2.execute(client);
```

### HIP-745 (dApp to wallet, incomplete transaction)

For flows where a dApp builds a transaction without a transactionId or nodeAccountIds and sends it to a wallet for completion.

```
// dApp builds without a client — no transactionId or nodeAccountIds generated
Transaction tx = new TransferTransactionBuilder()
    .addTransfer(alice, Hbar.from(-10))
    .addTransfer(bob, Hbar.from(10))
    .build();

bytes txBytes = tx.toBytes();
// send txBytes to wallet

// Wallet receives, unbuilds to modify, then rebuilds with its own client
TransactionBuilder builder = Transaction.fromBytes(txBytes).unbuild();
Transaction tx2 = builder.build(walletClient);
tx2.sign(walletKey);
tx2.execute(walletClient);
```

## Questions & Comments

- [@rwalworth](https://github.com/rwalworth): I can see use cases where it would be beneficial to switch the operator for a HieroClient (e.g. testing), as well as the network it connects to. I don't necessarily see a benefit in enforcing @@immutable here for these types.
- [@rwalworth](https://github.com/rwalworth) / [@0xivanov](https://github.com/0xivanov): Should maxTransactionFee and validDuration have default values?
