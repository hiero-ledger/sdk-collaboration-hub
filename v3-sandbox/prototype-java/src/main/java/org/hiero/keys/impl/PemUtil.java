package org.hiero.keys.impl;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import org.hiero.keys.KeyType;
import org.hiero.keys.io.ByteImportEncoding;
import org.jspecify.annotations.NonNull;

/**
 * Internal PEM helpers shared between the algorithm-specific implementations.
 */
public final class PemUtil {

    private PemUtil() {
    }

    @NonNull
    public static String toPem(@NonNull final String type, @NonNull final byte[] der) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(der, "der must not be null");
        final String base64 = Base64.getEncoder().encodeToString(der);
        final StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN ").append(type).append("-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            final int end = Math.min(i + 64, base64.length());
            sb.append(base64, i, end).append('\n');
        }
        sb.append("-----END ").append(type).append("-----\n");
        return sb.toString();
    }

    @NonNull
    public static byte[] fromPem(@NonNull final KeyType keyType, @NonNull final String pem) {
        Objects.requireNonNull(keyType, "keyType must not be null");
        Objects.requireNonNull(pem, "pem must not be null");

        final String header = "-----BEGIN " + keyType.getPemLabel() + "-----";
        final String footer = "-----END " + keyType.getPemLabel() + "-----";

        final List<String> lines = pem.replace("\r", "\n").lines()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (lines.isEmpty() || !lines.get(0).equals(header) || !lines.get(lines.size() - 1).equals(footer)) {
            throw new IllegalArgumentException("Invalid PEM for type " + keyType);
        }

        final List<String> payload = new ArrayList<>(lines.subList(1, lines.size() - 1));
        String base64 = String.join("", payload).replaceAll("[^A-Za-z0-9+/=]", "");

        final int mod = base64.length() % 4;
        if (mod == 1) {
            throw new IllegalArgumentException("Invalid Base64 payload in PEM (corrupted data)");
        } else if (mod == 2) {
            base64 = base64 + "==";
        } else if (mod == 3) {
            base64 = base64 + "=";
        }
        return ByteImportEncoding.BASE64.decode(base64);
    }
}
