package org.hiero.common;

import java.math.BigInteger;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Represents an amount of HBAR. Mirrors the {@code common.Hbar} type from the
 * meta-language definition where both fields are declared {@code @@immutable}, so the
 * Java implementation is a {@code record}.
 *
 * @param amount the amount in the given unit
 * @param unit   the unit the {@code amount} is expressed in
 */
public record Hbar(long amount, @NonNull HbarUnit unit) {

    public Hbar {
        Objects.requireNonNull(unit, "unit must not be null");
        // Defensive guard against silent overflow when converting to tinybars.
        // Mirrors how api-best-practices-java.md asks to enforce numeric constraints
        // close to construction.
        final BigInteger tinybars = BigInteger.valueOf(amount).multiply(BigInteger.valueOf(unit.getTinybars()));
        if (tinybars.bitLength() >= 64) {
            throw new IllegalArgumentException(
                    "Hbar amount overflows int64 tinybars: amount=" + amount + " unit=" + unit);
        }
    }

    /**
     * Convert this {@code Hbar} value to a different unit. The conversion is performed
     * losslessly when possible; otherwise an {@link IllegalArgumentException} is
     * thrown — the meta-language definition does not allow returning a fractional
     * amount.
     *
     * @param targetUnit the target unit, never {@code null}
     * @return a new {@code Hbar} expressed in {@code targetUnit}
     */
    @NonNull
    public Hbar to(@NonNull final HbarUnit targetUnit) {
        Objects.requireNonNull(targetUnit, "targetUnit must not be null");
        final long tinybars = toTinybars();
        if (tinybars % targetUnit.getTinybars() != 0L) {
            throw new IllegalArgumentException("Cannot losslessly convert " + this + " to " + targetUnit);
        }
        return new Hbar(tinybars / targetUnit.getTinybars(), targetUnit);
    }

    /**
     * Returns the total amount as tinybars.
     *
     * @return the total amount in tinybars
     */
    public long toTinybars() {
        return amount * unit.getTinybars();
    }

    /**
     * Convenience factory: create an {@code Hbar} value from a number of tinybars.
     *
     * @param tinybars the number of tinybars
     * @return a new {@code Hbar} value
     */
    @NonNull
    public static Hbar ofTinybars(final long tinybars) {
        return new Hbar(tinybars, HbarUnit.TINYBAR);
    }

    /**
     * Convenience factory: create an {@code Hbar} value from a number of whole hbars.
     *
     * @param hbars the number of hbars
     * @return a new {@code Hbar} value
     */
    @NonNull
    public static Hbar of(final long hbars) {
        return new Hbar(hbars, HbarUnit.HBAR);
    }
}
