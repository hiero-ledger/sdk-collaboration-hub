package org.hiero.keys.io;

import java.util.Objects;
import org.hiero.keys.KeyType;
import org.jspecify.annotations.NonNull;

/**
 * Supported key container formats. Mirrors the meta-language enum
 * {@code keys.io.KeyContainer}.
 */
public enum KeyContainer {

    /** PKCS#8 Private Key Specification. */
    PKCS8,

    /** Subject Public Key Info. */
    SPKI;

    /**
     * Returns whether this container format supports the given key type.
     *
     * @param type the key type to check
     * @return {@code true} if this container can hold a key of the given type
     */
    public boolean supportsType(@NonNull final KeyType type) {
        Objects.requireNonNull(type, "type must not be null");
        return switch (this) {
            case PKCS8 -> type == KeyType.PRIVATE;
            case SPKI -> type == KeyType.PUBLIC;
        };
    }
}
