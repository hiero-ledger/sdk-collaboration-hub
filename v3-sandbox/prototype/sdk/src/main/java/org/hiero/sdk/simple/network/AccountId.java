package org.hiero.sdk.simple.network;

import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

public record AccountId(long shard,
                        long realm,
                        long num,
                        String checksum) implements Address {

    private static final Pattern ENTITY_ID_REGEX =
            Pattern.compile("(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-([a-z]{5}))?$");

    public static AccountId from(String id) {
        if ((id.startsWith("0x") && id.length() == 42) || id.length() == 40) {
            throw new IllegalArgumentException("Creation based on EVM address is not supported.");
        }
        var match = ENTITY_ID_REGEX.matcher(id);
        if (match.find()) {
            return new AccountId(
                    Long.parseLong(match.group(1)),
                    Long.parseLong(match.group(2)),
                    Long.parseLong(match.group(3)),
                    match.group(4));
        }
        throw new IllegalArgumentException("Invalid Account ID '" + id + "'");
    }

    @Override
    public boolean validateChecksum(@NonNull Network network) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     *  Returns a human readable string representation of this address.
     *
     * @return a human readable string representation of this address
     */
    @Override
    public String toString() {
        return shard() + "." + realm() + "." + num();
    }

    /**
     *  Returns a human readable string representation of this address with checksum.
     *
     * @return a human readable string representation of this address with checksum
     */
    @NonNull
    public String toStringWithChecksum() {
        return toString() + "-" + checksum();
    }
}
