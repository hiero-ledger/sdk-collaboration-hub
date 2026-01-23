package org.hiero.keys;

/**
 * All key types.
 */
public enum KeyType {
    PUBLIC,
    PRIVATE;

    public String getPemLabel() {
        return switch (this) {
            case PUBLIC -> "PUBLIC KEY";
            case PRIVATE -> "PRIVATE KEY";
        };
    }
}
