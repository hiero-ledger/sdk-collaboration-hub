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
AccountCreateTransactionBuilder extends transactions.TransactionBuilder<AccountCreateTransactionBuilder> {
    @@nullable accountMemo:string
    @@default(0) initialBalance:common.Hbar
    key:keys.PublicKey
}

@@finalType
AccountCreateResponse extends transactions.Response<AccountCreateReceipt, AccountCreateRecord> {
}

@@finalType
AccountCreateReceipt extends transactions.Receipt {
    @@immutable accountId:common.AccountId
}

@@finalType
AccountCreateRecord extends transactions.Record<AccountCreateReceipt> {
    @@immutable accountId:common.AccountId
}

```

## Examples

### Simple flow

Creates a new account with a balance of 100 hbars using `buildAndExecute` for a single-signer flow.

```
HieroClient client = ...
PublicKey keyOfNewAccount = ...

AccountCreateResponse response = new AccountCreateTransactionBuilder()
    .setKey(keyOfNewAccount)
    .setInitialBalance(new Hbar(100, HbarUnit.HBAR))
    .buildAndExecute(client);

AccountId newAccountId = response.queryReceipt().getAccountId();
```

### Multi-party signing

Creates a new account where both the operator and another party need to sign.

```
HieroClient client = ...
PublicKey keyOfNewAccount = ...

Transaction tx = new AccountCreateTransactionBuilder()
    .setKey(keyOfNewAccount)
    .setInitialBalance(new Hbar(100, HbarUnit.HBAR))
    .build(client);

tx.sign(operatorKey);
tx.sign(otherPartyKey);

AccountCreateResponse response = tx.execute(client);
AccountId newAccountId = response.queryReceipt().getAccountId();
```

## Questions & Comments
