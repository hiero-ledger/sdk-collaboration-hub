package org.hiero.keys.impl;

import org.hiero.keys.io.ByteImportEncoding;

public final class DerUtil {
    public static byte[] decode(String value) {
        return ByteImportEncoding.HEX.decode(value);
    }
}
