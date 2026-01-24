package org.hiero.keys;

public interface Key {

    byte[] toRawBytes();

    KeyAlgorithm algorithm();

    KeyType type();

    byte[] toBytes(KeyFormat container);

    String toString(KeyFormat container);
}
