# Requests SPI API

This section defines the internal SPI (Service Provider Interface) for the request hierarchy. These are not public API — they are the internal extension points that each request base and concrete type must implement.

## Description

The [Requests API](requests.md) defines the public types and contracts. This document defines the internal mechanics:

1. **`withRetry` execution loop** — The shared retry/backoff loop on `Request` that drives both unary and streaming execution.
2. **SPI methods** — Abstract methods that each network-specific base and concrete type override to provide request-specific behavior.
3. **`getNetwork()` method** — Defined on each network-specific base, returns the appropriate `Network` from the client. Passed as a parameter to `withRetry`.

### Design rationale

The retry loop for unary requests (send → receive → retry on failure) and streaming subscriptions (open stream → deliver items → reconnect on failure) are structurally identical. Rather than duplicating the loop or introducing a separate `StreamingHandler` utility, a single `withRetry` method handles both patterns. The only difference is the `action` callback: unary types send a single request, streaming types open a long-lived connection.

`Request` does **not** define `getNetwork()` — it remains network-agnostic. Each network-specific base (`ConsensusRequest`, `MirrorGrpcRequest`, `BlockNodeRequest`, `MirrorRestQuery`) defines `getNetwork(client)` and passes the result to `withRetry`. This keeps `Request` free of network concerns and avoids forcing all request types through a single `getNetwork()` signature.

## SPI Schema

