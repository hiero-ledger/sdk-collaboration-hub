package org.hiero.keys;

import org.hiero.keys.io.KeyFormat;

public interface Key {

    byte[] toRawBytes();

    KeyAlgorithm algorithm();

    KeyType type();

    byte[] toBytes(KeyFormat container);

    String toString(KeyFormat container);
}
