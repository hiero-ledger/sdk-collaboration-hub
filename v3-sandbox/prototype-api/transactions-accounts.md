# Account Transactions API

This section defines the API for the account transactions.

```
namespace transactions-accounts
requires common, transactions, keys

AccountCreateTransaction extends Transaction<AccountCreatePackedTransaction> {
    @nullable accountMemo:String
    @default(0) initialBalance:Hbar
    key:PublicKey
}

AccountCreatePackedTransaction extends PackedTransaction<AccountCreateTransaction, AccountCreateResponse> {
}

AccountCreateResponse extends Response<AccountCreateReceipt, AccountCreateRecord> {
}

AccountCreateReceipt extends Receipt {
    @immutable accountId:AccountId
}

AccountCreateRecord extends Record<AccountCreateReceipt> {
    @immutable accountId:AccountId
}

```

### Comments
