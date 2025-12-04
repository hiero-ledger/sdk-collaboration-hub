package org.hiero.sdk.simple;

import org.jspecify.annotations.NonNull;
import java.util.Objects;

/**
 * Response from a {@link HieroClient#getNetworkVersionInfo()} query.
 *
 * @param hapiProtoVersion      the HAPI protobuf version
 * @param hederaServicesVersion the Hedera Services version
 */
public record NetworkVersionInfo(@NonNull SemanticVersion hapiProtoVersion, @NonNull SemanticVersion hederaServicesVersion) {
    public NetworkVersionInfo {
        Objects.requireNonNull(hapiProtoVersion, "hapiProtoVersion must not be null");
        Objects.requireNonNull(hederaServicesVersion, "hederaServicesVersion must not be null");
    }
}
