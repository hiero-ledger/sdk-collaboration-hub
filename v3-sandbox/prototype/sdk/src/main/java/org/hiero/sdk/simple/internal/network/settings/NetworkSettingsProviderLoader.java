package org.hiero.sdk.simple.internal.network.settings;

import java.lang.System.Logger.Level;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import org.hiero.sdk.simple.network.settings.NetworkSettings;
import org.hiero.sdk.simple.network.settings.spi.NetworkSettingsProvider;
import org.jspecify.annotations.NonNull;

/**
 * Loads network settings from all available {@link NetworkSettingsProvider} implementations by using Java SPI.
 */
public final class NetworkSettingsProviderLoader {

    private final static System.Logger logger = System.getLogger(NetworkSettingsProviderLoader.class.getName());

    private final static NetworkSettingsProviderLoader instance = new NetworkSettingsProviderLoader();

    private final Set<NetworkSettings> settings;

    private NetworkSettingsProviderLoader() {
        final Set<NetworkSettings> loaded = new HashSet<>();
        final ServiceLoader<NetworkSettingsProvider> loader = ServiceLoader.load(NetworkSettingsProvider.class);
        loader.stream().forEach(provider -> {
            final NetworkSettingsProvider networkSettingsProvider = provider.get();
            logger.log(Level.INFO, "Loading network settings from provider: {}", networkSettingsProvider.getName());
            final Set<NetworkSettings> networkSettingsFromProvider = networkSettingsProvider.createNetworkSettings();
            logger.log(Level.DEBUG, "Loaded {} network settings from provider {}", networkSettingsFromProvider.size(),
                    networkSettingsProvider.getName());
            networkSettingsFromProvider.forEach(setting -> {
                if (loaded.stream().anyMatch(
                        existing -> Objects.equals(existing.getNetworkIdentifier(), setting.getNetworkIdentifier()))) {
                    throw new IllegalStateException(
                            "Network settings with identifier " + setting.getNetworkIdentifier() + " already loaded");
                } else {
                    loaded.add(setting);
                }
            });
        });
        this.settings = Collections.unmodifiableSet(loaded);
    }

    /**
     * Returns all loaded network settings.
     *
     * @return all loaded network settings
     */
    @NonNull
    public Set<NetworkSettings> all() {
        return settings;
    }

    /**
     * Returns the network settings for the given identifier.
     *
     * @param identifier the network identifier
     * @return the network settings for the given identifier
     */
    @NonNull
    public Optional<NetworkSettings> forIdentifier(@NonNull final String identifier) {
        Objects.requireNonNull(identifier, "identifier must not be null");
        return all().stream().filter(settings -> Objects.equals(settings.getNetworkIdentifier(), identifier))
                .findFirst();
    }

    /**
     * Returns the singleton instance of this class.
     *
     * @return the singleton instance of this class
     */
    @NonNull
    public static NetworkSettingsProviderLoader getInstance() {
        return instance;
    }
}
