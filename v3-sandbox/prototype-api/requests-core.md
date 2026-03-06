# Requests Core Types

This document defines the foundational contracts, base abstractions, and configuration types for the request hierarchy. These are the building blocks that all domain-specific request types (transactions, queries, streaming subscriptions) extend or implement.

Every concrete request type answers three orthogonal questions via its type declaration:

1. **Network** (class inheritance): Which network does this target? (`ConsensusRequest`, `MirrorRequest`, `BlockNodeRequest`)
2. **Execution** (interface): What execution pattern does it use? (`Executable<R>` or `Subscribable<I>`)
3. **Transport** (interface): What transport protocol does it use? (`GrpcRequest` or `RestRequest`)

For the overall architecture and hierarchy diagrams, see [requests.md](requests.md).

## API Schema

```
namespace requests-core
requires common, client

// ============================================================================
// EXECUTION INTERFACES
// ============================================================================

// Any request that produces a single response via execute().
// Does NOT extend Request — avoids diamond inheritance.
interface Executable<$$Response> {
    @@async
    @@throws(network-error, request-timeout, max-attempts-exceeded)
    $$Response execute(client: HieroClient)
}

// Any request that produces a stream of items via subscribe().
// Does NOT extend Request — avoids diamond inheritance.
interface Subscribable<$$Item> {
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
// TRANSPORT INTERFACES
// ============================================================================

// Marks a request as using gRPC transport.
// Carries transport-specific SPI methods (see requests-spi.md):
//   buildRequest(node) — builds the protobuf request
//   shouldRetry(error) — default: retry on gRPC UNAVAILABLE, RESOURCE_EXHAUSTED
interface GrpcRequest {
    bool shouldRetry(error: $$Error)
}

// Marks a request as using REST/HTTP transport.
// Carries transport-specific SPI methods (see requests-spi.md):
//   buildRequest(node) — builds the HTTP request (URL, headers, body)
//   shouldRetry(error) — default: retry on HTTP 503, 429, 408
interface RestRequest {
    bool shouldRetry(error: $$Error)
}

// ============================================================================
// SHARED CONFIGURATION
// ============================================================================

// Shared retry and timeout configuration.
// Composed by Request (execution hierarchy) and HieroClient (defaults).
// Fields are surfaced directly on composing types (flattened access, not nested).
//
// Priority chain: per-request config > client defaults > hardcoded defaults.
// When execute()/subscribe() is called, any unset fields on the request are
// filled from client.defaultRequestConfig, then from @@default values here.
RequestConfig {
    @@default(10) maxAttempts: int32
    @@default(8s) maxBackoff: duration
    @@default(250ms) minBackoff: duration
    @@nullable attemptTimeout: duration // max time for a single attempt (one network call); applies to both gRPC and REST
    @@nullable requestTimeout: duration // max time for the entire operation (all attempts combined)
}

// ============================================================================
// ROOT BASE
// ============================================================================

// Root of all executable request types. Composes RequestConfig for retry/timeout.
// Fields from RequestConfig are surfaced directly (flattened access).
// Also provides the internal withRetry execution loop (see requests-spi.md).
abstraction Request {
    requestConfig: RequestConfig
}

// ============================================================================
// NETWORK-SPECIFIC REQUEST BASES
// ============================================================================

// Shared base for all consensus node requests.
// Network resolution is handled internally via SPI (see requests-spi.md).
abstraction ConsensusRequest extends Request {
    nodeAccountIds: list<AccountId>
}

// Shared base for all mirror node requests (gRPC and REST, unary and streaming).
// Network resolution is handled internally via SPI (see requests-spi.md).
abstraction MirrorRequest extends Request {
}

// Shared base for all block node requests (unary and streaming).
// Network resolution is handled internally via SPI (see requests-spi.md).
abstraction BlockNodeRequest extends Request {
}
```

## Questions & Comments

- Should `ConsensusRequest.nodeAccountIds` have a default (e.g., all known nodes) or must it always be explicitly set?
