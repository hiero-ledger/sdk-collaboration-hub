# Requests SPI API

This section defines the internal SPI (Service Provider Interface) for the request hierarchy. These are not public API — they are the internal extension points that each request base and concrete type must implement.

## Description

The [Requests Core Types](requests-core.md) define the public interfaces and base abstractions. This document defines the internal mechanics:

1. **Node and network types** — `ConsensusNode`, `MirrorNode`, `BlockNode`, `Network<$$Node>`, and concrete network implementations. These are not part of the public API.
2. **`withRetry` execution loop** — The shared retry/backoff loop on `Request` that drives both unary and streaming execution.
3. **SPI methods distributed across the 3 axes** — Each of the three public axes (network base, execution interface, transport interface) carries its own internal SPI methods:
   - **Network base** → `getNetwork(client)` for network resolution
   - **Execution interface** → `send()`, `mapResponse()` (for `Executable`); `openStream()` (for `Subscribable`)
   - **Transport interface** → `buildRequest(node)` to build the transport-specific request, `shouldRetry(error)` with transport-specific retry logic (for `GrpcRequest` or `RestRequest`)

### Design rationale

The retry loop for unary requests (send → receive → retry on failure) and streaming subscriptions (open stream → deliver items → reconnect on failure) are structurally identical. Rather than duplicating the loop or introducing a separate `StreamingHandler` utility, a single `withRetry` method handles both patterns. The only difference is the `action` callback: unary types send a single request, streaming types open a long-lived connection.

`Request` does **not** define `getNetwork()` — it remains network-agnostic. Each network-specific base (`ConsensusRequest`, `MirrorRequest`, `BlockNodeRequest`) defines `getNetwork(client)` and passes the result to `withRetry`. This keeps `Request` free of network concerns and avoids forcing all request types through a single `getNetwork()` signature.

SPI methods are not grouped into separate `ExecutableSpi` / `SubscribableSpi` interfaces. Instead, they are distributed across the three axis interfaces that each request type already declares. This avoids redundant SPI interfaces and keeps each axis self-contained: the network base owns network resolution, the transport interface owns request building and retry logic, and the execution interface owns the response consumption pattern (send+map for unary, openStream for streaming).

## SPI Schema

