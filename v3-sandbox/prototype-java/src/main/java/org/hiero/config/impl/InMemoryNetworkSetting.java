package org.hiero.config.impl;

import java.util.Objects;
import java.util.Set;
import org.hiero.common.ConsensusNode;
import org.hiero.common.Ledger;
import org.hiero.common.MirrorNode;
import org.hiero.config.NetworkSetting;
import org.jspecify.annotations.NonNull;

/**
 * Trivial in-memory implementation of {@link NetworkSetting}. This is an internal
 * implementation class used by the prototype to wire up tests; it is not part of
 * the public API.
 */
public final class InMemoryNetworkSetting implements NetworkSetting {

    private final Ledger ledger;

    private final Set<ConsensusNode> consensusNodes;

    private final Set<MirrorNode> mirrorNodes;

    public InMemoryNetworkSetting(
            @NonNull final Ledger ledger,
            @NonNull final Set<ConsensusNode> consensusNodes,
            @NonNull final Set<MirrorNode> mirrorNodes) {
        this.ledger = Objects.requireNonNull(ledger, "ledger must not be null");
        Objects.requireNonNull(consensusNodes, "consensusNodes must not be null");
        Objects.requireNonNull(mirrorNodes, "mirrorNodes must not be null");
        // Defensive copy + immutable wrap so the caller cannot mutate the field
        // through the original reference.
        this.consensusNodes = Set.copyOf(consensusNodes);
        this.mirrorNodes = Set.copyOf(mirrorNodes);
    }

    @Override
    @NonNull
    public Ledger getLedger() {
        return ledger;
    }

    @Override
    @NonNull
    public Set<ConsensusNode> getConsensusNodes() {
        return consensusNodes;
    }

    @Override
    @NonNull
    public Set<MirrorNode> getMirrorNodes() {
        return mirrorNodes;
    }
}
