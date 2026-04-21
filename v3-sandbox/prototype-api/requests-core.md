# Requests Core Types

This document defines the execution contracts for the request hierarchy: `Executable` for unary requests that
produce a single response, and `Subscribable` for streaming requests that produce an ongoing sequence of items.

For the overall request hierarchy and how these contracts compose with network and transport axes, see
[requests.md](requests.md) (defined in [PR #145](https://github.com/hiero-ledger/sdk-collaboration-hub/pull/145)).

## API Schema

```
namespace requests-core
requires common, client

// ============================================================================
// EXECUTION INTERFACES
// ============================================================================

// Any request that produces a single response via execute().
// Does NOT extend Request - avoids diamond inheritance.
interface Executable<$$Response> {
    @@async
    @@throws(network-error, request-timeout, max-attempts-exceeded)
    $$Response execute(client: HieroClient)
}

// Any request that produces an ongoing stream of $$Item values via subscribe().
// Does NOT extend Request - avoids diamond inheritance.
// streamResult<$$Item> wraps each item as either a success value or a per-item error,
// allowing the stream to continue past individual item failures (e.g. deserialization errors).
// Terminal failures (connection lost, auth revoked) surface via @@throws.
interface Subscribable<$$Item> {
    @@streaming
    @@throws(network-error, request-timeout, max-attempts-exceeded)
    streamResult<$$Item> subscribe(client: HieroClient)
}
```

## Comparison: Executable vs Subscribable

| | `Executable<$$Response>` | `Subscribable<$$Item>` |
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

- Should `Subscribable` carry any retry/reconnect annotations, or is reconnect-on-disconnect purely an
  internal SPI concern (i.e., transparent to the consumer unless all reconnect attempts fail)?
