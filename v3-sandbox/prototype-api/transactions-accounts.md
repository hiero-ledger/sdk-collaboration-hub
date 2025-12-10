# Account Transactions API

This section defines the API for the account transactions.

## Description

A concrete Transaction implementation for creating a new account.

```
namespace transactions-accounts
requires common, transactions, keys

@@finalType
AccountCreateTransaction extends Transaction<AccountCreatePackedTransaction> {
    @nullable accountMemo:String
    @default(0) initialBalance:Hbar
    key:PublicKey
}

@@finalType
AccountCreatePackedTransaction extends PackedTransaction<AccountCreateTransaction, AccountCreateResponse> {
}

@@finalType
AccountCreateResponse extends Response<AccountCreateReceipt, AccountCreateRecord> {
}

@@finalType
AccountCreateReceipt extends Receipt {
    @immutable accountId:AccountId
}

@@finalType
AccountCreateRecord extends Record<AccountCreateReceipt> {
    @immutable accountId:AccountId
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
