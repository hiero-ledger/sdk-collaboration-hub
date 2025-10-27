package org.hiero.sdk.simple.internal.util;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.AccountID;
import com.hedera.hashgraph.sdk.proto.ExchangeRateSet;
import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;
import com.hedera.hashgraph.sdk.proto.SignaturePair;
import com.hedera.hashgraph.sdk.proto.Timestamp;
import com.hedera.hashgraph.sdk.proto.TimestampSeconds;
import com.hedera.hashgraph.sdk.proto.TransactionID;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.hiero.sdk.simple.ExchangeRate;
import org.hiero.sdk.simple.TransactionStatus;
import org.hiero.sdk.simple.network.AccountId;
import org.hiero.sdk.simple.network.TransactionId;
import org.hiero.sdk.simple.network.keys.Key;
import org.hiero.sdk.simple.network.keys.KeyAlgorithm;
import org.hiero.sdk.simple.network.keys.KeyEncoding;
import org.hiero.sdk.simple.network.keys.PublicKey;
import org.jspecify.annotations.NonNull;

public final class ProtobufUtil {

    public static TransactionID toProtobuf(@NonNull TransactionId transactionId) {
        var id = TransactionID.newBuilder();

        if (transactionId.accountId() != null) {
            id.setAccountID(toProtobuf(transactionId.accountId()));
        }

        if (transactionId.validStart() != null) {
            id.setTransactionValidStart(toProtobuf(transactionId.validStart()));
        }

        return id.build();
    }

    public static AccountID toProtobuf(@NonNull AccountId accountId) {
        var accountIdBuilder = AccountID.newBuilder()
                .setShardNum(accountId.shard())
                .setRealmNum(accountId.realm());
        accountIdBuilder.setAccountNum(accountId.num());
        return accountIdBuilder.build();
    }


    @NonNull
    public static Duration fromProtobuf(com.hedera.hashgraph.sdk.proto.@NonNull Duration duration) {
        Objects.requireNonNull(duration, "duration must not be null");
        return Duration.ofSeconds(duration.getSeconds());
    }

    public static com.hedera.hashgraph.sdk.proto.@NonNull Duration toProtobuf(@NonNull Duration duration) {
        Objects.requireNonNull(duration, "duration must not be null");
        return com.hedera.hashgraph.sdk.proto.Duration.newBuilder()
                .setSeconds(duration.getSeconds())
                .build();
    }

    public static Instant fromProtobuf(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    /**
     * Create an instance from a timestamp in seconds protobuf.
     *
     * @param timestampSeconds the protobuf
     * @return the instance
     */
    public static Instant fromProtobuf(TimestampSeconds timestampSeconds) {
        return Instant.ofEpochSecond(timestampSeconds.getSeconds());
    }

    /**
     * Convert an instance into a timestamp.
     *
     * @param instant the instance
     * @return the timestamp
     */
    public static Timestamp toProtobuf(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    /**
     * Convert an instance into a timestamp in seconds.
     *
     * @param instant the instance
     * @return the timestamp in seconds
     */
    public static TimestampSeconds toSecondsProtobuf(Instant instant) {
        return TimestampSeconds.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .build();
    }

    public static com.hedera.hashgraph.sdk.proto.Key toKeyProtobuf(Key key) {
        if (key instanceof PublicKey publicKey) {
            if (publicKey.algorithm() == KeyAlgorithm.ECDSA) {
                return com.hedera.hashgraph.sdk.proto.Key.newBuilder()
                        .setECDSASecp256K1(ByteString.copyFrom(publicKey.toBytes(KeyEncoding.RAW)))
                        .build();
            } else {
                return com.hedera.hashgraph.sdk.proto.Key.newBuilder()
                        .setEd25519(ByteString.copyFrom(publicKey.toBytes(KeyEncoding.RAW)))
                        .build();
            }
        } else {
            throw new IllegalArgumentException("Unsupported key type: " + key.getClass().getName());
        }
    }

    public static SignaturePair toSignaturePairProtobuf(PublicKey publicKey, byte[] signature) {
        if (publicKey.algorithm() == KeyAlgorithm.ECDSA) {
            return SignaturePair.newBuilder()
                    .setPubKeyPrefix(ByteString.copyFrom(publicKey.toBytes(KeyEncoding.RAW)))
                    .setECDSASecp256K1(ByteString.copyFrom(signature))
                    .build();
        } else {
            return SignaturePair.newBuilder()
                    .setPubKeyPrefix(ByteString.copyFrom(publicKey.toBytes(KeyEncoding.RAW)))
                    .setEd25519(ByteString.copyFrom(signature))
                    .build();
        }
    }

    public static TransactionId fromProtobuf(TransactionID transactionID) {
        var accountId = transactionID.hasAccountID() ? fromProtobuf(transactionID.getAccountID()) : null;
        var validStart = transactionID.hasTransactionValidStart()
                ? fromProtobuf(transactionID.getTransactionValidStart())
                : null;
        return new TransactionId(accountId, validStart);
    }

    public static AccountId fromProtobuf(AccountID accountID) {
        Objects.requireNonNull(accountID);
        return new AccountId(
                accountID.getShardNum(),
                accountID.getRealmNum(),
                accountID.getAccountNum(),
                null);
    }

    public static ExchangeRate fromProtobuf(ExchangeRateSet exchangeRate) {
        Objects.requireNonNull(exchangeRate, "exchangeRate must not be null");
        return new ExchangeRate(exchangeRate.getCurrentRate().getHbarEquiv(),
                exchangeRate.getCurrentRate().getCentEquiv(),
                fromProtobuf(exchangeRate.getCurrentRate().getExpirationTime()));
    }

    public static TransactionStatus fromProtobuf(ResponseCodeEnum status) {
        return TransactionStatus.UNDEFINED;
    }
}
