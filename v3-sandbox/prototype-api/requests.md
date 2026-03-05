# Requests API

This section defines the request hierarchy for v3 SDKs, covering all request types across consensus nodes, mirror nodes, and block nodes.

## Description

The requests API defines the foundational types for all SDK requests: transactions, queries, and streaming subscriptions. It replaces the v2 `Executable` base class with a design that cleanly separates:

- **Shared configuration** (`Request` base) from **execution behavior** (`Executable`, `Subscribable` contracts)
- **Network targeting** (`Network` contract, network-specific request bases) from **request semantics** (transaction signing, query payment, etc.)
- **Transaction building** (mutable `Transaction`) from **transaction execution** (immutable `PackedTransaction`)

This design avoids diamond inheritance by keeping all contracts (interfaces) outside the class inheritance tree. Every type has exactly one class-inheritance path to `Request`.

**Note:** Transaction types use a two-phase lifecycle. `Transaction` is a mutable builder that extends `ConsensusRequest` (inheriting retry/timeout config and `nodeAccountIds`) but does NOT implement `Executable`. Calling `pack(client)` produces an immutable `PackedTransaction<$$Data>` that also extends `ConsensusRequest` and implements `Executable`. This enforces at the type level that you cannot execute an unfinalized transaction, and you cannot modify a packed one. `pack()` transfers inherited `Request` config and `nodeAccountIds` from the builder to the packed form. See [transactions.md](transactions.md) for supporting types (`TransactionId`, `TransactionStatus`, `Response`, `Receipt`, `Record`).

### Keywords

This document uses two meta-language keywords for abstract types (see [API Guideline](../../guides/api-guideline.md)):

- **`contract`** — Only method signatures. Implement as interfaces (Java/Go/TS), protocols (Swift), traits (Rust), or ABCs with no concrete methods (Python).
- **`abstraction`** — Shared fields and implementation. Implement as abstract classes (Java/C++/Python/JS/TS/Swift) or embedded structs + generics (Go/Rust).

## API Schema

