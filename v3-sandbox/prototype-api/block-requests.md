# Block Node Requests API

This section defines request types that target block nodes — both gRPC queries and gRPC streaming subscriptions.

## Description

Block node requests all extend `BlockNodeRequest` (from [requests-core.md](requests-core.md)) for shared block node network resolution, and declare their execution pattern and transport via the 3-axis model:

- **`ServerStatusQuery`** / **`BlockQuery`** — Unary gRPC queries. Extend `BlockNodeRequest`, implement `Executable` and `GrpcRequest`.
- **`BlockStreamQuery`** — Streaming gRPC subscription. Extends `BlockNodeRequest`, implements `Subscribable` and `GrpcRequest`. See [HIP-1081](../../proposals/hips/hip-1081.md) for the block streaming design.

For the overall request hierarchy, see [requests.md](requests.md). For the internal execution flow, see [requests-spi.md](requests-spi.md).

## API Schema

```
namespace block-requests
requires requests-core, common

// ============================================================================
// BLOCK NODE QUERIES
// ============================================================================

@@finalType
ServerStatusQuery extends BlockNodeRequest, Executable<ServerStatus>, GrpcRequest { }

@@finalType
BlockQuery extends BlockNodeRequest, Executable<BlockResult>, GrpcRequest { }

// ============================================================================
// BLOCK NODE STREAMING
// ============================================================================

@@finalType
BlockStreamQuery extends BlockNodeRequest, Subscribable<Block>, GrpcRequest {
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
BlockStreamQuery streamQuery = new BlockStreamQuery()
streamQuery.setStartBlockNumber(1000)

SubscriptionHandle handle = streamQuery.subscribe(client, (block) -> {
    // process each Block
})

// Later, to stop receiving blocks:
handle.unsubscribe()
```

## Questions & Comments

- Should `BlockStreamQuery` support a `filter` parameter to receive only specific block items?
- Should `BlockNodeQuery` responses include metadata about the block node that served the request?
