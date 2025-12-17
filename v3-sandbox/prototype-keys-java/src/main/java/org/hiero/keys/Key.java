package org.hiero.keys;

public interface Key {

    byte[] toRawBytes();

    KeyAlgorithm algorithm();

    KeyType type();

    byte[] toBytes(EncodedKeyContainer container);

    String toString(EncodedKeyContainer container);
}
