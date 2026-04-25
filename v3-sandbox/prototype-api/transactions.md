# Transactions API

This section defines the API for transactions.

## Description

The transactions API defines the basic building blocks for transactions. It does not define specific transaction types but everything that is common to all transaction types.

The transaction lifecycle has two phases, each with its own type:

1. **Build phase** — `TransactionBuilder` is mutable and exposes domain-specific setters (e.g. `setKey`, `setAmount`). This is where the transaction body is constructed.
2. **Sign/send phase** — `Transaction` is immutable (in terms of the transaction body) and exposes `sign()`, `execute()`, serialization, and network configuration. This is where the transaction is signed, serialized, and submitted.

`build()` transitions from phase 1 to phase 2. `buildAndExecute()` is a convenience that handles both phases in one call for simple single-signer flows.

`Transaction` extends `ConsensusCall<$$Response>` from the request hierarchy and implements `GrpcTransport`. See [requests.md](requests.md) for the full hierarchy and [requests-core.md](requests-core.md) for base type definitions.

## API Schema

```
namespace transactions
requires common, keys, client, requests-core

// Defines the status of a transaction. Since we can have custom transaction types based on custom
// services in the consensus node we can not use an enum here anymore.
abstraction TransactionStatus {
  @@immutable code:int32 // the status code that should be unique based on the consensus node
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
  @@immutable accountId:common.AccountId // the account that is the payer of the transaction
  @@immutable validStart:zonedDateTime // the start time of the transaction
  @@immutable @@nullable nonce:int32 // nonce of an internal transaction

  string toString() // returns the transaction id as a string
  string toStringWithChecksum() // returns the transaction id as a string with a checksum
}

// Helper to allow external signing of transactions
abstraction TransactionSigner {
  bytes signTransaction(transactionBytes: bytes) // returns the signature as a byte array
}

// Mutable builder for constructing a transaction body. Concrete transaction builders extend this
// with domain-specific setters.
//
// Two generic parameters:
//   $$Builder  — self-referential (CRTP) so fluent setter chains return the concrete builder type.
//   $$Response — the typed response produced by this transaction. Use Response<Receipt>
//                for transactions that do not add entity-specific receipt fields.
abstraction TransactionBuilder<$$Builder extends TransactionBuilder, $$Response extends Response> {
  @@nullable maxTransactionFee: common.Hbar
  @@nullable validDuration: int64
  @@nullable memo: string
  @@nullable transactionId: TransactionId
  nodeAccountIds: list<common.AccountId>

  // Transitions from build phase to sign/send phase. If a client is provided, transactionId and
  // nodeAccountIds are auto-generated from the client. If no client is provided, they are left
  // unset (for flows like HIP-745 where incomplete transactions are serialized).
  Transaction<$$Response> build(@@nullable client: client.HieroClient)

  // Convenience for simple single-signer flows. Requires a client. Internally does:
  // build(client) -> sign(client.operator) -> execute(client)
  @@async
  $$Response buildAndExecute(client: client.HieroClient)
}

// An immutable transaction ready for signing, serialization, and submission. Extends
// ConsensusCall<$$Response> from the request hierarchy (see requests-core.md) and implements
// GrpcTransport. The transaction body cannot be modified after build — only network
// execution config and signatures can be added.
//
// The generic parameter $$Response carries the typed response produced when the transaction is
// executed. This ensures that the typed return value is preserved through the full
// build() → sign() → execute() chain, including multi-party signing flows.
@@finalType
Transaction<$$Response extends Response> extends requests-core.ConsensusCall<$$Response> : requests-core.GrpcTransport {

  // Sign the transaction with the given key pair. Returns self to allow chaining.
  Transaction<$$Response> sign(keyPair: keys.KeyPair)

  // Sign the transaction using an external signer. Returns self to allow chaining.
  Transaction<$$Response> sign(publicKey: keys.PublicKey, transactionSigner: TransactionSigner)

  // Returns the signatures that have been added to this transaction, keyed by node account id and public key
  map<common.AccountId, map<keys.PublicKey, bytes>> getSignatures()

  // Submit the transaction to the network and return the typed response
  @@async
  $$Response execute(client: client.HieroClient)

  // Serialize the transaction (including signatures) to bytes
  bytes toBytes()

  // Deserialize a transaction from bytes. Returns Transaction<Response<Receipt>> because the response
  // type cannot be inferred from bytes alone. If the transaction type is known at the call site
  // (e.g. in a multi-party signing flow), language implementations may provide typed overloads
  // or allow an explicit cast to the expected Transaction<$$Response>.
  @@static
  Transaction<Response<Receipt>> fromBytes(transactionBytes: bytes)

  // Returns a mutable builder pre-populated with this transaction's body. Because Transaction is
  // generic but TransactionBuilder requires two type parameters (the concrete builder type is not
  // recoverable from bytes alone), this returns the base TransactionBuilder. Callers that know
  // the concrete builder type may cast to it.
  TransactionBuilder unbuild()
}

// The response of a transaction execution. Parameterized by the typed receipt only.
//
// Records are NOT parameterized here by design: the TransactionRecord protobuf evolves with
// cross-cutting protocol features (assessed_custom_fees, paid_staking_rewards,
// high_volume_pricing_multiplier, etc.) that apply to subsets of existing transaction types and
// expand over time. Named record subtypes would create breaking changes whenever a new protocol
// feature extends to additional transaction types. queryRecord() therefore always returns the
// universal Record<$$Receipt>, with nullable fields on the base Record for protocol-specific data.
//
// See the "Design Rationale" section for the full analysis.
Response<$$Receipt extends Receipt> {
  @@immutable transactionId: TransactionId // the id of the transaction

  @@async $$Receipt queryReceipt()          // query for the receipt of the transaction
  @@async Record<$$Receipt> queryRecord()   // query for the record of the transaction
}

// The receipt of a transaction. Subtype this only when the transaction creates an entity or
// returns meaningful data in the receipt (e.g. AccountCreate → accountId, TokenMint → serials).
// For all other transactions, use this base type directly.
Receipt {
  @@immutable transactionId: TransactionId     // the id of the transaction
  @@immutable status: TransactionStatus        // the status of the transaction
  @@immutable exchangeRate: common.HBarExchangeRate     // the exchange rate at the time of the transaction
  @@immutable nextExchangeRate: common.HBarExchangeRate // the next exchange rate
}

// The record of a transaction. Named subtypes of Record are intentionally not used — see the
// "Design Rationale" section. All protocol-specific record fields are nullable on this base type.
Record<$$Receipt extends Receipt> {
  @@immutable transactionId: TransactionId          // the id of the transaction
  @@immutable consensusTimestamp: zonedDateTime      // the consensus time of the transaction
  @@immutable receipt: $$Receipt                     // the typed receipt of the transaction
}

// Factory methods for TransactionId
TransactionId generateTransactionId(accountId:common.AccountId)
@@throws(illegal-format) TransactionId fromString(transactionId:string)

```

