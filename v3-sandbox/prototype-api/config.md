# Configuration API

This section defines the API for configuration.

## Description

TODO

## API Schema

```
namespace config
requires common

// The full configuration to connect to a specific network
NetworkSetting {
 
    @@immutable ledger: Ledger // the definition of the ledger
   
    @@immutable Set<ConsensusNode> getConsensusNodes() // set of all consensus nodes
    
    @@immutable Set<MirrorNode> getMirrorNodes() // set of all mirror nodes

}

// factory methods of `NetworkSetting` that should be added to the namespace in the best language dependent way

@@nullable NetworkSetting getNetworkSetting(String identifier) // returns null if no network with that identifier exists. Network settings can be added as plug and play by external modules.
```

## Questions & Comments

- [@hendrikebbers](https://github.com/hendrikebbers): Do we want to have mirror node information in the configuration at all or should v3 do a concrete split between mirror node and consensus node?