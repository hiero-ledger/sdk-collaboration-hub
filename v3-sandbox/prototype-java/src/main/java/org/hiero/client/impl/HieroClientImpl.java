package org.hiero.client.impl;

import java.util.Objects;
import org.hiero.client.HieroClient;
import org.hiero.client.OperatorAccount;
import org.hiero.common.Ledger;
import org.hiero.config.NetworkSetting;
import org.jspecify.annotations.NonNull;

/**
 * Trivial in-memory {@link HieroClient} implementation. The prototype intentionally
 * does NOT implement actual gRPC plumbing — that lives behind the
 * {@code transactions-spi} / {@code grpc} / {@code hiero-proto} namespaces which are
 * still placeholders in the meta-language. See REPORT.md.
 */
public final class HieroClientImpl implements HieroClient {

    private final NetworkSetting networkSetting;

    private final OperatorAccount operatorAccount;

    public HieroClientImpl(
            @NonNull final NetworkSetting networkSetting,
            @NonNull final OperatorAccount operatorAccount) {
        this.networkSetting = Objects.requireNonNull(networkSetting, "networkSetting must not be null");
        this.operatorAccount = Objects.requireNonNull(operatorAccount, "operatorAccount must not be null");
    }

    @Override
    @NonNull
    public OperatorAccount getOperatorAccount() {
        return operatorAccount;
    }

    @Override
    @NonNull
    public Ledger getLedger() {
        return networkSetting.getLedger();
    }

    /**
     * Internal accessor used by the prototype's transaction layer to grab the wired
     * {@link NetworkSetting}. Not part of the public {@link HieroClient} interface.
     *
     * @return the network setting this client was created with
     */
    @NonNull
    public NetworkSetting getNetworkSetting() {
        return networkSetting;
    }
}
