package org.hiero.common;

import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents a specific ledger instance.
 *
 * <p>Mirrors the meta-language type {@code common.Ledger}. Both fields are immutable;
 * since {@code id} is a {@code byte[]}, defensive copies are taken at construction
 * and on access so the record cannot be mutated through its array reference.
 *
 * <p>This is implemented as a non-record {@code class} on purpose: a Java {@code record}
 * for {@code byte[]} would expose the array reference directly via the canonical
 * accessor and would have a broken {@code equals}/{@code hashCode}.
 */
public final class Ledger {

    private final byte[] id;

    @Nullable
    private final String name;

    public Ledger(@NonNull final byte[] id, @Nullable final String name) {
        Objects.requireNonNull(id, "id must not be null");
        this.id = id.clone();
        this.name = name;
    }

    /**
     * Returns the identifier of the ledger as a defensive copy.
     *
     * @return the identifier of the ledger
     */
    @NonNull
    public byte[] getId() {
        return id.clone();
    }

    /**
     * Returns the human readable name of the network or {@code null} if no name is
     * defined.
     *
     * @return the name or {@code null}
     */
    @Nullable
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Ledger other)) {
            return false;
        }
        return Arrays.equals(id, other.id) && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(id) + Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "Ledger[id=<" + id.length + " bytes>, name=" + Objects.toString(name, "N/A") + "]";
    }
}
