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
AccountCreateTransactionBuilder extends transactions.TransactionBuilder<AccountCreateTransactionBuilder, AccountCreateResponse> {
    @@nullable accountMemo: string
    @@default(0) initialBalance: common.Hbar
    key: keys.PublicKey
}

// Named alias for ergonomics. Appears in IDE completions, method signatures, and docs in a way that
// Response<AccountCreateReceipt> does not. All transaction-specific data lives on AccountCreateReceipt.
@@alias AccountCreateResponse = transactions.Response<AccountCreateReceipt>

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
The return type is `AccountCreateResponse` — no cast or extraction needed.

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

Creates a new account where both the operator and another party need to sign. Because `build()` returns
`Transaction<AccountCreateResponse>`, the typed response is preserved through the signing chain.

```
HieroClient client = ...
PublicKey keyOfNewAccount = ...

Transaction<AccountCreateResponse> tx = new AccountCreateTransactionBuilder()
    .setKey(keyOfNewAccount)
    .setInitialBalance(new Hbar(100, HbarUnit.HBAR))
    .build(client);

tx.sign(operatorKey);
tx.sign(otherPartyKey);

AccountCreateResponse response = tx.execute(client);
AccountId newAccountId = response.queryReceipt().getAccountId();
```

## Questions & Comments
