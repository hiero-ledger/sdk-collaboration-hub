package org.hiero.sdk.simple.transactions.spi;

import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Set;
import java.util.stream.Collectors;
import org.hiero.sdk.simple.HieroClient;
import org.hiero.sdk.simple.Response;
import org.hiero.sdk.simple.Transaction;
import org.jspecify.annotations.NonNull;

public interface TransactionProtobuffSupport<R extends Response, T extends Transaction<T, R>> {

    Class<T> getTransactionClass();

    T unpack(TransactionBody transactionBody);

    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor();

    R createResponse(HieroClient client, com.hedera.hashgraph.sdk.proto.Transaction protoTransaction,
            TransactionResponse protoResponse);

    void updateBodyBuilderWithSpecifics(T transaction, TransactionBody.@NonNull Builder builder);

    static <RE extends Response, TR extends Transaction<TR, RE>> TransactionProtobuffSupport<RE, TR> of(
            Class<TR> transactionClass) {
        final Set<TransactionProtobuffSupport> result = ServiceLoader.load(TransactionProtobuffSupport.class)
                .stream()
                .map(Provider::get)
                .filter(support -> support.getTransactionClass().equals(transactionClass))
                .collect(Collectors.toUnmodifiableSet());
        if (result.isEmpty()) {
            throw new IllegalArgumentException(
                    "No TransactionProtobuffSupport found for " + transactionClass.getName());
        } else if (result.size() > 1) {
            throw new IllegalArgumentException("Multiple TransactionProtobuffSupport found for "
                    + transactionClass.getName() + ": " + result);
        } else {
            return (TransactionProtobuffSupport<RE, TR>) result.iterator().next();
        }
    }

}