```
namespace requests-spi
requires requests, common, client

// ============================================================================
// SHARED EXECUTION LOOP
// ============================================================================

// Protected method on Request. Not part of the public API.
// Drives retry and backoff for both unary and streaming execution.
//
// Parameters:
//   network — The Network instance for node selection and health tracking.
//             Obtained from getNetwork(client) on the caller's network-specific base.
//   action  — A callback that performs the actual work for a single attempt.
//             For unary: builds request, sends it, returns the response.
//             For streaming: opens the stream and delivers items.
//
// The loop:
//   1. Selects a node from the network.
//   2. Invokes the action with that node.
//   3. On success: marks the node healthy, returns the result.
//   4. On retryable failure: marks the node unhealthy, backs off, retries.
//   5. On non-retryable failure: rethrows.
//   6. On exceeding maxAttempts: throws max-attempts-exceeded.
abstraction Request {
    protected $$Result withRetry(
        network: Network<$$Node>,
        action: @@throws(network-error) callback(node: $$Node) -> $$Result,
        shouldRetry: callback(error: $$Error) -> bool
    )
}


// ============================================================================
// NETWORK RESOLUTION — getNetwork()
// ============================================================================

// Each network-specific base defines getNetwork(client) to return the
// appropriate Network instance from the client.

abstraction ConsensusRequest {
    protected ConsensusNetwork getNetwork(client: HieroClient)
    // Implementation: return client.consensusNetwork
}

abstraction MirrorGrpcRequest {
    protected MirrorNetwork getNetwork(client: HieroClient)
    // Implementation: return client.mirrorNetwork
}

abstraction MirrorRestQuery {
    protected MirrorNetwork getNetwork(client: HieroClient)
    // Implementation: return client.mirrorNetwork
}

abstraction BlockNodeRequest {
    protected BlockNodeNetwork getNetwork(client: HieroClient)
    // Implementation: return client.blockNodeNetwork
}


// ============================================================================
// REQUEST LIFECYCLE SPI METHODS — Unary (Executable)
// ============================================================================

// Unary request types (PackedTransaction, ConsensusQuery, MirrorGrpcQuery,
// MirrorRestQuery, BlockNodeQuery) override these methods.

// SPI that each unary request type implements to customize execution.
contract ExecutableSpi<$$Node, $$ProtoRequest, $$ProtoResponse, $$Response> {

    // Build the protobuf (or HTTP) request for the given node.
    $$ProtoRequest buildRequest(node: $$Node)

    // Send the request to the given node and return the raw response.
    // For gRPC types, this sends a unary RPC.
    // For REST types, this sends an HTTP request.
    @@throws(network-error)
    $$ProtoResponse send(node: $$Node, request: $$ProtoRequest)

    // Map the raw response to the SDK return type.
    $$Response mapResponse(response: $$ProtoResponse)

    // Determine if the error warrants a retry.
    // Returns true if the request should be retried.
    bool shouldRetry(error: $$Error)
}


// ============================================================================
// REQUEST LIFECYCLE SPI METHODS — Streaming (Subscribable)
// ============================================================================

// Streaming request types (TopicMessageQuery, BlockStreamQuery) override
// these methods.

// SPI that each streaming request type implements to customize subscription.
contract SubscribableSpi<$$Node, $$ProtoRequest, $$Item> {

    // Build the streaming request for the given node.
    $$ProtoRequest buildStreamRequest(node: $$Node)

    // Open a gRPC server-streaming call to the given node.
    // Items are delivered to onNext. On stream end, returns normally.
    // On stream failure, throws a network-error so withRetry can handle it.
    @@throws(network-error)
    void openStream(
        node: $$Node,
        request: $$ProtoRequest,
        onNext: void callback(item: $$Item)
    )

    // Determine if the error warrants a reconnection attempt.
    // Returns true if the stream should reconnect.
    bool shouldRetry(error: $$Error)
}


// ============================================================================
// EXECUTION FLOW — how it all fits together
// ============================================================================

// Unary execution (e.g. PackedTransaction.execute):
//
//   execute(client):
//     network = this.getNetwork(client)                     // from network-specific base
//     return this.withRetry(network, (node) -> {             // from Request
//       request = this.buildRequest(node)                     // from ExecutableSpi
//       response = this.send(node, request)                   // from ExecutableSpi
//       return this.mapResponse(response)                      // from ExecutableSpi
//     }, (error) -> this.shouldRetry(error))                  // from ExecutableSpi
//
// Streaming execution (e.g. TopicMessageQuery.subscribe):
//
//   subscribe(client, onNext):
//     network = this.getNetwork(client)                     // from network-specific base
//     return this.withRetry(network, (node) -> {             // from Request
//       request = this.buildStreamRequest(node)               // from SubscribableSpi
//       this.openStream(node, request, onNext)                // from SubscribableSpi
//     }, (error) -> this.shouldRetry(error))                  // from SubscribableSpi
//
// In both cases, withRetry handles:
//   - Node selection via network.selectNode()
//   - Health tracking via network.markHealthy() / markUnhealthy()
//   - Backoff via exponential backoff between minBackoff and maxBackoff
//   - Retry gating via shouldRetry(error)
//   - Attempt limiting via maxAttempts


// ============================================================================
// CONSENSUS-SPECIFIC SPI ADDITIONS
// ============================================================================

// ── PackedTransaction: ExecutableSpi delegating to TransactionSupport ────
//
// PackedTransaction<$$Data> is the generic immutable/executable form of all
// transactions. It implements ExecutableSpi by delegating data-layer concerns
// to TransactionSupport (defined in transactions-spi.md).
//
// TransactionSupport is the per-type data-layer SPI:
//   - getMethodDescriptor()     -> which gRPC method to call
//   - updateBody(tx, body)      -> populate TransactionBody proto from tx data
//   - convert(protoBody)        -> convert TransactionBody proto back to tx data
//   - convert(protoResponse)    -> convert proto response to SDK Response type
//
// PackedTransaction's ExecutableSpi methods delegate to TransactionSupport:
//
//   buildRequest(node):
//     support = getTransactionSupport(this.data)
//     body = support.updateBody(this.data, new TransactionBody())
//     body.transactionId = this.transactionId
//     body.nodeAccountId = node.accountId
//     signed = signWithCollectedSigners(body)
//     return wrapInTransaction(signed)
//
//   send(node, request):
//     support = getTransactionSupport(this.data)
//     method = support.getMethodDescriptor()
//     return node.invoke(method, request)
//
//   mapResponse(response):
//     support = getTransactionSupport(this.data)
//     return support.convert(response)
//
//   shouldRetry(error):
//     return isRetryableConsensusError(error)
//
// This means:
//   - PackedTransaction is ONE generic class, not per-type subclasses
//   - TransactionSupport provides the per-type specialization (proto conversion, gRPC method)
//   - Adding a new transaction type only requires a new TransactionSupport implementation
//     and a new Transaction builder subclass — no new PackedTransaction subclass is needed

// ── ConsensusQuery ──────────────────────────────────────────────────────
//
// ConsensusQuery adds query payment handling.
// The buildRequest method on ConsensusQuery handles:
//   1. Build the protobuf Query with header
//   2. Attach query payment transaction (if required)
//
// ConsensusQuery.getCost(client) is a convenience that executes a
// cost-only query (COST_ANSWER response type) using the same withRetry loop.
```

## withRetry Pseudocode

The complete `withRetry` logic, expanded for clarity:

```
Request.withRetry(network, action, shouldRetry):
    lastError = null

    for attempt in 1..this.maxAttempts:
        node = network.selectNode()

        try:
            result = action(node)
            network.markHealthy(node)
            return result

        catch error:
            lastError = error

            if not shouldRetry(error):
                throw error

            network.markUnhealthy(node)

            if attempt < this.maxAttempts:
                delay = min(
                    this.minBackoff * 2^(attempt - 1),
                    this.maxBackoff
                )
                sleep(delay)

    throw MaxAttemptsExceededError(this.maxAttempts, lastError)
```

## Questions & Comments

- Should `shouldRetry` have a default implementation on `Request` that handles common gRPC status codes (UNAVAILABLE, RESOURCE_EXHAUSTED), or must every base type provide its own?
- For streaming types, should `withRetry` track cumulative attempts across reconnections, or reset per-reconnection?
- Should the `action` callback receive the attempt number for logging/diagnostics purposes?
