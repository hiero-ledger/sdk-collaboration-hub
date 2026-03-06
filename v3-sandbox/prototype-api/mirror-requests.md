# Mirror Node Requests API

This section defines request types that target mirror nodes — both gRPC and REST queries, as well as gRPC streaming subscriptions.

## Description

Mirror node requests all extend `MirrorRequest` (from [requests-core.md](requests-core.md)) for shared mirror network resolution, and declare their execution pattern and transport via the 3-axis model:

- **`AddressBookQuery`** — Unary gRPC query. Extends `MirrorRequest`, implements `Executable` and `GrpcRequest`.
- **`MirrorNodeContractCallQuery`** / **`MirrorNodeContractEstimateGasQuery`** — REST queries. Extend `MirrorRequest`, implement `Executable` and `RestRequest`.
- **`TopicMessageQuery`** — Streaming gRPC subscription. Extends `MirrorRequest`, implements `Subscribable` and `GrpcRequest`.

For the overall request hierarchy, see [requests.md](requests.md). For the internal execution flow, see [requests-spi.md](requests-spi.md).

## API Schema

```
namespace mirror-requests
requires requests-core, common

// ============================================================================
// MIRROR gRPC QUERIES
// ============================================================================

@@finalType
AddressBookQuery extends MirrorRequest, Executable<NodeAddressBook>, GrpcRequest { }

// ============================================================================
// MIRROR REST QUERIES
// ============================================================================

@@finalType
MirrorNodeContractCallQuery extends MirrorRequest, Executable<string>, RestRequest { }

@@finalType
MirrorNodeContractEstimateGasQuery extends MirrorRequest, Executable<int64>, RestRequest { }

// ============================================================================
// MIRROR gRPC STREAMING
// ============================================================================

@@finalType
TopicMessageQuery extends MirrorRequest, Subscribable<TopicMessage>, GrpcRequest {
    topicId: TopicId
    @@nullable startTime: zonedDateTime
    @@nullable endTime: zonedDateTime
    @@nullable limit: uint64
}
```

## Usage Example

```
// Unary mirror query (gRPC)
AddressBookQuery query = new AddressBookQuery()
NodeAddressBook book = query.execute(client)

// REST mirror query
MirrorNodeContractCallQuery callQuery = new MirrorNodeContractCallQuery()
string result = callQuery.execute(client)

// Streaming subscription (gRPC)
TopicMessageQuery subscription = new TopicMessageQuery()
subscription.setTopicId(topicId)
subscription.setStartTime(startTime)

SubscriptionHandle handle = subscription.subscribe(client, (message) -> {
    // process each TopicMessage
})

// Later, to stop receiving messages:
handle.unsubscribe()
```

## Questions & Comments

- Should `TopicMessageQuery` support resuming from the last-received message timestamp on reconnection?
