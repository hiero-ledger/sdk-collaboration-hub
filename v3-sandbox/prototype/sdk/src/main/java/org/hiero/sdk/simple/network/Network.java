package org.hiero.sdk.simple.network;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 *  Network representation.
 *
 * @param identifier  the network identifier
 * @param name  the network name
 * @param id  the binary network id
 */
public record Network(@NonNull String identifier, @Nullable String name, @NonNull byte[] id) {

    public Network {
        Objects.requireNonNull(identifier, "identifier must not be null");
        Objects.requireNonNull(id, "id must not be null");
    }
}
