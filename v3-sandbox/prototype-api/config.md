# Keys API

This section defines the API for configuration.

```
namespace config
requires common

// The full configuration to connect to a specific network
NetworkSetting {
 
    @@immutable ledger: Ledger
   
    @@immutable Set<ConsensusNode> getConsensusNodes() // set of all consensus nodes
    
    // TODO: add more configuration options like mirror node addresses
}

// factory methods of keys that should be added to the namespace in the best language dependent way

@@nullable NetworkSetting getNetworkSetting(String identifier) // returns null if no network with that identifier exists. Network settings can be added as plug and play by external modules.
```