package org.hiero.sdk.simple.transactions;

import com.hedera.hashgraph.sdk.proto.AccountID;
import com.hedera.hashgraph.sdk.proto.TransactionReceipt;
import com.hedera.hashgraph.sdk.proto.TransactionRecord;
import java.time.Instant;
import org.hiero.sdk.simple.ExchangeRate;
import org.hiero.sdk.simple.HieroClient;
import org.hiero.sdk.simple.TransactionStatus;
import org.hiero.sdk.simple.internal.AbstractResponse;
import org.hiero.sdk.simple.internal.util.ProtobufUtil;
import org.hiero.sdk.simple.network.AccountId;
import org.hiero.sdk.simple.network.TransactionId;
import org.jspecify.annotations.NonNull;

public final class AccountCreateResponse extends
        AbstractResponse<AccountCreateReceipt, AccountCreateRecord> {

    public AccountCreateResponse(@NonNull HieroClient hieroClient, @NonNull final TransactionId transactionId) {
        super(hieroClient, transactionId, (id, r) -> createReceipt(id, r),
                (id, r) -> createRecord(id, r));
    }

    private static AccountCreateReceipt createReceipt(TransactionId transactionId, TransactionReceipt receipt) {
        final AccountID accountIdProto = receipt.getAccountID();
        if (accountIdProto == null) {
            throw new IllegalStateException("Account ID is null in the receipt");
        }
        final TransactionStatus status = ProtobufUtil.fromProtobuf(receipt.getStatus());
        final ExchangeRate exchangeRate = ProtobufUtil.fromProtobuf(receipt.getExchangeRate());
        AccountId accountID = ProtobufUtil.fromProtobuf(accountIdProto);
        return new AccountCreateReceipt(transactionId, status, exchangeRate, accountID);
    }

    private static AccountCreateRecord createRecord(TransactionId transactionId, TransactionRecord record) {
        final AccountID accountIdProto = record.getReceipt().getAccountID();
        if (accountIdProto == null) {
            throw new IllegalStateException("Account ID is null in the receipt");
        }
        final AccountCreateReceipt receipt = createReceipt(transactionId, record.getReceipt());
        final Instant consensusTimestamp = ProtobufUtil.fromProtobuf(record.getConsensusTimestamp());
        return new AccountCreateRecord(receipt, consensusTimestamp);
    }
}
