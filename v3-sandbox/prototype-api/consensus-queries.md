# Consensus Queries API

This section defines the consensus query types — queries sent to consensus nodes over gRPC.

## Description

All consensus queries extend `ConsensusQuery`, which provides query payment handling and cost estimation. `ConsensusQuery` follows the 3-axis model: it extends `ConsensusRequest` (network), implements `Executable` (execution), and implements `GrpcRequest` (transport). It inherits retry/timeout config from `Request` and `nodeAccountIds` from `ConsensusRequest`.

For the overall request hierarchy, see [requests.md](requests.md). For the internal execution flow, see [requests-spi.md](requests-spi.md).

## API Schema

```
namespace consensus-queries
requires requests-core, common

// All consensus queries. Query payment, cost estimation.
// Class chain: ConsensusQuery -> ConsensusRequest -> Request
// Contracts: implements Executable<$$Response>, GrpcRequest
abstraction ConsensusQuery<$$Response> extends ConsensusRequest, Executable<$$Response>, GrpcRequest {
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
- Should `TransactionReceiptQuery` and `TransactionRecordQuery` have specialized retry behavior (e.g., waiting for consensus)?