```
namespace requests
requires common, client, keys

// ============================================================================
// CONTRACTS
// ============================================================================

// Provides node selection and health tracking for a network.
// 4 of 7 existing SDKs already have a shared network base (Java BaseNetwork,
// JS ManagedNetwork, Go _ManagedNetwork, C++ BaseNetwork). This formalizes
// that existing pattern as an explicit contract.
contract Network<$$Node> {
    $$Node selectNode()
    void markHealthy(node: $$Node)
    void markUnhealthy(node: $$Node)
}

// Any request that produces a single response via execute().
// Does NOT extend Request — avoids diamond inheritance.
contract Executable<$$Response> {
    @@async
    @@throws(network-error, request-timeout, max-attempts-exceeded)
    $$Response execute(client: HieroClient)
}

// Any request that produces a stream of items via subscribe().
// Does NOT extend Request — avoids diamond inheritance.
contract Subscribable<$$Item> {
    Subscribable setCompletionHandler(handler: void callback())
    Subscribable setErrorHandler(handler: void callback(error: $$Error))
    Subscribable setRetryHandler(handler: bool callback(error: $$Error))

    @@async
    @@throws(network-error, network-not-configured)
    SubscriptionHandle subscribe(client: HieroClient, onNext: void callback(item: $$Item))
}

// Handle returned by Subscribable.subscribe() to control a streaming subscription.
SubscriptionHandle {
    void unsubscribe()
}

// ============================================================================
// ROOT BASE
// ============================================================================

// Root of all request types. Carries shared retry and timeout configuration.
// Also provides the internal withRetry execution loop (see Requests SPI).
abstraction Request {
    @@default(10) maxAttempts: int32
    @@default(8s) maxBackoff: duration
    @@default(250ms) minBackoff: duration
    @@nullable grpcDeadline: duration
    @@nullable requestTimeout: duration
}

// ============================================================================
// CONCRETE NETWORK TYPES
// ============================================================================

// Consensus nodes are identified by AccountId.
ConsensusNetwork implements Network<ConsensusNode> {
}

// Mirror nodes are identified by host:port (gRPC) or base URL (REST).
MirrorNetwork implements Network<MirrorNode> {
}

// Block nodes are identified by host:port.
BlockNode {
    @@immutable host: string
    @@immutable port: uint16
}

BlockNodeNetwork implements Network<BlockNode> {
}

// ============================================================================
// CONSENSUS NODE REQUEST BASES (gRPC unary only)
// ============================================================================

// Shared base for all consensus node requests.
// Provides nodeAccountIds and getNetwork() -> client.consensusNetwork.
abstraction ConsensusRequest extends Request {
    nodeAccountIds: list<AccountId>
}

// ── Transaction Builder ──────────────────────────────────────────────────

// Mutable builder for transaction parameters.
// Class chain: Transaction -> ConsensusRequest -> Request
// Inherits retry/timeout config from Request and nodeAccountIds from ConsensusRequest.
// Does NOT implement Executable — users must call pack() to get an executable form.
abstraction Transaction extends ConsensusRequest {
    @@nullable maxTransactionFee: Hbar
    @@nullable validDuration: duration
    @@nullable memo: string

    @@async
    PackedTransaction<$$Data> pack(client: HieroClient)
}

// Mutable builder for transactions that split large payloads into chunks.
abstraction ChunkedTransaction extends Transaction {
    @@async
    PackedChunkedTransaction<$$Data> pack(client: HieroClient)
}

// ── Packed Transactions ─────────────────────────────────────────────────

// Immutable, signed, executable transaction. Produced by Transaction.pack().
// Class chain: PackedTransaction -> ConsensusRequest -> Request
// Contract: implements Executable<TransactionResponse>
// Parameterized by $$Data (the per-type transaction data provided by TransactionSupport).
abstraction PackedTransaction<$$Data> extends ConsensusRequest, Executable<TransactionResponse> {
    @@immutable transactionId: TransactionId

    Transaction unpack()

    void sign(keyPair: KeyPair)
    void sign(publicKey: PublicKey, signer: TransactionSigner)

    bytes toBytes()
}

// Packed form of chunked transactions. Handles multi-chunk signing and execution.
abstraction PackedChunkedTransaction<$$Data> extends PackedTransaction<$$Data> {
}

// All consensus queries. Query payment, cost estimation.
// Class chain: ConsensusQuery -> ConsensusRequest -> Request
// Contract: implements Executable<$$Response>
abstraction ConsensusQuery<$$Response> extends ConsensusRequest, Executable<$$Response> {
    @@nullable queryPayment: Hbar

    @@async
    @@throws(network-error, request-timeout, max-attempts-exceeded)
    Hbar getCost(client: HieroClient)
}

// ============================================================================
// MIRROR NODE REQUEST BASES
// ============================================================================

// Shared base for all mirror node gRPC requests (unary AND streaming).
// Provides getNetwork() -> client.mirrorNetwork and shared gRPC channel selection.
// Exists because both MirrorGrpcQuery and TopicMessageQuery share mirror gRPC plumbing.
abstraction MirrorGrpcRequest extends Request {
}

// Unary mirror gRPC queries.
// Class chain: MirrorGrpcQuery -> MirrorGrpcRequest -> Request
// Contract: implements Executable<$$Response>
abstraction MirrorGrpcQuery<$$Response> extends MirrorGrpcRequest, Executable<$$Response> {
}

// Mirror node queries using REST transport.
// Class chain: MirrorRestQuery -> Request (no intermediate base; no streaming REST variant)
// Contract: implements Executable<$$Response>
abstraction MirrorRestQuery<$$Response> extends Request, Executable<$$Response> {
}

// ============================================================================
// BLOCK NODE REQUEST BASES
// ============================================================================

// Shared base for all block node gRPC requests (unary AND streaming).
// Provides getNetwork() -> client.blockNodeNetwork and shared gRPC channel selection.
// Exists because both BlockNodeQuery and BlockStreamQuery share block node gRPC plumbing.
abstraction BlockNodeRequest extends Request {
}

// Unary block node queries.
// Class chain: BlockNodeQuery -> BlockNodeRequest -> Request
// Contract: implements Executable<$$Response>
abstraction BlockNodeQuery<$$Response> extends BlockNodeRequest, Executable<$$Response> {
}

// ============================================================================
// STREAMING TYPES
// ============================================================================

// Subscribe to a stream of topic messages from a mirror node.
// Class chain: TopicMessageQuery -> MirrorGrpcRequest -> Request
// Contract: implements Subscribable<TopicMessage>
@@finalType
TopicMessageQuery extends MirrorGrpcRequest, Subscribable<TopicMessage> {
    topicId: TopicId
    @@nullable startTime: zonedDateTime
    @@nullable endTime: zonedDateTime
    @@nullable limit: uint64
}

// Subscribe to a stream of blocks from a block node. See HIP-1081.
// Class chain: BlockStreamQuery -> BlockNodeRequest -> Request
// Contract: implements Subscribable<Block>
@@finalType
BlockStreamQuery extends BlockNodeRequest, Subscribable<Block> {
    @@default(0) startBlockNumber: uint64
    @@nullable endBlockNumber: uint64
}

// ============================================================================
// CONCRETE CONSENSUS TRANSACTION TYPES (representative subset)
//
// Each concrete type is a mutable builder (extends Transaction).
// Packing produces PackedTransaction<XxxTransactionData> (or
// PackedChunkedTransaction<XxxTransactionData> for chunked types).
// Languages with type aliases may expose e.g.:
//   type PackedAccountCreateTransaction = PackedTransaction<AccountCreateTransactionData>
// ============================================================================

@@finalType AccountCreateTransaction extends Transaction { }
@@finalType AccountUpdateTransaction extends Transaction { }
@@finalType AccountDeleteTransaction extends Transaction { }
@@finalType TransferTransaction extends Transaction { }
@@finalType TokenCreateTransaction extends Transaction { }
@@finalType TokenMintTransaction extends Transaction { }
@@finalType TokenBurnTransaction extends Transaction { }
@@finalType TokenAssociateTransaction extends Transaction { }
@@finalType TokenAirdropTransaction extends Transaction { }
@@finalType TopicCreateTransaction extends Transaction { }
@@finalType TopicMessageSubmitTransaction extends ChunkedTransaction { }
@@finalType FileCreateTransaction extends Transaction { }
@@finalType FileAppendTransaction extends ChunkedTransaction { }
@@finalType ContractCreateTransaction extends Transaction { }
@@finalType ContractExecuteTransaction extends Transaction { }
@@finalType ScheduleCreateTransaction extends Transaction { }
@@finalType NodeCreateTransaction extends Transaction { }
@@finalType EthereumTransaction extends Transaction { }
@@finalType PrngTransaction extends Transaction { }

// ============================================================================
// CONCRETE CONSENSUS QUERY TYPES (representative subset)
// ============================================================================

@@finalType AccountInfoQuery extends ConsensusQuery<AccountInfo> { }
@@finalType AccountBalanceQuery extends ConsensusQuery<AccountBalance> { }
@@finalType ContractCallQuery extends ConsensusQuery<ContractFunctionResult> { }
@@finalType ContractInfoQuery extends ConsensusQuery<ContractInfo> { }
@@finalType FileInfoQuery extends ConsensusQuery<FileInfo> { }
@@finalType FileContentsQuery extends ConsensusQuery<bytes> { }
@@finalType TopicInfoQuery extends ConsensusQuery<TopicInfo> { }
@@finalType TokenInfoQuery extends ConsensusQuery<TokenInfo> { }
@@finalType TokenNftInfoQuery extends ConsensusQuery<TokenNftInfo> { }
@@finalType ScheduleInfoQuery extends ConsensusQuery<ScheduleInfo> { }
@@finalType TransactionReceiptQuery extends ConsensusQuery<TransactionReceipt> { }
@@finalType TransactionRecordQuery extends ConsensusQuery<TransactionRecord> { }
@@finalType NetworkVersionInfoQuery extends ConsensusQuery<NetworkVersionInfo> { }

// ============================================================================
// CONCRETE MIRROR NODE QUERY TYPES
// ============================================================================

@@finalType AddressBookQuery extends MirrorGrpcQuery<NodeAddressBook> { }
@@finalType MirrorNodeContractCallQuery extends MirrorRestQuery<string> { }
@@finalType MirrorNodeContractEstimateGasQuery extends MirrorRestQuery<int64> { }

// ============================================================================
// CONCRETE BLOCK NODE QUERY TYPES
// ============================================================================

@@finalType ServerStatusQuery extends BlockNodeQuery<ServerStatus> { }
@@finalType BlockQuery extends BlockNodeQuery<BlockResult> { }
```

