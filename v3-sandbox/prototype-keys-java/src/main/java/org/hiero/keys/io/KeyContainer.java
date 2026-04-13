package org.hiero.keys.io;

import org.hiero.keys.KeyType;

/**
 * Supported container formats.
 */
public enum KeyContainer {
    PKCS8, // private keys
    SPKI;  // public keys

    public boolean supportsType(final KeyType type) {
        return switch (this) {
            case PKCS8 -> type == KeyType.PRIVATE;
            case SPKI -> type == KeyType.PUBLIC;
        };
    }
}
