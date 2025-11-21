# Client API

This section defines the client API.

## Description

The client API is the API that will be used by the SDK to interact with the network.
A client defines a concrete network connection to a specific network with a specific operator account.

## API Schema

```
namespace client
requires common, config, keys

// Definition of an account that signs and pays for requests
OperatorAccount {
    @@immutable accountId: AccountId // the account id of the operator
    @@immutable privateKey: PrivateKey // the private key of the operator
}

// The client API that will be used by the SDK to interact with the network
HieroClient {
    @@immutable operatorAccount: OperatorAccount // the operator account
    @@immutable ledger: Ledger // the network to connect to
    // TO_BE_DEFINED_IN_FUTURE_VERSIONS
}

// factory methods of `HieroClient` that should be added to the namespace in the best language dependent way

HieroClient createClient(networkSettings: NetworkSetting, operatorAccount: OperatorAccount)
```

## Examples

The following example shows how to create a `HieroClient` instance:

```
AccountId accountId = ...;
PrivateKey privateKey = ...;
OperatorAccount operatorAccount = new OperatorAccount(accountId, privateKey);

NetworkSetting networkSettings = ...;

HieroClient client = HieroClient.createClient(networkSettings, operatorAccount);
```

## Questions & Comments

- [@rwalworth](https://github.com/rwalworth): Should the `operatorAccount` of `HieroClient` be immutable?
