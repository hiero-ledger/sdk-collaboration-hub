package org.hiero.sdk.simple;

import com.hedera.hashgraph.sdk.proto.TransactionReceipt;
import com.hedera.hashgraph.sdk.proto.TransactionRecord;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import org.hiero.sdk.simple.grpc.GrpcClient;
import org.hiero.sdk.simple.internal.HieroClientImpl;
import org.hiero.sdk.simple.network.Account;
import org.hiero.sdk.simple.network.TransactionId;
import org.hiero.sdk.simple.network.settings.NetworkSettings;
import org.jspecify.annotations.NonNull;

/**
 * Interface representing a Hiero client that provides methods to interact with the Hiero network.
 */
public interface HieroClient {

    /**
     * Returns whether transactions should be signed automatically with the operator account. By doing so a transaction
     * that is sent to the network (see {@code GrpcClient} will be signed with the operator account's key.
     *
     * @return true if transactions should be signed automatically, false otherwise
     */
    default boolean signTransactionsAutomaticallyWithOperator() {
        return true;
    }

    /**
     * Generates a new {@link TransactionId} for use in transactions.
     *
     * @return a new {@link TransactionId}
     */
    @NonNull
    TransactionId generateTransactionId();

    @NonNull
    CompletableFuture<Receipt> queryTransactionReceipt(@NonNull TransactionId transactionId);

    @NonNull
    <R extends Receipt> CompletableFuture<R> queryTransactionReceipt(@NonNull TransactionId transactionId,
            BiFunction<TransactionId, TransactionReceipt, R> receiptFactory);

    @NonNull
    CompletableFuture<Record> queryTransactionRecord(@NonNull TransactionId transactionId);

    <RECEIPT extends Receipt, RECORD extends Record<RECEIPT>> CompletableFuture<RECORD> queryTransactionRecord(
            TransactionId transactionId,
            BiFunction<TransactionId, TransactionRecord, RECORD> recordFactory);

    /**
     * Returns the gRPC client used to communicate with the Hiero network.
     *
     * @return the gRPC client
     */
    @NonNull
    GrpcClient getGrpcClient();

    /**
     * Returns the operator account used for signing transactions and other operations.
     *
     * @return the operator account
     */
    @NonNull
    Account getOperatorAccount();

    /**
     * Returns the network settings used by this Hiero client.
     *
     * @return the network settings
     */
    @NonNull
    NetworkSettings getNetworkSettings();

    /**
     * Returns the default timeout in milliseconds for network operations.
     *
     * @return the default timeout in milliseconds
     */
    default long getDefaultTimeoutInMs() {
        return 30_000; // 30 seconds
    }

    /**
     * Creates a new HieroClient instance with the specified operator account and network settings.
     *
     * @param operatorAccount the operator account
     * @param networkSettings the network settings
     * @return a new HieroClient instance
     */
    @NonNull
    static HieroClient create(@NonNull final Account operatorAccount, @NonNull final NetworkSettings networkSettings) {
        return new HieroClientImpl(operatorAccount, networkSettings, Executors.newCachedThreadPool());
    }

    /**
     * Creates a new HieroClient instance with the specified operator account and network identifier. See
     * {@link NetworkSettings#forIdentifier(String)} for documentation of the network identifier.
     *
     * @param operatorAccount   the operator account
     * @param networkIdentifier the network identifier
     * @return a new HieroClient instance
     * @throws IllegalArgumentException if the network identifier is invalid
     */
    @NonNull
    static HieroClient create(@NonNull final Account operatorAccount, @NonNull final String networkIdentifier) {
        final NetworkSettings networkSettings = NetworkSettings.forIdentifier(networkIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("Invalid network identifier: " + networkIdentifier));
        return new HieroClientImpl(operatorAccount, networkSettings, Executors.newCachedThreadPool());
    }

}
