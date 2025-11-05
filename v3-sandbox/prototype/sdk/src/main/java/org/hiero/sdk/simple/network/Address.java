package org.hiero.sdk.simple.network;

import org.jspecify.annotations.NonNull;

/**
 *  An address on a Hiero network.
 */
public interface Address {

    /**
     *  Returns the shard number.
     *
     *  @return the shard number
     */
    long shard();

    /**
     *  Returns the realm number.
     *
     * @return the realm number
     */
    long realm();

    /**
     *  Returns the num.
     *
     * @return the num
     */
    long num();

    /**
     *  Returns the checksum.
     *
     * @return the checksum
     */
    @NonNull String checksum();

    /**
     *  Returns true if the checksum is valid for the given network.
     *
     * @param network the network to validate against
     * @return true if the checksum is valid for the given network
     */
    boolean validateChecksum(@NonNull Network network);

    /**
     *  Returns a human readable string representation of this address.
     *
     * @return a human readable string representation of this address
     */
    @NonNull
    default String toHumanReadbleString() {
        return shard() + "." + realm() + "." + num();
    }
}
