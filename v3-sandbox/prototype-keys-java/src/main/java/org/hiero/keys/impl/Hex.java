package org.hiero.keys.impl;

public final class Hex {
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private Hex() {}

    public static String encode(final byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] decode(final String hex) {
        final String s = hex.replace(" ", "").replace("\n", "").replace("\r", "");
        int len = s.length();
        if ((len & 1) != 0) {
            throw new IllegalArgumentException("Hex string has odd length: " + len);
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) {
                throw new IllegalArgumentException("Invalid hex char at position " + i);
            }
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
    }
}
