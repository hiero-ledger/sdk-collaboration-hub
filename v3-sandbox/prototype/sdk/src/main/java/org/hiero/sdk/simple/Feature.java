package org.hiero.sdk.simple;

import org.jspecify.annotations.NonNull;

/**
 * Enumeration of features supported by the Hiero network, along with their minimum required version.
 */
public enum Feature {
    /**
     * HIP-1021: Auto-renew account assignment.
     */
    HIP_1021_AUTO_RENEW_ACCOUNTS(0, 38, 0, "HIP-1021: Auto-renew account assignment"),
    
    /**
     * HIP-1086: Staking rewards.
     */
    HIP_1086_STAKING_REWARDS(0, 40, 0, "HIP-1086: Staking rewards"),
    
    /**
     * EVM address aliases.
     */
    EVM_ADDRESS_ALIASES(0, 39, 0, "EVM address aliases"),
    
    /**
     * Lazy account creation.
     */
    LAZY_CREATION(0, 41, 0, "Lazy account creation"),
    
    /**
     * Contract call improvements.
     */
    SMART_CONTRACT_CONTRACT_CALL(0, 42, 0, "Contract call improvements");

    private final SemanticVersion minimumVersion;
    private final String description;

    Feature(int major, int minor, int patch, String description) {
        this.minimumVersion = new SemanticVersion(major, minor, patch, null, null);
        this.description = description;
    }

    /**
     * Returns the minimum network version required for this feature.
     *
     * @return the minimum version
     */
    @NonNull
    public SemanticVersion getMinimumVersion() {
        return minimumVersion;
    }

    /**
     * Returns the description of the feature.
     *
     * @return the description
     */
    @NonNull
    public String getDescription() {
        return description;
    }
}
