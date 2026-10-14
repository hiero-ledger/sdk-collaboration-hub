# Requests SPI API

This section defines the internal SPI (Service Provider Interface) for the request hierarchy. These are not public API — they are the internal extension points that each request base and concrete type must implement.

## Description

The [Requests Core Types](requests-core.md) define the public base abstractions and configuration types. This document defines the internal mechanics:

1. **Node and network types** — `ConsensusNode`, `MirrorNode`, `BlockNode`, `Network<$$Node>`, and concrete network implementations. These are not part of the public API.
2. **Execution helpers on `Request`** — `withRetry` drives the shared retry/backoff loop. `executeImpl` and `subscribeImpl` are protected helpers that wire the retry loop to the execution SPI, called by `execute()` and `subscribe()` on the concrete bases.
3. **SPI methods across the hierarchy** — Each layer of the hierarchy carries its own internal SPI methods:
   - **Concrete bases** (`ConsensusCall`, `MirrorCall`, etc.) → `getNetwork(client)` for network resolution
   - **Concrete types** → `send()`, `mapResponse()` (for unary); `openStream()` (for streaming)
   - **Transport interface** → `buildRequest(node)` to build the transport-specific request, `isRetryable(error)` with transport-specific retry defaults

### Design rationale

Network and execution pattern are unified in the class hierarchy, so `execute()` and `subscribe()` are sealed concrete implementations on each base — they cannot be accidentally overridden by concrete types. Each base implements `execute()` or `subscribe()` as a one-line delegation to `executeImpl` or `subscribeImpl` on `Request`, which handles the retry loop.

`getNetwork()` is declared abstract on `Request` and implemented by each concrete base. This keeps network resolution close to the types that own it while allowing `executeImpl` and `subscribeImpl` to call it without knowing which network is involved.

`isRetryable()` is declared abstract on `Request` and satisfied by the default implementation on whichever transport interface (`GrpcTransport` or `RestTransport`) the concrete type implements. This allows `withRetry` to call `this.isRetryable()` directly, removing it as an explicit parameter to the loop.

`buildRequest()` is declared abstract on both `Request` (so `executeImpl`/`subscribeImpl` can call it) and on the transport interfaces (where the transport-specific signature and default behavior live). Each concrete type satisfies both by implementing the single method.

## SPI Schema

