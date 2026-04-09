package org.hiero.common;

import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * The different units of Hbar. Mirrors the {@code common.HbarUnit} enum from the
 * meta-language definition.
 *
 * <p>Both attributes ({@code symbol}, {@code tinybars}) are declared {@code @@immutable}
 * in the meta-language and therefore become {@code final} fields set via the constructor.
 */
public enum HbarUnit {

    TINYBAR("tℏ", 1L),
    MICROBAR("μℏ", 100L),
    MILLIBAR("mℏ", 100_000L),
    HBAR("ℏ", 100_000_000L),
    KILOBAR("kℏ", 1_000L * 100_000_000L),
    MEGABAR("Mℏ", 1_000_000L * 100_000_000L),
    GIGABAR("Gℏ", 1_000_000_000L * 100_000_000L);

    private final String symbol;

    private final long tinybars;

    HbarUnit(@NonNull final String symbol, final long tinybars) {
        // No Objects.requireNonNull here on purpose: enum literals are compile-time
        // constants and the constructor is private — null is not reachable.
        this.symbol = symbol;
        this.tinybars = tinybars;
    }

    /**
     * Returns the human readable symbol of this unit (e.g. {@code "ℏ"}).
     *
     * @return the unit symbol
     */
    @NonNull
    public String getSymbol() {
        return symbol;
    }

    /**
     * Returns the number of tinybars contained in one unit of this kind.
     *
     * @return the number of tinybars in one unit
     */
    public long getTinybars() {
        return tinybars;
    }

    /**
     * Returns all defined {@link HbarUnit} values as an immutable list. This is the
     * Java mapping of the meta-language {@code static list<HbarUnit> values()} factory.
     *
     * @return the list of all units, never {@code null}
     */
    @NonNull
    public static List<HbarUnit> valuesAsList() {
        return List.of(values());
    }
}
