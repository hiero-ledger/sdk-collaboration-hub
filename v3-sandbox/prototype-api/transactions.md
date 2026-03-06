# Transactions API

This section defines the API for transactions.

## Description

The transactions API defines the basic building blocks for transactions.
It does not define specific transaction types but everything that is common to all transaction types.

Transactions use a **single-type design with an internal state machine**:

- **`Transaction`** — Extends `ConsensusRequest` (which extends `Request`) and implements `Executable<TransactionResponse>` and `GrpcRequest`. It starts in a **mutable** state where fields can be set freely, and transitions to a **frozen** state when finalized (transactionId and nodeAccountIds are assigned, the body is locked). Concrete transaction types (e.g. `AccountCreateTransaction`) extend this.

### State Machine

| State | Can modify fields? | Can sign? | Can execute? | Can serialize? |
|---|---|---|---|---|
| **Mutable** | Yes | No (auto-freezes first) | No (auto-freezes first) | Yes (serializes incomplete form) |
| **Frozen** | No (throws at runtime) | Yes | Yes | Yes |

- `sign()` auto-freezes if the transaction is still mutable (signing requires a finalized body).
- `execute()` auto-freezes if needed, then auto-signs with the client's operator key.
- `toBytes()` works in **any** state — serializes whatever is there, even if incomplete. This enables dApps to serialize an unsigned, unfrozen transaction for transport to a wallet.
- `fromBytes()` deserializes; if the transaction was frozen when serialized, the deserialized form is also frozen.
- Once any signature is added (including via auto-freeze), modifications throw at runtime.

### Why not PackedTransaction?

An earlier design used a two-type split: `Transaction` (mutable builder) and `PackedTransaction` (immutable executor, produced by `pack()`). This was dropped because:

1. **Conflicts with community direction on freeze deprecation** — [Issue #56](https://github.com/hiero-ledger/sdk-collaboration-hub/issues/56) and its linked discussions ([hiero-sdk-js#1445](https://github.com/hiero-ledger/hiero-sdk-js/issues/1445), [hedera-sdk-reference#127](https://github.com/hashgraph/hedera-sdk-reference/issues/127)) show the community wants to deprecate `freeze()` as an explicit step in favor of auto-freezing on `sign()`/`execute()`. `PackedTransaction` would have made freeze **mandatory** at the type level — `pack()` was semantically identical to `freezeWith()`, just returning a new type.
2. **Architectural complexity** — Both `Transaction` and `PackedTransaction` needed the same config fields (`maxAttempts`, `nodeAccountIds`, etc.), leading to awkward inheritance or field duplication across every design iteration.
3. **v2 SDKs prove runtime checks are sufficient** — Every existing SDK uses a single `Transaction` type with an internal frozen flag. `execute()` auto-freezes. Accidental modification of frozen transactions is extremely rare in practice because transaction flows are naturally linear.

See [requests.md](requests.md) for the full hierarchy and [requests-core.md](requests-core.md) for base type definitions.

## API Schema

```
namespace transactions
requires common, keys, client, requests-core

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
// TRANSACTION (single type — mutable builder + executable)
// ============================================================================

// Class chain: Transaction -> ConsensusRequest -> Request
// Contracts: implements Executable<TransactionResponse>, GrpcRequest
//
// Starts mutable, transitions to frozen on sign()/execute()/explicit freeze.
// See the State Machine table in the Description section above.
abstraction Transaction extends ConsensusRequest, Executable<TransactionResponse>, GrpcRequest {
    // Assigned during freeze (auto or explicit). Null while mutable.
    @@nullable transactionId: TransactionId

    @@nullable maxTransactionFee: common.Hbar

    @@nullable validDuration: duration

    @@nullable memo: string

    // Sign the transaction. Auto-freezes if still mutable.
    void sign(keyPair: keys.KeyPair)
    void sign(publicKey: keys.PublicKey, transactionSigner: TransactionSigner)

    // Serialize the transaction to bytes. Works in any state (mutable or frozen).
    bytes toBytes()

    // Execute the transaction. Auto-freezes if still mutable, auto-signs with operator.
    @@async
    TransactionResponse execute(client: HieroClient)
}

// Transactions that split large payloads into chunks.
abstraction ChunkedTransaction extends Transaction {
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

// Deserialize a Transaction from bytes (produced by Transaction.toBytes()).
// If the transaction was frozen when serialized, the deserialized form is also frozen.
// TransactionSupport SPI is used to identify the transaction type from the proto body.
@@throws(illegal-format) Transaction fromBytes(bytes: bytes)

```

## Example

```
HieroClient client = ...
KeyPair keyPair = ...

// Simple path: build, execute, done
TransactionResponse response = new FooTransaction()
    .setBar("baz")
    .setMaxAttempts(5)
    .execute(client)

FooReceipt receipt = response.queryReceipt(client)
FooRecord record = response.queryRecord(client)
```

### Multi-party signing

```
// Alice builds and signs (auto-freezes on sign)
FooTransaction tx = new FooTransaction()
tx.setBar("baz")
tx.sign(aliceKeyPair)
bytes txBytes = tx.toBytes()

// Bob receives, signs, and executes
Transaction tx2 = Transaction.fromBytes(txBytes)
tx2.sign(bobKeyPair)
TransactionResponse response = tx2.execute(client)
```

### Serialization for wallet transport

```
// dApp builds an incomplete transaction and sends to wallet
FooTransaction tx = new FooTransaction()
tx.setBar("baz")
bytes txBytes = tx.toBytes()    // works without freeze — for transport
// send txBytes to wallet via JSON payload, QR code, etc.
```

## Questions & Comments

- [@rwalworth](https://github.com/rwalworth): I can see use cases where it would be beneficial to switch the operator for a HieroClient (e.g. testing), as well as the network it connects to. I don't necessarily see a benefit in enforcing @@immutable here for these types.
- [@rwalworth](https://github.com/rwalworth) / [@0xivanov](https://github.com/0xivanov): Should maxTransactionFee and validDuration have default values?
