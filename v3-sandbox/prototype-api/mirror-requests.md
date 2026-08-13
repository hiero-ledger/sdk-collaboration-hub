# Mirror Node Requests API

This section defines request types that target mirror nodes — both gRPC and REST queries, as well as gRPC streaming subscriptions.

## Description

Mirror node requests extend the appropriate mirror base from [requests-core.md](requests-core.md) and declare their transport via the interface they implement:

- **`AddressBookQuery`** — Unary gRPC query. Extends `MirrorCall`, implements `GrpcTransport`.
- **`MirrorNodeContractCallQuery`** / **`MirrorNodeContractEstimateGasQuery`** — REST queries. Extend `MirrorCall`, implement `RestTransport`.
- **`TopicMessageQuery`** — Streaming gRPC subscription. Extends `MirrorStream`, implements `GrpcTransport`.

For the overall request hierarchy, see [requests.md](requests.md). For the internal execution flow, see [requests-spi.md](requests-spi.md).

## API Schema

```
namespace mirror-requests
requires requests-core, common

// ============================================================================
// MIRROR gRPC QUERIES
// ============================================================================

@@finalType
AddressBookQuery extends MirrorCall<NodeAddressBook> : GrpcTransport { }

// ============================================================================
// MIRROR REST QUERIES
// ============================================================================

@@finalType
MirrorNodeContractCallQuery extends MirrorCall<string> : RestTransport { }

@@finalType
MirrorNodeContractEstimateGasQuery extends MirrorCall<int64> : RestTransport { }

// ============================================================================
// MIRROR gRPC STREAMING
// ============================================================================

@@finalType
TopicMessageQuery extends MirrorStream<TopicMessage> : GrpcTransport {
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
// The concrete subscribe() signature is language-specific.
// This pseudocode shows the semantic intent:
TopicMessageQuery subscription = new TopicMessageQuery()
subscription.setTopicId(topicId)
subscription.setStartTime(startTime)
subscription.subscribe(client)  // delivers TopicMessage items; see language guide for concrete API
```

## Questions & Comments

- Should `TopicMessageQuery` support resuming from the last-received message timestamp on reconnection?
