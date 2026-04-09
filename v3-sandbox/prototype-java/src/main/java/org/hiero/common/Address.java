package org.hiero.common;

import org.jspecify.annotations.NonNull;

/**
 * Abstract base for all addresses on a Hiero network. Mirrors the meta-language
 * abstraction {@code common.Address}.
 *
 * <p>The meta-language declares all four attributes as {@code @@immutable}. In Java
 * we represent them as accessor methods on the abstraction so concrete subtypes
 * (e.g., {@link AccountId}) can be implemented as records.
 *
 * <p>Note: the meta-language uses {@code uint64} for {@code shard}, {@code realm}
 * and {@code num}. Java has no unsigned primitive types — see REPORT.md for the
 * decision to map {@code uint64} to {@code long} with caller-side semantics.
 */
public interface Address {

    /**
     * @return the shard number (interpreted as unsigned)
     */
    long shard();

    /**
     * @return the realm number (interpreted as unsigned)
     */
    long realm();

    /**
     * @return the entity number within the realm (interpreted as unsigned)
     */
    long num();

    /**
     * @return the checksum of the address; never {@code null}
     */
    @NonNull
    String checksum();

    /**
     * Validates the checksum of the address against the given ledger.
     *
     * @param ledger the ledger this address is associated with, never {@code null}
     * @return {@code true} if the checksum is valid for the given ledger
     */
    boolean validateChecksum(@NonNull Ledger ledger);

    /**
     * Returns the address in {@code shard.realm.num} format.
     *
     * @return the address in {@code shard.realm.num} format
     */
    @NonNull
    String toString();

    /**
     * Returns the address in {@code shard.realm.num-checksum} format.
     *
     * @return the address in {@code shard.realm.num-checksum} format
     */
    @NonNull
    String toStringWithChecksum();
}
