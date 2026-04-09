package org.hiero.keys;

import org.jspecify.annotations.NonNull;

/**
 * The two possible key types in the API. Mirrors the meta-language enum
 * {@code keys.KeyType}.
 */
public enum KeyType {

    /** A public key. */
    PUBLIC,

    /** A private key. */
    PRIVATE;

    /**
     * Returns the PEM label associated with this key type. This helper is used
     * internally by the PEM encoder/decoder; it is part of the public enum because
     * it is a pure function of the key type and is useful for callers that want
     * to inspect/produce raw PEM payloads themselves.
     *
     * @return {@code "PUBLIC KEY"} or {@code "PRIVATE KEY"}
     */
    @NonNull
    public String getPemLabel() {
        return switch (this) {
            case PUBLIC -> "PUBLIC KEY";
            case PRIVATE -> "PRIVATE KEY";
        };
    }
}
