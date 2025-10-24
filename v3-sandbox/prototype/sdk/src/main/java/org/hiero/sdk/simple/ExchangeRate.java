package org.hiero.sdk.simple;

import java.time.Instant;

public record ExchangeRate(int hbars, int cents, Instant expirationTime) {

}
