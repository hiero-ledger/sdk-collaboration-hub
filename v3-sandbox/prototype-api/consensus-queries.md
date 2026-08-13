# Consensus Queries API

This section defines the consensus query types — queries sent to consensus nodes over gRPC.

## Description

All consensus queries extend `ConsensusQuery`, which provides query payment handling and cost estimation. `ConsensusQuery` extends `ConsensusCall` (network + execution) and implements `GrpcTransport` (transport). It inherits retry/timeout config from `Request` and holds `nodeAccountIds` for targeting specific consensus nodes at query time.

For the overall request hierarchy, see [requests.md](requests.md). For the internal execution flow, see [requests-spi.md](requests-spi.md).

## API Schema

```
namespace consensus-queries
requires requests-core, common

// All consensus queries. Query payment, cost estimation, and node targeting.
// Class chain: ConsensusQuery -> ConsensusCall -> Request
// Transport: implements GrpcTransport
abstraction ConsensusQuery<$$Response> extends ConsensusCall<$$Response> : GrpcTransport {
    // Allows the user to explicitly target specific consensus nodes.
    // When set, the withRetry loop selects from this list instead of the full network.
    // Corresponds to the node_account_id field in the protobuf QueryHeader.
    nodeAccountIds: list<AccountId>

    @@nullable queryPayment: Hbar

    @@async
    @@throws(network-error, request-timeout, max-attempts-exceeded)
    Hbar getCost(client: HieroClient)
}

// ============================================================================
// CONCRETE CONSENSUS QUERY TYPES (representative subset)
// ============================================================================

@@finalType AccountInfoQuery extends ConsensusQuery<AccountInfo> { }
@@finalType AccountBalanceQuery extends ConsensusQuery<AccountBalance> { }
@@finalType ContractCallQuery extends ConsensusQuery<ContractFunctionResult> { }
@@finalType ContractInfoQuery extends ConsensusQuery<ContractInfo> { }
@@finalType FileInfoQuery extends ConsensusQuery<FileInfo> { }
@@finalType FileContentsQuery extends ConsensusQuery<bytes> { }
@@finalType TopicInfoQuery extends ConsensusQuery<TopicInfo> { }
@@finalType TokenInfoQuery extends ConsensusQuery<TokenInfo> { }
@@finalType TokenNftInfoQuery extends ConsensusQuery<TokenNftInfo> { }
@@finalType ScheduleInfoQuery extends ConsensusQuery<ScheduleInfo> { }
@@finalType TransactionReceiptQuery extends ConsensusQuery<TransactionReceipt> { }
@@finalType TransactionRecordQuery extends ConsensusQuery<TransactionRecord> { }
@@finalType NetworkVersionInfoQuery extends ConsensusQuery<NetworkVersionInfo> { }
```

## Usage Example

```
AccountBalanceQuery query = new AccountBalanceQuery()
query.setAccountId(accountId)
query.setMaxAttempts(3)

// Optionally estimate cost first
Hbar cost = query.getCost(client)

// Execute the query
AccountBalance balance = query.execute(client)
```

## Questions & Comments

- Should `ConsensusQuery` provide a default `queryPayment` or always require the user to set one (or call `getCost`)?
- Should `nodeAccountIds` have a default (e.g., all known nodes) or must it always be explicitly set?
- Should `TransactionReceiptQuery` and `TransactionRecordQuery` have specialized retry behavior (e.g., waiting for consensus)?
