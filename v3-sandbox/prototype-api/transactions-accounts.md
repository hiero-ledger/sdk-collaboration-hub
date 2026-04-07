# Account Transactions API

This section defines the API for the account transactions.

## Description

A concrete TransactionBuilder implementation for creating a new account. Demonstrates the builder pattern where
domain-specific setters live on the builder and `build()` produces a universal `Transaction`.

## API Schema

```
namespace transactions-accounts
requires common, transactions, keys

@@finalType
AccountCreateTransactionBuilder extends transactions.TransactionBuilder<AccountCreateTransactionBuilder, transactions.Response<AccountCreateReceipt>> {
    @@nullable accountMemo: string
    @@default(0) initialBalance: common.Hbar
    key: keys.PublicKey
}

@@finalType
// Extends the base Receipt with the account ID assigned by the consensus node.
// This is the only new field: everything else (status, exchangeRate, etc.) lives on Receipt.
AccountCreateReceipt extends transactions.Receipt {
    @@immutable accountId: common.AccountId
}

```

## Examples

### Simple flow

Creates a new account with a balance of 100 hbars using `buildAndExecute` for a single-signer flow.

```
HieroClient client = ...
PublicKey keyOfNewAccount = ...

Response<AccountCreateReceipt> response = new AccountCreateTransactionBuilder()
    .setKey(keyOfNewAccount)
    .setInitialBalance(new Hbar(100, HbarUnit.HBAR))
    .buildAndExecute(client);

AccountId newAccountId = response.queryReceipt().getAccountId();
```

### Multi-party signing

Creates a new account where both the operator and another party need to sign. Because `build()` returns
`Transaction<Response<AccountCreateReceipt>>`, the typed response is preserved through the signing chain.

```
HieroClient client = ...
PublicKey keyOfNewAccount = ...

Transaction<Response<AccountCreateReceipt>> tx = new AccountCreateTransactionBuilder()
    .setKey(keyOfNewAccount)
    .setInitialBalance(new Hbar(100, HbarUnit.HBAR))
    .build(client);

tx.sign(operatorKey);
tx.sign(otherPartyKey);

Response<AccountCreateReceipt> response = tx.execute(client);
AccountId newAccountId = response.queryReceipt().getAccountId();
```

## Questions & Comments
