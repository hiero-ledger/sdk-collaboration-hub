package org.hiero.config;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.hiero.sdk.annotation.ThreadSafe;
import org.jspecify.annotations.NonNull;

/**
 * Static registry for {@link NetworkSetting} instances. This is the Java mapping of
 * the namespace-level factory methods defined in the {@code config} meta-language
 * namespace:
 *
 * <ul>
 *   <li>{@code constant HEDERA_MAINNET_IDENTIFIER} → {@link #HEDERA_MAINNET_IDENTIFIER}</li>
 *   <li>{@code constant HEDERA_TESTNET_IDENTIFIER} → {@link #HEDERA_TESTNET_IDENTIFIER}</li>
 *   <li>{@code void registerNetworkSetting(...)} → {@link #registerNetworkSetting(String, NetworkSetting)}</li>
 *   <li>{@code @@throws(not-found-error) NetworkSetting getNetworkSetting(...)} → {@link #getNetworkSetting(String)}</li>
 * </ul>
 *
 * <p>The api-best-practice guide says namespace-level factories should live as static
 * methods on the type they create. {@code NetworkSetting} is an interface, so the
 * registry would clutter the interface contract with static state. Following the
 * convention of grouping related namespace-level functions in a {@code FooConstants}
 * /{@code FooFactory} helper class, we expose them on this dedicated holder.
 *
 * <p>Both methods are thread-safe so external modules can register network settings
 * at any time during application bootstrap.
 */
public final class NetworkSettings {

    /**
     * Identifier for the Hedera mainnet.
     */
    public static final String HEDERA_MAINNET_IDENTIFIER = "hedera-mainnet";

    /**
     * Identifier for the Hedera testnet.
     */
    public static final String HEDERA_TESTNET_IDENTIFIER = "hedera-testnet";

    private static final ConcurrentMap<String, NetworkSetting> REGISTRY = new ConcurrentHashMap<>();

    private NetworkSettings() {
        // Prevent instantiation — this is a static helper.
    }

    /**
     * Registers a network configuration under the given identifier. If a setting is
     * already registered under the same identifier it will be replaced.
     *
     * @param identifier the identifier to register the setting under
     * @param setting    the setting to register
     */
    @ThreadSafe
    public static void registerNetworkSetting(@NonNull final String identifier, @NonNull final NetworkSetting setting) {
        Objects.requireNonNull(identifier, "identifier must not be null");
        Objects.requireNonNull(setting, "setting must not be null");
        REGISTRY.put(identifier, setting);
    }

    /**
     * Returns the network setting that has been registered under the given identifier.
     *
     * @param identifier the identifier of the network setting to look up
     * @return the registered {@link NetworkSetting}; never {@code null}
     * @throws NoSuchElementException Java mapping of the meta-language
     *                                {@code @@throws(not-found-error)}
     */
    @ThreadSafe
    @NonNull
    public static NetworkSetting getNetworkSetting(@NonNull final String identifier) {
        Objects.requireNonNull(identifier, "identifier must not be null");
        final NetworkSetting setting = REGISTRY.get(identifier);
        if (setting == null) {
            throw new NoSuchElementException("No network setting registered for identifier: " + identifier);
        }
        return setting;
    }
}
