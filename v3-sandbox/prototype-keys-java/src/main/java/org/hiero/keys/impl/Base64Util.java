package org.hiero.keys.impl;

import java.util.Base64;

public final class Base64Util {
    private Base64Util() {}

    public static String encode(final byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] decode(final String s) {
        return Base64.getDecoder().decode(s);
    }
}
