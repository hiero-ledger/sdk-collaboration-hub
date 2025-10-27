package org.hiero.sdk.simple.internal.transactions;

import com.google.auto.service.AutoService;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.CryptoCreateTransactionBody;
import com.hedera.hashgraph.sdk.proto.Transaction;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.util.Objects;
import org.hiero.sdk.simple.HieroClient;
import org.hiero.sdk.simple.internal.grpc.GrpcMethodDescriptorFactory;
import org.hiero.sdk.simple.internal.util.ProtobufUtil;
import org.hiero.sdk.simple.network.Hbar;
import org.hiero.sdk.simple.network.HbarUnit;
import org.hiero.sdk.simple.network.TransactionId;
import org.hiero.sdk.simple.transactions.AccountCreateResponse;
import org.hiero.sdk.simple.transactions.AccountCreateTransaction;
import org.hiero.sdk.simple.transactions.spi.TransactionProtobuffSupport;
import org.jspecify.annotations.NonNull;

@AutoService(TransactionProtobuffSupport.class)
public class AccountCreateTransactionProtobuffSupport implements
        TransactionProtobuffSupport<AccountCreateResponse, AccountCreateTransaction> {

    @Override
    public Class<AccountCreateTransaction> getTransactionClass() {
        return AccountCreateTransaction.class;
    }

    @Override
    public AccountCreateTransaction unpack(TransactionBody transactionBody) {
        final CryptoCreateTransactionBody cryptoCreateBody = transactionBody.getCryptoCreateAccount();
        final AccountCreateTransaction transaction = new AccountCreateTransaction();
        transaction.setFee(Hbar.of(transactionBody.getTransactionFee(), HbarUnit.TINYBAR));
        transaction.setValidDuration(ProtobufUtil.fromProtobuf(transactionBody.getTransactionValidDuration()));
        transaction.setMemo(transactionBody.getMemo());
        transaction.setInitialBalance(Hbar.of(cryptoCreateBody.getInitialBalance(), HbarUnit.TINYBAR));
        transaction.setAccountMemo(cryptoCreateBody.getMemo());
        // TODO: cryptoCreateBody.getKey() conversion currently not supported
        return transaction;
    }

    @Override
    public MethodDescriptor<Transaction, TransactionResponse> getMethodDescriptor() {
        return GrpcMethodDescriptorFactory.getOrCreateMethodDescriptor(
                "proto.CryptoService",
                "createAccount",
                com.hedera.hashgraph.sdk.proto.Transaction::getDefaultInstance,
                com.hedera.hashgraph.sdk.proto.TransactionResponse::getDefaultInstance);
    }

    @Override
    public AccountCreateResponse createResponse(HieroClient client, Transaction protoTransaction,
            TransactionResponse protoResponse) {
        try {
            final TransactionBody body = TransactionBody.parseFrom(protoTransaction.getBodyBytes());
            final TransactionId transactionId = ProtobufUtil.fromProtobuf(body.getTransactionID());
            return new AccountCreateResponse(client, transactionId);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException("Error in converting protobuff objects", e);
        }
    }

    @Override
    public void updateBodyBuilderWithSpecifics(AccountCreateTransaction transaction,
            TransactionBody.@NonNull Builder builder) {
        Objects.requireNonNull(builder, "builder must not be null");
        final CryptoCreateTransactionBody.Builder cryptoCreateBuilder = CryptoCreateTransactionBody.newBuilder();
        final Hbar initialBalance = transaction.getInitialBalance();
        cryptoCreateBuilder.setInitialBalance(initialBalance != null ? initialBalance.tinybar() : 0);
        cryptoCreateBuilder.setMemo(transaction.getAccountMemo());
        cryptoCreateBuilder.setKey(ProtobufUtil.toKeyProtobuf(transaction.getKey()));
        cryptoCreateBuilder.setAutoRenewPeriod(ProtobufUtil.toProtobuf(Duration.ofDays(90)));
        builder.setCryptoCreateAccount(cryptoCreateBuilder);
    }
}
