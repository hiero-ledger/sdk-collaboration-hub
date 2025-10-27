module org.hiero.sdk.simple {
    exports org.hiero.sdk.simple;
    exports org.hiero.sdk.simple.network;
    exports org.hiero.sdk.simple.network.keys;
    exports org.hiero.sdk.simple.network.settings;
    exports org.hiero.sdk.simple.network.settings.spi;
    exports org.hiero.sdk.simple.transactions;
    exports org.hiero.sdk.simple.grpc;
    exports org.hiero.sdk.simple.transactions.spi;

    requires static org.jspecify;
    requires static com.google.auto.service;
    requires org.bouncycastle.provider;
    requires io.grpc;
    requires com.hiero.proto;
    requires com.google.protobuf;
    requires io.grpc.protobuf.lite;

    uses org.hiero.sdk.simple.network.settings.spi.NetworkSettingsProvider;
    provides org.hiero.sdk.simple.network.settings.spi.NetworkSettingsProvider with
            org.hiero.sdk.simple.internal.network.settings.HederaTestnetSettingsProvider,
            org.hiero.sdk.simple.internal.network.settings.HieroTestEnvironmentSettingsProvider;

    uses org.hiero.sdk.simple.transactions.spi.TransactionProtobuffSupport;
    provides org.hiero.sdk.simple.transactions.spi.TransactionProtobuffSupport with org.hiero.sdk.simple.internal.transactions.AccountCreateTransactionProtobuffSupport;
}