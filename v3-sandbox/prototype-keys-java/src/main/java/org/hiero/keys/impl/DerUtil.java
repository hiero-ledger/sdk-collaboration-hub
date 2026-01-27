package org.hiero.keys.impl;

import org.hiero.keys.ByteImportEncoding;

public final class DerUtil {
    public static byte[] decode(String value) {
        return ByteImportEncoding.HEX.decode(value);
    }
}
