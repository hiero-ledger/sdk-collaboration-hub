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
        final List<String> lines = pem.lines().map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (lines.isEmpty() || !lines.get(0).equals(header) || !lines.get(lines.size() - 1).equals(footer)) {
            throw new IllegalArgumentException("Invalid PEM for type " + expectedType);
        }
        final List<String> payload = new ArrayList<>(lines.subList(1, lines.size() - 1));
        final String base64 = String.join("", payload);
        return Base64.getDecoder().decode(base64);
    }
}