## Examples

### Simple flow (single signer)

The most common case. `buildAndExecute` handles build, signing with the client operator, and execution in one call.
The typed response is inferred directly from the builder's `$$Response` parameter.

```
HieroClient client = ...

Response<FooReceipt> response = new FooTransactionBuilder()
    .setBar("baz")
    .buildAndExecute(client);

FooReceipt receipt = response.queryReceipt();
```

### Multi-party signing

For transactions requiring multiple signatures. `build(client)` returns a `Transaction<Response<$$Receipt>>`, preserving the
typed response through the signing chain. This is the key benefit of the generic parameter on `Transaction`.

```
HieroClient client = ...

// Alice builds — Transaction<Response<AccountCreateReceipt>> carries the type forward
Transaction<Response<AccountCreateReceipt>> tx = new AccountCreateTransactionBuilder()
    .setKey(keyOfNewAccount)
    .setInitialBalance(new Hbar(100, HbarUnit.HBAR))
    .build(client);

tx.sign(aliceKey);
bytes txBytes = tx.toBytes();
// send txBytes to Bob

// Bob receives and signs. fromBytes() returns Transaction<Response<Receipt>> (raw — type cannot be
// inferred from bytes). Because Bob knows the transaction type out of band, he may cast explicitly.
Transaction<Response<AccountCreateReceipt>> tx2 = (Transaction<Response<AccountCreateReceipt>>) Transaction.fromBytes(txBytes);
tx2.sign(bobKey);

Response<AccountCreateReceipt> response = tx2.execute(client);
AccountId newAccountId = response.queryReceipt().getAccountId();
```

