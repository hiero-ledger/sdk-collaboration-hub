# Client API

This section defines the client API.

```
namespace client
requires common, keys

OperatorAccount {
    @@immutable accountId: AccountId // the account id of the operator
    @@immutable privateKey: PrivateKey // the private key of the operator
}

HieroClient {
    @@immutable operatorAccount: OperatorAccount // the operator account
    @@immutable ledger: Ledger // the network to connect to
    // TO_BE_DEFINED_IN_FUTURE_VERSIONS
}

```