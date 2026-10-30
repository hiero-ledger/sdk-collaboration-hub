# Mirror Node Query API

## Description

This API defines the types and repository abstractions for querying a Hiero Mirror Node via its REST API.
It covers accounts, tokens, NFTs, transactions, topics, contracts, and network-level data.

## API Schema

```
namespace mirrornode
requires common, keys, mirrornode.contract, mirrornode.network

MirrorNodeClient {
    @@immutable contracts: ContractRepository
    @@immutable network: NetworkRepository
}

@@throws(mirror-node-error)
MirrorNodeClient createMirrorNodeClient(mirrorNode: common.MirrorNode)
```

## Example

```
mirrorNode = MirrorNode(restBaseUrl: "https://mainnet.mirrornode.hedera.com/api/v1")
client = await createMirrorNodeClient(mirrorNode)

// Look up an contract
contract = await client.contracts.findById(fromString("0.0.1234"))
```