### HIP-745 (dApp to wallet, incomplete transaction)

For flows where a dApp builds a transaction without a transactionId or nodeAccountIds and sends it to a wallet for
completion. Both sides treat the transaction as `Transaction<Response<Receipt>>` because the wallet operates on
arbitrary transaction types.

```
// dApp builds without a client — no transactionId or nodeAccountIds generated
Transaction<Response<Receipt>> tx = new TransferTransactionBuilder()
    .addTransfer(alice, Hbar.from(-10))
    .addTransfer(bob, Hbar.from(10))
    .build();

bytes txBytes = tx.toBytes();
// send txBytes to wallet

// Wallet receives, unbuilds to modify, then rebuilds with its own client
TransactionBuilder builder = Transaction.fromBytes(txBytes).unbuild();
Transaction<Response<Receipt>> tx2 = builder.build(walletClient);
tx2.sign(walletKey);
tx2.execute(walletClient);
```

## Design Rationale

Receipts are typed per-transaction because the receipt's purpose is narrow ("what entity was created?")
and 7 years of mainnet history show that receipt fields have always been introduced alongside new
transaction types, never added to existing ones after the fact. Records are intentionally not typed
because the `TransactionRecord` protobuf evolves with cross-cutting protocol features that expand to new
transaction types over time — named record subtypes would produce breaking changes as that set grows.

**This is a working draft.** Typed receipts and responses carry real protocol-evolution risk and these
decisions should be validated with broader community input before V3 is finalized.

### Transactions needing typed receipts

| Transaction              | New receipt field(s)                                                |
|--------------------------|---------------------------------------------------------------------|
| AccountCreate            | `accountId`                                                         |
| FileCreate               | `fileId`                                                            |
| ContractCreate           | `contractId`                                                        |
| TopicCreate              | `topicId`                                                           |
| TokenCreate              | `tokenId`                                                           |
| ScheduleCreate           | `scheduleId`, `scheduledTransactionId`                              |
| TokenMint                | `newTotalSupply`; `@@nullable serials` (NFT mints only)             |
| TokenBurn                | `newTotalSupply`                                                    |
| TokenWipe                | `newTotalSupply`                                                    |
| NodeCreate               | `nodeId`                                                            |
| TopicMessageSubmit       | `topicSequenceNumber`, `topicRunningHash`                           |

Transactions not listed above — those whose receipts carry no transaction-specific output fields — use
`Response<Receipt>` directly. Note that this table is a working draft: which transactions belong here
should be validated with broader community input before V3 is finalized.

### How to define a typed transaction in practice

For a transaction that creates an entity (typed receipt):

```
@@finalType
FooTransactionBuilder extends transactions.TransactionBuilder<FooTransactionBuilder, transactions.Response<FooReceipt>> {
    // domain-specific fields
}

@@finalType
FooReceipt extends transactions.Receipt {
    @@immutable fooId: common.FooId  // the only new field
}
```

For a transaction with no receipt-specific output (base receipt):

```
@@finalType
BazTransactionBuilder extends transactions.TransactionBuilder<BazTransactionBuilder, transactions.Response<transactions.Receipt>> {
    // domain-specific fields
}
```

## Questions & Comments

- [@rwalworth](https://github.com/rwalworth): I can see use cases where it would be beneficial to switch the operator for a HieroClient (e.g. testing), as well as the network it connects to. I don't necessarily see a benefit in enforcing @@immutable here for these types.
- [@hendrikEbbers](https://github.com/hendrikebbers): Only because protobuf defined 1 generic record type we do not need to do it if we can provide a more simple/smaller/concreter scope/api to the app developer. Nothing we need to decide today. We should discuss pro/con of that
- [@hendrikEbbers](https://github.com/hendrikebbers): It might make sense to have even for transaction types that does not add new attributes concrete implementations for Response/Receipt. Those classes could provide documentation and help in switch cases as example.
- [@rwalworth](https://github.com/rwalworth) / [@0xivanov](https://github.com/0xivanov): Should maxTransactionFee and validDuration have default values?
