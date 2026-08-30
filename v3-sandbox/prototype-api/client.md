# Client API

This section defines the API for the client.

## Description

The client API provides the central entry point for all SDK operations.
A `HieroClient` holds the operator identity and the network configuration needed to build, sign, and submit
transactions, and to execute queries.

The operator is the account that pays transaction fees and provides the default signing key for single-signer
flows. The network configuration determines which consensus nodes and mirror nodes the client communicates
with.

A client can be constructed directly from an `Operator` and a `NetworkSetting`, or loaded from a named
network identifier registered in the config namespace.

## API Schema

```
namespace client
requires common, keys, config

// The operator account and signing key used by the client for fee payment and default signing.
Operator {
    @@immutable accountId: common.AccountId // the account that pays transaction fees
    @@immutable privateKey: keys.PrivateKey // the key used to sign transactions by default
}

// The central entry point for all SDK operations.
// Holds the operator identity and the network the client connects to.
HieroClient {
    @@immutable operator: Operator             // the operator used for fee payment and default signing
    @@immutable network: config.NetworkSetting // the network this client connects to

    // Close the client and release any underlying resources (connections, thread pools, etc.)
    void close()
}

// factory methods of `HieroClient` that should be added to the namespace in the best language dependent way

// Create a client connected to the given network with the given operator.
HieroClient create(network: config.NetworkSetting, operator: Operator)

// Create a client connected to a named network. The identifier must be registered in the config namespace.
@@throws(not-found-error) HieroClient forNetwork(networkIdentifier: string, operator: Operator)
```

## Examples

### Connecting to Hedera testnet

```
Operator operator = new Operator(
    accountId: AccountId.fromString("0.0.12345"),
    privateKey: PrivateKey.create("302e...")
)

HieroClient client = HieroClient.forNetwork(HEDERA_TESTNET_IDENTIFIER, operator)
```

### Connecting to a custom network

```
NetworkSetting customNetwork = NetworkSetting.getNetworkSetting("my-network")

HieroClient client = HieroClient.create(customNetwork, operator)
```

### Closing the client

```
client.close()
```

## Questions & Comments

- [@rwalworth](https://github.com/rwalworth): I can see use cases where it would be beneficial to switch
  the operator for a `HieroClient` (e.g. testing), as well as the network it connects to. I don't
  necessarily see a benefit in enforcing `@@immutable` here for these types.
- [@hendrikebbers](https://github.com/hendrikebbers): Should `HieroClient` expose execution configuration
  such as max retry attempts and backoff bounds, or should those live only on `Transaction`? Currently
  `Transaction` already has `maxAttempts`, `maxBackoff`, `minBackoff`, and `attemptTimeout`.
- [@hendrikebbers](https://github.com/hendrikebbers): Should `close()` be modelled as `@@async`? Closing
  gRPC channels may involve a drain period that is better handled asynchronously.
