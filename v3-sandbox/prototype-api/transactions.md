# Transactions API

This section defines the API for transactions.

## Description

The transactions API defines the basic building blocks for transactions. It does not define specific transaction types but everything that is common to all transaction types.

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
  @@nullable validDuration: long
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

// An immutable transaction ready for signing, serialization, and submission. The transaction body
// cannot be modified after build — only network execution config and signatures can be added.
//
// The generic parameter $$Response carries the typed response produced when the transaction is
// executed. This ensures that the typed return value is preserved through the full
// build() → sign() → execute() chain, including multi-party signing flows.
@@finalType
Transaction<$$Response extends Response> {
  // Network execution config — does not affect the signed transaction body
  @@nullable maxAttempts: int32
  @@nullable maxBackoff: long
  @@nullable minBackoff: long
  @@nullable attemptTimeout: long

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

  // Deserialize a transaction from bytes. Returns a raw Transaction<Response> because the response
  // type cannot be inferred from bytes alone. If the transaction type is known at the call site
  // (e.g. in a multi-party signing flow), language implementations may provide typed overloads
  // or allow an explicit cast to the expected Transaction<$$Response>.
  @@static
  Transaction<Response> fromBytes(transactionBytes: bytes)

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

FooResponse response = new FooTransactionBuilder()
    .setBar("baz")
    .buildAndExecute(client);

FooReceipt receipt = response.queryReceipt();
```

### Multi-party signing

For transactions requiring multiple signatures. `build(client)` returns a `Transaction<$$Response>`, preserving the
typed response through the signing chain. This is the key benefit of the generic parameter on `Transaction`.

```
HieroClient client = ...

// Alice builds — Transaction<AccountCreateResponse> carries the type forward
Transaction<AccountCreateResponse> tx = new AccountCreateTransactionBuilder()
    .setKey(keyOfNewAccount)
    .setInitialBalance(new Hbar(100, HbarUnit.HBAR))
    .build(client);

tx.sign(aliceKey);
bytes txBytes = tx.toBytes();
// send txBytes to Bob

// Bob receives and signs. fromBytes() returns Transaction<Response> (raw — type cannot be inferred
// from bytes). Because Bob knows the transaction type out of band, he may cast explicitly.
Transaction<AccountCreateResponse> tx2 = (Transaction<AccountCreateResponse>) Transaction.fromBytes(txBytes);
tx2.sign(bobKey);

AccountCreateResponse response = tx2.execute(client);
AccountId newAccountId = response.queryReceipt().getAccountId();
```

### HIP-745 (dApp to wallet, incomplete transaction)

For flows where a dApp builds a transaction without a transactionId or nodeAccountIds and sends it to a wallet for
completion. Both sides treat the transaction as raw (`Transaction<Response>`) because the wallet operates on
arbitrary transaction types.

```
// dApp builds without a client — no transactionId or nodeAccountIds generated
Transaction<Response> tx = new TransferTransactionBuilder()
    .addTransfer(alice, Hbar.from(-10))
    .addTransfer(bob, Hbar.from(10))
    .build();

bytes txBytes = tx.toBytes();
// send txBytes to wallet

// Wallet receives, unbuilds to modify, then rebuilds with its own client
TransactionBuilder builder = Transaction.fromBytes(txBytes).unbuild();
Transaction<Response> tx2 = builder.build(walletClient);
tx2.sign(walletKey);
tx2.execute(walletClient);
```

## Design Rationale: Typed Receipts, Universal Records

### The problem with a single base Receipt

The v2 SDKs use a single `TransactionReceipt` with roughly 15 optional/nullable fields — `accountId`,
`fileId`, `contractId`, `topicId`, `tokenId`, `scheduleId`, `scheduledTransactionId`, `serials`,
`topicSequenceNumber`, `topicRunningHash`, and more. Every field is nullable because it only applies to a
specific transaction type. After creating an account, for example, the user must know to read `.accountId`
and accept that all other fields are null. The compiler cannot help, and documentation cannot fully
substitute for type safety.

### Why receipts are typed

The concern with per-type receipts is that a future protocol change could add a receipt field to an
existing transaction type that previously had no typed receipt, changing its `queryReceipt()` return type.
This is a real risk and the decision to use typed receipts should be revisited with broader community input
before V3 is finalized.

That said, the historical evidence from `transaction_receipt.proto` across 7 years of mainnet is
encouraging:

- Every new receipt field introduced since genesis has been tied to a **new transaction type** (HCS, HTS,
  scheduled transactions, NFTs, node management, etc.).
- The one cross-cutting addition — `block_number` (added Mar 2026, block stream work) — applies to
  **all** transactions and therefore lives on the base `Receipt` type. It does not require changes to any
  typed receipt subtype.
- There is no historical precedent for an existing transaction type that returns only a base `Receipt`
  gaining a transaction-specific receipt field after the fact.

This pattern holds because the receipt's purpose is narrow: "what entity was created, and did it succeed?"
That question is answered at the time a transaction type is designed, not later. The set of transactions
needing typed receipts is listed below and should be treated as a working draft pending broader review.

### Why responses are named

The protobuf `TransactionResponse` — the immediate gRPC reply from the consensus node — has exactly two
fields (`nodeTransactionPrecheckCode`, `cost`) and has had **zero structural changes** in 7+ years of
mainnet. It is a pure pre-consensus acknowledgement: "the node received your transaction and will gossip
it." There is no transaction-specific data in it and there never has been. The risk of named SDK `Response`
aliases causing breaking changes from protocol evolution is low, though the same caution applied to typed
receipts applies here as well.

Named response aliases exist for ergonomics. There is a meaningful difference between a *structural
description* and a *name*:

```
Response<AccountCreateReceipt> response = builder.buildAndExecute(client);  // structural, verbose
AccountCreateResponse response = builder.buildAndExecute(client);           // named, clear intent
```

The named type appears in call sites, method signatures, error messages, and documentation in a way that
immediately tells the user what kind of operation produced it.

Response types are declared using `@@alias` (see `guides/api-guideline.md`), which communicates that these
are names for parameterizations of `Response<$$Receipt>` rather than distinct type extensions. See each
language's best practice guide for the implementation pattern.

Named response aliases **must have no fields or methods**. All transaction-specific data lives on the
typed `Receipt` or the universal `Record`. The `Response`'s only job is to carry the transaction ID and
provide the async poll methods.

Every transaction type gets a named response alias. Transactions with receipt-specific output are
parameterized with a typed receipt (`AccountCreateResponse` = `Response<AccountCreateReceipt>`).
Transactions without are parameterized with the base receipt (`AccountUpdateResponse` = `Response<Receipt>`).
Either way the user gets a named type.

### Why records are NOT typed (risk: ~60%)

The `TransactionRecord` protobuf tells a different story. Its fields evolve with protocol features that cut
across subsets of **existing** transaction types:

- `assessed_custom_fees` (field 13) — applies to CryptoTransfer and token transfers
- `paid_staking_rewards` (field 18) — applies to practically every transaction that adjusts stake
- `high_volume_pricing_multiplier` (field 23, added Feb 2026, HIP-1313) — applies to ~14 transaction
  types today, with the subset expected to expand over time

If named record subtypes existed for these transaction types, each expansion would change the
`queryRecord()` return type for newly-affected transactions — a breaking change across all SDK
implementations.

The record is the protocol's extension surface. Protocol-specific record data belongs as nullable fields
on the universal `Record<$$Receipt>` base type, not in named subtypes. This keeps additions non-breaking
by design.

### Transactions needing typed receipts

| Transaction              | New receipt field(s)                      |
|--------------------------|-------------------------------------------|
| AccountCreate            | `accountId`                               |
| FileCreate               | `fileId`                                  |
| ContractCreate           | `contractId`                              |
| TopicCreate              | `topicId`                                 |
| TokenCreate              | `tokenId`                                 |
| ScheduleCreate           | `scheduleId`, `scheduledTransactionId`    |
| TokenMint (NFT)          | `serials`, `totalSupply`                  |
| TopicMessageSubmit       | `topicSequenceNumber`, `topicRunningHash` |

Transactions not listed above — those whose receipts carry no transaction-specific output fields — still
get a named `Response` alias but are parameterized with the base `Receipt` rather than a typed one. Note
that this table is a working draft: which transactions belong here should be validated with broader
community input before V3 is finalized.

### How to define a typed transaction in practice

For a transaction that creates an entity (typed receipt, named response):

```
@@finalType
FooTransactionBuilder extends transactions.TransactionBuilder<FooTransactionBuilder, FooResponse> {
    // domain-specific fields
}

@@alias FooResponse = transactions.Response<FooReceipt>

@@finalType
FooReceipt extends transactions.Receipt {
    @@immutable fooId: common.FooId  // the only new field
}
```

For a transaction with no receipt-specific output (base receipt, named response):

```
@@finalType
BazTransactionBuilder extends transactions.TransactionBuilder<BazTransactionBuilder, BazResponse> {
    // domain-specific fields
}

@@alias BazResponse = transactions.Response<transactions.Receipt>

## Questions & Comments

- [@rwalworth](https://github.com/rwalworth): I can see use cases where it would be beneficial to switch the operator for a HieroClient (e.g. testing), as well as the network it connects to. I don't necessarily see a benefit in enforcing @@immutable here for these types.
- [@rwalworth](https://github.com/rwalworth) / [@0xivanov](https://github.com/0xivanov): Should maxTransactionFee and validDuration have default values?
