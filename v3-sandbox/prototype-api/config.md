# Configuration API

This section defines the API for configuration.

## Description

The config API provides functions to define and retrieve the configuration of a specific network.

## API Schema

```
namespace config
requires common

constant HEDERA_MAINNET_IDENTIFIER:string = "hedera-mainnet" // identifier for the Hedera mainnet
constant HEDERA_TESTNET_IDENTIFIER:string = "hedera-testnet" // identifier for the Hedera testnet

// The full configuration to connect to a specific network
NetworkSetting {
 
    @@immutable ledger: common.Ledger // the definition of the ledger
   
    // Returns an immutable set of consensus nodes
    // Modifications to the returned set do not affect the original
    @@immutable set<common.ConsensusNode> getConsensusNodes()

    // Returns an immutable set of mirror nodes
    // Modifications to the returned set do not affect the original
    @@immutable set<common.MirrorNode> getMirrorNodes()

}

// factory methods of `NetworkSetting` that should be added to the namespace in the best language dependent way

// Method to register a network configuration
void registerNetworkSetting(identifier: string, setting: NetworkSetting)

// throws not-found-error if no network with that identifier exists
// Network settings can be added as plug and play by external modules
@@throws(not-found-error) NetworkSetting getNetworkSetting(identifier: string) 
```

## Examples

The following example shows how to load the network configuration for the Hedera testnet:

```
NetworkSetting setting = NetworkSetting.getNetworkSetting(HEDERA_TESTNET_IDENTIFIER)
```

## Questions & Comments

- [@hendrikebbers](https://github.com/hendrikebbers): Do we want to have mirror node information in the configuration at
  all or should v3 do a concrete split between mirror node and consensus node?

  **Resolved:** Keep mirror node information in `NetworkSetting` (Option A). The separation of concerns between
  consensus and mirror communication is already enforced at the request layer — `ConsensusCall` targets
  consensus nodes and `MirrorCall` targets mirror nodes. Duplicating that split at the configuration layer
  would add overhead without a clear benefit: applications load a complete network preset in a single
  `getNetworkSetting()` call, and splitting the type would force callers to manage two separate config
  objects. The unified `NetworkSetting` also maps naturally to the single `network` field on `HieroClient`
  in `client.md`.