```
namespace requests-spi
requires requests-core, common, client

// ============================================================================
// EXECUTION HELPERS AND LOOP
// ============================================================================

// Protected methods on Request. Not part of the public API.
//
// withRetry drives retry and backoff for both unary and streaming execution.
//
// Parameters:
//   network — The Network instance for node selection and health tracking.
//             Obtained from getNetwork(client) on the concrete base.
//   action  — A callback that performs the actual work for a single attempt.
//             For unary: builds the request, sends it, returns the response.
//             For streaming: opens the stream and delivers items.
//
// Retry logic calls this.isRetryable(error), which is satisfied by the
// default implementation on the concrete type's transport interface.
//
// The loop:
//   1. Selects a node from the network.
//   2. Invokes the action with that node.
//   3. On success: marks the node healthy, returns the result.
//   4. On retryable failure: marks the node unhealthy, backs off, retries.
//   5. On non-retryable failure: rethrows.
//   6. On exceeding maxAttempts: throws max-attempts-exceeded.
//
// executeImpl is called by execute() on ConsensusCall, MirrorCall, BlockNodeCall.
// The sender callback combines buildRequest + send + mapResponse for one attempt.
//
// subscribeImpl is called by subscribe() on MirrorStream, BlockNodeStream.
// The streamer callback combines buildRequest + openStream for one attempt.
abstraction Request {
    protected $$Result withRetry(
        network: Network<$$Node>,
        action: @@throws(network-error) function<$$Result attempt(node: $$Node)>
    )

    protected $$R executeImpl(
        client: HieroClient,
        sender: function<$$R(node: $$Node, request: $$TransportRequest)>
    )
    // Implementation:
    //   network = this.getNetwork(client)
    //   return this.withRetry(network, (node) -> {
    //       req = this.buildRequest(node)
    //       return sender(node, req)
    //   })

    protected subscribeImpl(
        client: HieroClient,
        streamer: function<void(node: $$Node, request: $$TransportRequest)>
    )
    // Implementation:
    //   network = this.getNetwork(client)
    //   this.withRetry(network, (node) -> {
    //       req = this.buildRequest(node)
    //       streamer(node, req)
    //   })

    // Abstract SPI methods implemented across the hierarchy:
    protected abstract Network<$$Node> getNetwork(client: HieroClient)
    protected abstract $$TransportRequest buildRequest(node: $$Node)
    protected abstract bool isRetryable(error: $$Error)
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

// getNetwork() is declared abstract on Request and implemented by each concrete base.

abstraction ConsensusCall<$$Response> {
    protected ConsensusNetwork getNetwork(client: HieroClient)
    // Implementation: return client.consensusNetwork
}

abstraction MirrorCall<$$Response> {
    protected MirrorNetwork getNetwork(client: HieroClient)
    // Implementation: return client.mirrorNetwork
}

abstraction MirrorStream<$$Item> {
    protected MirrorNetwork getNetwork(client: HieroClient)
    // Implementation: return client.mirrorNetwork
}

abstraction BlockNodeCall<$$Response> {
    protected BlockNodeNetwork getNetwork(client: HieroClient)
    // Implementation: return client.blockNodeNetwork
}

abstraction BlockNodeStream<$$Item> {
    protected BlockNodeNetwork getNetwork(client: HieroClient)
    // Implementation: return client.blockNodeNetwork
}


// ============================================================================
// AXIS 2 SPI: EXECUTION — send(), mapResponse(), openStream()
// ============================================================================

// Each unary concrete type (Transaction, ConsensusQuery, AddressBookQuery, etc.)
// must implement these protected methods. They are passed as a callback to
// executeImpl() from execute():
//
//   execute(client):
//     return executeImpl(client, (node, req) -> mapResponse(send(node, req)))
//
// Note: buildRequest() is on the transport interface (Axis 3), not here.

// For each type extending ConsensusCall, MirrorCall, or BlockNodeCall:
//
//   @@throws(network-error)
//   protected $$RawResponse send(node: $$Node, request: $$TransportRequest)
//
//   protected $$Response mapResponse(response: $$RawResponse)


// Each streaming concrete type (TopicMessageQuery, BlockStreamQuery) must
// implement this protected method. It is passed as a callback to
// subscribeImpl() from subscribe():
//
//   subscribe(client):
//     subscribeImpl(client, (node, req) -> openStream(node, req))

// For each type extending MirrorStream or BlockNodeStream:
//
//   @@throws(network-error)
//   protected void openStream(node: $$Node, request: $$TransportRequest)


// ============================================================================
// AXIS 3 SPI: TRANSPORT — GrpcTransport / RestTransport
// ============================================================================

// Transport interfaces own request building and retry logic.
// buildRequest() depends on transport (proto for gRPC, HTTP for REST),
// not on execution pattern (unary vs streaming).

interface GrpcTransport {
    // Build the protobuf request for the given node.
    protected $$ProtoRequest buildRequest(node: $$Node)

    // Default behavior: retry on gRPC status UNAVAILABLE, RESOURCE_EXHAUSTED.
    // Concrete types may override for additional retryable statuses
    // (e.g. consensus BUSY, PLATFORM_TRANSACTION_NOT_CREATED).
    bool isRetryable(error: $$Error)
}

interface RestTransport {
    // Build the HTTP request (URL, headers, body) for the given node.
    protected $$HttpRequest buildRequest(node: $$Node)

    // Default behavior: retry on HTTP status 503 (Service Unavailable),
    // 429 (Too Many Requests), 408 (Request Timeout).
    bool isRetryable(error: $$Error)
}


// ============================================================================
// EXECUTION FLOW — how the hierarchy fits together
// ============================================================================

// Unary execution (e.g. Transaction.execute, AccountInfoQuery.execute):
//
//   execute(client):
//     return executeImpl(client, (node, req) -> mapResponse(send(node, req)))
//
//   executeImpl(client, sender):                          // from Request
//     network = this.getNetwork(client)                   // Axis 1: concrete base
//     return this.withRetry(network, (node) -> {          // from Request
//       req = this.buildRequest(node)                      // Axis 3: GrpcTransport/RestTransport
//       return sender(node, req)                           // Axis 2: send + mapResponse
//     })
//
// Streaming execution (e.g. TopicMessageQuery.subscribe):
//
//   subscribe(client):
//     subscribeImpl(client, (node, req) -> openStream(node, req))
//
//   subscribeImpl(client, streamer):                      // from Request
//     network = this.getNetwork(client)                   // Axis 1: concrete base
//     this.withRetry(network, (node) -> {                 // from Request
//       req = this.buildRequest(node)                      // Axis 3: GrpcTransport
//       streamer(node, req)                                // Axis 2: openStream
//     })
//
// In both cases, withRetry handles:
//   - Node selection via network.selectNode()
//   - Health tracking via network.markHealthy() / markUnhealthy()
//   - Backoff via exponential backoff between minBackoff and maxBackoff
//   - Retry gating via this.isRetryable(error) from the transport interface
//   - Attempt limiting via maxAttempts


```

## withRetry Pseudocode

The complete `withRetry` logic, expanded for clarity:

```
Request.withRetry(network, action):
    lastError = null

    for attempt in 1..this.maxAttempts:
        node = network.selectNode()

        try:
            result = action(node)
            network.markHealthy(node)
            return result

        catch error:
            lastError = error

            if not this.isRetryable(error):
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
