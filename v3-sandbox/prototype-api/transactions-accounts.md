# Account Transactions API

This section defines the API for account transactions.

## Description

A concrete Transaction implementation for creating a new account. This serves as a representative example of how concrete transaction types are defined. Each concrete transaction type extends `Transaction` (from [transactions.md](transactions.md)) and inherits both the mutable builder methods and the `Executable` implementation.

## API Schema

```
namespace transactions-accounts
requires common, transactions, keys

@@finalType
AccountCreateTransaction extends transactions.Transaction {
    @@nullable accountMemo: string
    @@default(0) initialBalance: common.Hbar
    key: keys.PublicKey
}

@@finalType
AccountCreateResponse extends transactions.Response<AccountCreateReceipt, AccountCreateRecord> {
}

@@finalType
AccountCreateReceipt extends transactions.Receipt {
    @@immutable accountId: common.AccountId
}

@@finalType
AccountCreateRecord extends transactions.Record<AccountCreateReceipt> {
    @@immutable accountId: common.AccountId
}
```

## Example

The following example creates a new account with a balance of 100 hbars.

```
HieroClient client = ...
KeyPair operatorKeyPair = ...

Hbar initialBalance = new Hbar(100, HbarUnit.HBAR)
PublicKey keyOfNewAccount = ...

AccountCreateTransaction transaction = new AccountCreateTransaction()
transaction.setKey(keyOfNewAccount)
transaction.setInitialBalance(initialBalance)

AccountId newAccountId = transaction
    .sign(operatorKeyPair)
    .execute(client)
    .queryReceipt(client)
    .getAccountId()
```

## Questions & Comments
