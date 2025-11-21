# Account Transactions API

This section defines the API for the account transactions.

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

### Comments
