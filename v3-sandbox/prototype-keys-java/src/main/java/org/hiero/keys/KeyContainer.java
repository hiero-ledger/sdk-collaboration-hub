package org.hiero.keys;

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

    public boolean supportsEncoding(final KeyEncoding encoding) {
        return switch (this) {
            case PKCS8, SPKI -> encoding == KeyEncoding.DER || encoding == KeyEncoding.PEM;
        };
    }
}