```
namespace requests-spi
requires requests-core, common, client

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
        action: @@throws(network-error) function<$$Result attempt(node: $$Node)>,
        shouldRetry: function<bool shouldRetry(error: $$Error)>
    )
}


// ============================================================================
// NODE TYPES (internal — not part of the public API)
// ============================================================================

abstraction Node {
    @@immutable host: string
    @@immutable port: uint16
}

// Consensus nodes are identified by AccountId and connect via gRPC.
ConsensusNode extends Node {
    @@immutable accountId: common.AccountId
}

// Mirror nodes are identified by host:port. Used for both gRPC and REST.
MirrorNode extends Node {
}

// Block nodes are identified by host:port.
BlockNode extends Node {
}

// ============================================================================
// NETWORK MANAGEMENT (internal — not part of the public API)
// ============================================================================

// Node selection and health tracking for a network.
// 4 of 7 existing SDKs have a shared network base (Java BaseNetwork,
// JS ManagedNetwork, Go _ManagedNetwork, C++ BaseNetwork).
abstraction Network<$$Node> {
    $$Node selectNode()
    void markHealthy(node: $$Node)
    void markUnhealthy(node: $$Node)
}

ConsensusNetwork implements Network<ConsensusNode> { }
MirrorNetwork implements Network<MirrorNode> { }
BlockNodeNetwork implements Network<BlockNode> { }


// ============================================================================
// AXIS 1 SPI: NETWORK RESOLUTION — getNetwork()
// ============================================================================

// Each network-specific base defines getNetwork(client) to return the
// appropriate Network instance from the client.

abstraction ConsensusRequest {
    protected ConsensusNetwork getNetwork(client: HieroClient)
    // Implementation: return client.consensusNetwork
}

abstraction MirrorRequest {
    protected MirrorNetwork getNetwork(client: HieroClient)
    // Implementation: return client.mirrorNetwork
}

abstraction BlockNodeRequest {
    protected BlockNodeNetwork getNetwork(client: HieroClient)
    // Implementation: return client.blockNodeNetwork
}


// ============================================================================
// AXIS 2 SPI: EXECUTION — Executable internal methods
// ============================================================================

// Each type implementing Executable must provide these protected methods.
// They are called by the execute() implementation within the withRetry loop.
// Note: buildRequest() is on the transport interface (Axis 3), not here —
// what you build depends on the transport (proto vs HTTP), not the execution pattern.

interface Executable<$$Response> {

    // Send the request to the given node and return the raw response.
    // For gRPC types: sends a unary RPC via gRPC channel.
    // For REST types: sends an HTTP request via HTTP client.
    @@throws(network-error)
    protected $$ProtoResponse send(node: $$Node, request: $$ProtoRequest)

    // Map the raw response to the SDK return type.
    protected $$Response mapResponse(response: $$ProtoResponse)
}


// ============================================================================
// AXIS 2 SPI: EXECUTION — Subscribable internal methods
// ============================================================================

// Each type implementing Subscribable must provide an internal method to open
// the server-streaming call. Called by the subscribe() implementation within
// the withRetry loop. The item delivery mechanism is language-specific.
// Note: buildRequest() is on the transport interface (Axis 3), not here.

interface Subscribable<$$Item> {

    // Open a server-streaming call to the given node.
    // Items are delivered to the consumer. On stream end, returns normally.
    // On stream failure, throws a network-error so withRetry can handle it.
    @@throws(network-error)
    protected void openStream(
        node: $$Node,
        request: $$ProtoRequest
    )
}


// ============================================================================
// AXIS 3 SPI: TRANSPORT — GrpcRequest / RestRequest
// ============================================================================

// Transport interfaces own request building and retry logic.
// buildRequest() depends on transport (proto for gRPC, HTTP for REST),
// not on execution pattern (unary vs streaming).

interface GrpcRequest {
    // Build the protobuf request for the given node.
    protected $$ProtoRequest buildRequest(node: $$Node)

    // Default behavior: retry on gRPC status UNAVAILABLE, RESOURCE_EXHAUSTED.
    // Concrete types may override for additional retryable statuses
    // (e.g. consensus BUSY, PLATFORM_TRANSACTION_NOT_CREATED).
    bool shouldRetry(error: $$Error)
}

interface RestRequest {
    // Build the HTTP request (URL, headers, body) for the given node.
    protected $$HttpRequest buildRequest(node: $$Node)

    // Default behavior: retry on HTTP status 503 (Service Unavailable),
    // 429 (Too Many Requests), 408 (Request Timeout).
    bool shouldRetry(error: $$Error)
}


// ============================================================================
// EXECUTION FLOW — how the 3 axes fit together
// ============================================================================

// Unary execution (e.g. Transaction.execute):
//
//   execute(client):
//     network = this.getNetwork(client)                     // Axis 1: network base
//     return this.withRetry(network, (node) -> {             // from Request
//       request = this.buildRequest(node)                     // Axis 3: GrpcRequest/RestRequest
//       response = this.send(node, request)                   // Axis 2: Executable
//       return this.mapResponse(response)                      // Axis 2: Executable
//     }, (error) -> this.shouldRetry(error))                  // Axis 3: GrpcRequest/RestRequest
//
// Streaming execution (e.g. TopicMessageQuery.subscribe):
//
//   subscribe(client):
//     network = this.getNetwork(client)                     // Axis 1: network base
//     return this.withRetry(network, (node) -> {             // from Request
//       request = this.buildRequest(node)                     // Axis 3: GrpcRequest
//       this.openStream(node, request)                        // Axis 2: Subscribable
//     }, (error) -> this.shouldRetry(error))                  // Axis 3: GrpcRequest
//
// In both cases, withRetry handles:
//   - Node selection via network.selectNode()
//   - Health tracking via network.markHealthy() / markUnhealthy()
//   - Backoff via exponential backoff between minBackoff and maxBackoff
//   - Retry gating via shouldRetry(error) from the transport interface
//   - Attempt limiting via maxAttempts


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

- For streaming types, should `withRetry` track cumulative attempts across reconnections, or reset per-reconnection?
- Should the `action` callback receive the attempt number for logging/diagnostics purposes?
