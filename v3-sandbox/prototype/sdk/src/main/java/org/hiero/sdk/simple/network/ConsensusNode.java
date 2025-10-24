package org.hiero.sdk.simple.network;

import org.jspecify.annotations.NonNull;

public record ConsensusNode(@NonNull String ip, @NonNull String port, @NonNull String account) {

    /**
     * Get the address of the consensus node. The address is the IP address and port of the consensus node.
     *
     * @return the address
     */
    @NonNull
    public String getAddress() {
        return ip + ":" + port;
    }

    /**
     * Get the account ID of the consensus node.
     *
     * @return the account ID
     */
    @NonNull
    public AccountId getAccountId() {
        return AccountId.from(account);
    }
}
