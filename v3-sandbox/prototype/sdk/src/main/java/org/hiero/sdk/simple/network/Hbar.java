package org.hiero.sdk.simple.network;

import java.math.BigDecimal;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

public record Hbar(long tinybar) implements Comparable<Hbar> {

    public static final Hbar ZERO = of(0);

    @Override
    public int compareTo(@NonNull final Hbar o) {
        Objects.requireNonNull(o, "Comparison object must not be null");
        return Long.compare(tinybar(), o.tinybar());
    }

    @NonNull
    public static Hbar of(final long valueInHbar) {
        return of(valueInHbar, HbarUnit.HBAR);
    }

    @NonNull
    public static Hbar of(@NonNull final BigDecimal value) {
        Objects.requireNonNull(value, "value must not be null");
        BigDecimal tinybars = value.multiply(BigDecimal.valueOf(HbarUnit.HBAR.getInTinybar()));
        if (tinybars.doubleValue() % 1 != 0) {
            throw new IllegalArgumentException(
                    "Amount and Unit combination results in a fractional value for tinybar.  Ensure tinybar value is a whole number.");
        }
        return of(tinybars.longValue(), HbarUnit.TINYBAR);
    }

    @NonNull
    public static Hbar of(final long value, @NonNull final HbarUnit unit) {
        Objects.requireNonNull(unit, "unit must not be null");
        return new Hbar(value * unit.getInTinybar());
    }

    public boolean isNegative() {
        return tinybar < 0;
    }
}
