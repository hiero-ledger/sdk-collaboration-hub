package org.hiero.common;

import java.time.ZonedDateTime;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Represents the exchange rate of HBAR in USD cents.
 *
 * <p>Mirrors the meta-language type {@code common.HBarExchangeRate}. The Java class
 * is renamed to {@code HbarExchangeRate} to follow Java naming conventions
 * (PascalCase, single uppercase letter for acronyms).
 *
 * @param expirationTime         the expiration time of the exchange rate
 * @param exchangeRateInUsdCents the exchange rate of HBar in USD cents
 */
public record HbarExchangeRate(
        @NonNull ZonedDateTime expirationTime,
        double exchangeRateInUsdCents) {

    public HbarExchangeRate {
        Objects.requireNonNull(expirationTime, "expirationTime must not be null");
    }

    /**
     * Returns whether this exchange rate is past its {@link #expirationTime()}.
     *
     * @return {@code true} if the current time is past {@code expirationTime}
     */
    public boolean isExpired() {
        return ZonedDateTime.now(expirationTime.getZone()).isAfter(expirationTime);
    }
}
