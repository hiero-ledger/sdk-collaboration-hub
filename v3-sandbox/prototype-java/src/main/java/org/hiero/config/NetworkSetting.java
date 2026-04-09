package org.hiero.config;

import java.util.Set;
import org.hiero.common.ConsensusNode;
import org.hiero.common.Ledger;
import org.hiero.common.MirrorNode;
import org.jspecify.annotations.NonNull;

/**
 * Full configuration to connect to a specific network. Mirrors the meta-language
 * type {@code config.NetworkSetting}.
 *
 * <p>The meta-language defines this as a complex type with one immutable field
 * (the {@link Ledger}) plus two getter methods that return immutable sets. The
 * Java mapping is an interface so concrete implementations are free to back the
 * sets with whatever storage they prefer; the immutable contract is enforced by
 * the {@link #getConsensusNodes()} / {@link #getMirrorNodes()} method contract.
 */
public interface NetworkSetting {

    /**
     * @return the definition of the ledger this configuration connects to
     */
    @NonNull
    Ledger getLedger();

    /**
     * Returns an immutable set of consensus nodes. Modifications to the returned
     * set must not affect the original.
     *
     * @return the immutable set of consensus nodes (never {@code null})
     */
    @NonNull
    Set<ConsensusNode> getConsensusNodes();

    /**
     * Returns an immutable set of mirror nodes. Modifications to the returned set
     * must not affect the original.
     *
     * @return the immutable set of mirror nodes (never {@code null})
     */
    @NonNull
    Set<MirrorNode> getMirrorNodes();
}