## Hierarchy Diagram

`──>` = class inheritance (single chain to `Request`). `··>` = implements contract (interface).

### Contracts (interfaces — no state, no inheritance relationship to Request)

```
Network<Node>          Executable<Response>              Subscribable<Item>
  ··> ConsensusNetwork   ··> PackedTransaction<Data>       ··> TopicMessageQuery
  ··> MirrorNetwork      ··> ConsensusQuery                ··> BlockStreamQuery
  ··> BlockNodeNetwork   ··> MirrorGrpcQuery
                         ··> MirrorRestQuery
                         ··> BlockNodeQuery
```

### Class Inheritance Tree (single-inheritance, rooted at Request)

```
Request
├── ConsensusRequest                                       [+ nodeAccountIds]
│   ├── Transaction                                        [+ maxTransactionFee, memo, validDuration]
│   │   ├── ChunkedTransaction                             (does NOT implement Executable)
│   │   │   ├── FileAppendTransaction
│   │   │   └── TopicMessageSubmitTransaction
│   │   ├── AccountCreateTransaction
│   │   ├── TransferTransaction
│   │   ├── TokenCreateTransaction
│   │   └── ... (50+ more)
│   ├── PackedTransaction<Data> : Executable               [+ transactionId, sign(), toBytes()]
│   │   └── PackedChunkedTransaction<Data>                 [+ chunking logic]
│   └── ConsensusQuery<R> : Executable                     [+ queryPayment, getCost()]
│       ├── AccountInfoQuery
│       ├── AccountBalanceQuery
│       ├── TransactionReceiptQuery
│       └── ... (17+ more)
│
├── MirrorGrpcRequest
│   ├── MirrorGrpcQuery<R> : Executable
│   │   └── AddressBookQuery
│   └── TopicMessageQuery : Subscribable
│
├── MirrorRestQuery<R> : Executable
│   ├── MirrorNodeContractCallQuery
│   └── MirrorNodeContractEstimateGasQuery
│
└── BlockNodeRequest
    ├── BlockNodeQuery<R> : Executable
    │   ├── ServerStatusQuery
    │   └── BlockQuery
    └── BlockStreamQuery : Subscribable
```

## Usage Example

```
// Create a mutable builder and set fields (including retry config inherited from Request)
AccountCreateTransaction tx = new AccountCreateTransaction()
tx.setKey(newAccountKey)
tx.setInitialBalance(Hbar.from(10))
tx.setMaxAttempts(5)

// Pack into an immutable, executable form (assigns transactionId, copies config)
PackedTransaction<AccountCreateTransactionData> packed = tx.pack(client)

// Sign and execute (execute() uses the shared withRetry loop from Request)
TransactionResponse response = packed
    .sign(keyPair)
    .execute(client)

// Get the receipt
TransactionReceipt receipt = response.queryReceipt(client)
```

## Questions & Comments

- Should `Request` default values for `maxAttempts`, `maxBackoff`, `minBackoff` be standardized across all SDKs, or configurable per-SDK?
- Should `MirrorRestQuery` have a separate `requestTimeout` default since REST transports have different latency characteristics than gRPC?
