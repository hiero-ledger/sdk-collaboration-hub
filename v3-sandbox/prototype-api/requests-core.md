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

// Produces an asynchronous stream of $$Item values.
// Does NOT extend Request — avoids diamond inheritance.
// Implementations must provide item delivery, error propagation,
// completion signaling, and cancellation support.
// The return type and consumer mechanism are language-specific.
// See language best practice guides for the concrete signature.
interface Subscribable<$$Item> {
    subscribe(client: HieroClient)
}

// ============================================================================
// TRANSPORT INTERFACES
// ============================================================================

// Marks a request as using gRPC transport.
// Carries transport-specific SPI methods (see requests-spi.md):
//   buildRequest(node) — builds the protobuf request
//   shouldRetry(error) — default: retry on gRPC UNAVAILABLE, RESOURCE_EXHAUSTED
interface GrpcRequest {
}

// Marks a request as using REST/HTTP transport.
// Carries transport-specific SPI methods (see requests-spi.md):
//   buildRequest(node) — builds the HTTP request (URL, headers, body)
//   shouldRetry(error) — default: retry on HTTP 503, 429, 408
interface RestRequest {
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
RequestConfig {
    @@default(10) maxAttempts: int32
    @@default(8s) maxBackoff: duration
    @@default(250ms) minBackoff: duration
    @@nullable attemptTimeout: duration
    @@nullable requestTimeout: duration
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
    // Allows the user to explicitly target specific consensus nodes.
    // When set, the withRetry loop selects from this list instead of the full network.
    // Corresponds to the node_account_id field in the protobuf TransactionBody/QueryHeader.
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
