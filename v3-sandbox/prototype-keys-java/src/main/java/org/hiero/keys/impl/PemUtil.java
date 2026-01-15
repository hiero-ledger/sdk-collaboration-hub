package org.hiero.keys.impl;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class PemUtil {

    private PemUtil() {}

    public static String toPem(final String type, final byte[] der) {
        final String base64 = Base64.getEncoder().encodeToString(der);
        final StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN ").append(type).append("-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            int end = Math.min(i + 64, base64.length());
            sb.append(base64, i, end).append('\n');
        }
        sb.append("-----END ").append(type).append("-----\n");
        return sb.toString();
    }

    public static byte[] fromPem(final String expectedType, final String pem) {
        final String header = "-----BEGIN " + expectedType + "-----";
        final String footer = "-----END " + expectedType + "-----";

        // Normalize line endings and trim surrounding whitespace
        final List<String> lines = pem.replace("\r", "\n").lines()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (lines.isEmpty() || !lines.get(0).equals(header) || !lines.get(lines.size() - 1).equals(footer)) {
            throw new IllegalArgumentException("Invalid PEM for type " + expectedType);
        }

        // Join payload lines and remove any non-base64 characters just in case (defensive)
        final List<String> payload = new ArrayList<>(lines.subList(1, lines.size() - 1));
        String base64 = String.join("", payload)
                .replaceAll("[^A-Za-z0-9+/=]", "");

        // Fix padding if necessary (Base64 length must be multiple of 4)
        final int mod = base64.length() % 4;
        if (mod == 1) {
            // impossible to fix, signal invalid data
            throw new IllegalArgumentException("Invalid Base64 payload in PEM (bad length)");
        } else if (mod == 2) {
            base64 = base64 + "==";
        } else if (mod == 3) {
            base64 = base64 + "=";
        }

        // Use MIME decoder to be tolerant, though we already cleaned the payload
        return Base64.getDecoder().decode(base64);
    }
}
