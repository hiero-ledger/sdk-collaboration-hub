# Block Node Requests API

This section defines request types that target block nodes — both gRPC queries and gRPC streaming subscriptions.

## Description

Block node requests extend the appropriate block node base from [requests-core.md](requests-core.md) and declare their transport via the interface they implement:

- **`ServerStatusQuery`** / **`BlockQuery`** — Unary gRPC queries. Extend `BlockNodeCall`, implement `GrpcTransport`.
- **`BlockStreamQuery`** — Streaming gRPC subscription. Extends `BlockNodeStream`, implements `GrpcTransport`. See [HIP-1081](../../proposals/hips/hip-1081.md) for the block streaming design.

For the overall request hierarchy, see [requests.md](requests.md). For the internal execution flow, see [requests-spi.md](requests-spi.md).

## API Schema

```
namespace block-requests
requires requests-core, common

// ============================================================================
// BLOCK NODE QUERIES
// ============================================================================

@@finalType
ServerStatusQuery extends BlockNodeCall<ServerStatus> : GrpcTransport { }

@@finalType
BlockQuery extends BlockNodeCall<BlockResult> : GrpcTransport { }

// ============================================================================
// BLOCK NODE STREAMING
// ============================================================================

@@finalType
BlockStreamQuery extends BlockNodeStream<Block> : GrpcTransport {
    @@default(0) startBlockNumber: uint64
    @@nullable endBlockNumber: uint64
}
```

## Usage Example

```
// Query server status
ServerStatusQuery statusQuery = new ServerStatusQuery()
ServerStatus status = statusQuery.execute(client)

// Subscribe to block stream
// The concrete subscribe() signature is language-specific.
// This pseudocode shows the semantic intent:
BlockStreamQuery streamQuery = new BlockStreamQuery()
streamQuery.setStartBlockNumber(1000)
streamQuery.subscribe(client)  // delivers Block items; see language guide for concrete API
```

## Questions & Comments

- Should `BlockStreamQuery` support a `filter` parameter to receive only specific block items?
- Should `BlockNodeCall` responses include metadata about the block node that served the request?
