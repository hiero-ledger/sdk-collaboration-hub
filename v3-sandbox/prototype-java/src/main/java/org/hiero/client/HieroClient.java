package org.hiero.client;

import org.hiero.client.impl.HieroClientImpl;
import org.hiero.common.Ledger;
import org.hiero.config.NetworkSetting;
import org.jspecify.annotations.NonNull;

/**
 * The client API used by the SDK to interact with the network. Mirrors the
 * meta-language type {@code client.HieroClient}.
 *
 * <p>The meta-language definition contains a marker comment
 * ({@code TO_BE_DEFINED_IN_FUTURE_VERSIONS}) — only the operator account and ledger
 * are exposed at this stage. The Java interface mirrors that contract and offers a
 * single static factory mapping the namespace-level
 * {@code createClient(networkSettings, operatorAccount)} function.
 */
public interface HieroClient {

    /**
     * @return the operator account this client uses
     */
    @NonNull
    OperatorAccount getOperatorAccount();

    /**
     * @return the ledger this client is connected to
     */
    @NonNull
    Ledger getLedger();

    /**
     * Creates a new {@link HieroClient} that talks to the network described by
     * {@code networkSettings} using {@code operatorAccount} as its signing payer.
     *
     * @param networkSettings the network settings to use
     * @param operatorAccount the operator account to use
     * @return the new client
     */
    @NonNull
    static HieroClient createClient(
            @NonNull final NetworkSetting networkSettings,
            @NonNull final OperatorAccount operatorAccount) {
        return new HieroClientImpl(networkSettings, operatorAccount);
    }
}
