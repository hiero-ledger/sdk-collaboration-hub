package org.hiero.common;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Represents a consensus node on a network. Mirrors the meta-language type
 * {@code common.ConsensusNode}. All fields are {@code @@immutable}, so the type is a
 * Java record.
 *
 * @param ip      the IP address of the node
 * @param port    the port of the node (mapped from {@code uint16})
 * @param account the account id of the node
 */
public record ConsensusNode(
        @NonNull String ip,
        int port,
        @NonNull AccountId account) {

    /**
     * The maximum value for an unsigned 16-bit integer.
     */
    private static final int MAX_UINT16 = 0xFFFF;

    public ConsensusNode {
        Objects.requireNonNull(ip, "ip must not be null");
        Objects.requireNonNull(account, "account must not be null");
        if (port < 0 || port > MAX_UINT16) {
            throw new IllegalArgumentException("port must be in range [0, " + MAX_UINT16 + "]: " + port);
        }
    }
}
