# Requests Core Types

This document defines the foundational contracts, base abstractions, and configuration types for the request hierarchy. These are the building blocks that all domain-specific request types (transactions, queries, streaming subscriptions) extend or implement.

Every concrete request type answers two questions via its type declaration:

1. **Network + execution pattern** (class inheritance): Which network does this target, and does it return a single response or a stream? (`ConsensusCall`, `MirrorCall`, `MirrorStream`, `BlockNodeCall`, `BlockNodeStream`)
2. **Transport** (interface): What transport protocol does it use? (`GrpcTransport` or `RestTransport`)

Network and execution pattern are unified in the class hierarchy rather than split across class inheritance and interfaces. This allows each concrete base to hold a sealed `execute()` or `subscribe()` implementation that delegates to protected SPI helpers — removing the need for concrete types to re-implement execution logic.

For the overall architecture and hierarchy diagrams, see [requests.md](requests.md).

## API Schema

```
namespace requests-core
requires common, client

// ============================================================================
// TRANSPORT INTERFACES
// ============================================================================

// Marks a request as using gRPC transport.
// Carries transport-specific SPI methods (see requests-spi.md):
//   buildRequest(node) — builds the protobuf request
//   isRetryable(error) — default: retry on gRPC UNAVAILABLE, RESOURCE_EXHAUSTED
interface GrpcTransport {
}

// Marks a request as using REST/HTTP transport.
// Carries transport-specific SPI methods (see requests-spi.md):
//   buildRequest(node) — builds the HTTP request (URL, headers, body)
//   isRetryable(error) — default: retry on HTTP 503, 429, 408
interface RestTransport {
}

// ============================================================================
// RETRY AND TIMEOUT CONFIGURATION
// ============================================================================

// Retry and timeout configuration.
// Composed by Request and HieroClient (defaults).
// Fields are surfaced directly on composing types (flattened access, not nested).
//
// Priority chain: per-request config > client defaults > hardcoded defaults.
// When execute()/subscribe() is called, any unset fields on the request are
// filled from client.defaultRetryPolicy, then from @@default values here.
//
// Timeout semantics:
//
//   attemptTimeout — Max time for a SINGLE network call (one attempt).
//     gRPC: the deadline for a single unary RPC or initial connection timeout for streaming.
//     REST: the HTTP request timeout for a single call (connect + read).
//     If this fires, the attempt fails and withRetry can try the next node.
//
//   requestTimeout — Max time for the ENTIRE operation across ALL attempts.
//     gRPC and REST: same semantics — wall-clock timeout covering all retries
//     and backoff delays combined. If this fires, withRetry stops immediately
//     regardless of remaining attempts.
//
//   Example: maxAttempts=3, attemptTimeout=5s, requestTimeout=30s means each
//   individual call can take up to 5s, but the whole operation (up to 3 attempts
//   with backoff) must finish within 30s.
RetryPolicy {
    @@default(10) maxAttempts: int32
    @@default(8s) maxBackoff: duration
    @@default(250ms) minBackoff: duration
    @@nullable attemptTimeout: duration
    @@nullable requestTimeout: duration
}

// ============================================================================
// ROOT BASE
// ============================================================================

// Root of all request types. Composes RetryPolicy for retry/timeout configuration.
// Fields from RetryPolicy are surfaced directly (flattened access).
// Provides the internal withRetry loop and executeImpl/subscribeImpl helpers
// used by the concrete bases (see requests-spi.md).
abstraction Request {
    retryPolicy: RetryPolicy
}

// ============================================================================
// CONSENSUS NODE BASES
// ============================================================================

// Base for all unary requests to the consensus node.
// Provides getNetwork() → client.consensusNetwork (see requests-spi.md).
// Transport is declared by each concrete subtype via GrpcTransport or RestTransport.
abstraction ConsensusCall<$$Response> extends Request {
    @@async
    @@throws(network-error, request-timeout, max-attempts-exceeded)
    $$Response execute(client: HieroClient)
}

// ============================================================================
// MIRROR NODE BASES
// ============================================================================

// Base for all unary requests to the mirror node.
// Provides getNetwork() → client.mirrorNetwork (see requests-spi.md).
// Transport is declared by each concrete subtype via GrpcTransport or RestTransport.
abstraction MirrorCall<$$Response> extends Request {
    @@async
    @@throws(network-error, request-timeout, max-attempts-exceeded)
    $$Response execute(client: HieroClient)
}

// Base for all streaming subscriptions to the mirror node.
// Provides getNetwork() → client.mirrorNetwork (see requests-spi.md).
// streamResult<$$Item> wraps each item as either a success value or a per-item error,
// allowing the stream to continue past individual item failures (e.g. deserialization errors).
// Terminal failures (connection lost, auth revoked) surface via @@throws.
abstraction MirrorStream<$$Item> extends Request {
    @@streaming
    @@throws(network-error, request-timeout, max-attempts-exceeded)
    streamResult<$$Item> subscribe(client: HieroClient)
}

// ============================================================================
// BLOCK NODE BASES
// ============================================================================

// Base for all unary requests to the block node.
// Provides getNetwork() → client.blockNodeNetwork (see requests-spi.md).
// Transport is declared by each concrete subtype via GrpcTransport or RestTransport.
abstraction BlockNodeCall<$$Response> extends Request {
    @@async
    @@throws(network-error, request-timeout, max-attempts-exceeded)
    $$Response execute(client: HieroClient)
}

// Base for all streaming subscriptions to the block node.
// Provides getNetwork() → client.blockNodeNetwork (see requests-spi.md).
abstraction BlockNodeStream<$$Item> extends Request {
    @@streaming
    @@throws(network-error, request-timeout, max-attempts-exceeded)
    streamResult<$$Item> subscribe(client: HieroClient)
}
```

## Comparison: execute() vs subscribe()

| | `execute()` bases | `subscribe()` bases |
|---|---|---|
| Result cardinality | Exactly one response | Zero or more items over time |
| Return type | `$$Response` | `streamResult<$$Item>` |
| Execution annotation | `@@async` | `@@streaming` |
| Per-item errors | Not applicable | Non-terminal — error variant of `streamResult<$$Item>` |
| Terminal errors | `@@throws` | `@@throws` — stream ends, error surfaces via language mechanism |
| Completion | Implicit — `execute()` returns | Server-driven — server closes stream when end condition reached |
| Cancellation | Not applicable (single shot) | Implicit — stop iterating; SDK releases resources |
| Concurrency model | Future / promise / coroutine | Async iteration / reactive stream / callbacks |

## Questions & Comments

- Should `RetryPolicy` default values for `maxAttempts`, `maxBackoff`, `minBackoff` be standardized across all SDKs, or configurable per-SDK?
- Should `MirrorStream` or `BlockNodeStream` carry any retry/reconnect annotations, or is reconnect-on-disconnect purely an internal SPI concern (i.e., transparent to the consumer unless all reconnect attempts fail)?
