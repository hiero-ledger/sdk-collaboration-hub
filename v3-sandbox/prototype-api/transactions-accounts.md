# Account Transactions API

This section defines the API for the account transactions.

## Description

A concrete Transaction implementation for creating a new account.

```
namespace transactions-accounts
requires common, transactions, keys

@@finalType
AccountCreateTransaction extends transactions.Transaction<AccountCreatePackedTransaction> {
    @@nullable accountMemo:String
    @@default(0) initialBalance:common.Hbar
    key:keys.PublicKey
}

@@finalType
AccountCreatePackedTransaction extends transactions.PackedTransaction<AccountCreateTransaction, AccountCreateResponse> {
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

## Example

The following example creates a new account with a balance of 100 hbars.

```
HieroClient client = ...
PrivateKey operatorKey = ...

Hbar initialBalance = new Hbar(100, HbarUnit.HBAR);
PublicKey keyOfNewAccount = ...

AccountCreateTransaction transaction = new AccountCreateTransaction(keyOfNewAccount, initialBalance);
AccountId newAccountId = transaction.pack(client)
           .sign(operatorKey)
           .execute(client)
           .sendAndWait(30_000)
           .queryReceiptAndWait(30_000)
           .getAccountId();
```

## Questions & Comments
