package org.hiero.sdk.simple.test;

import org.hiero.sdk.simple.HieroClient;
import org.hiero.sdk.simple.network.Account;
import org.hiero.sdk.simple.network.AccountId;
import org.hiero.sdk.simple.network.keys.KeyAlgorithm;
import org.hiero.sdk.simple.network.keys.PrivateKey;
import org.hiero.sdk.simple.network.settings.NetworkSettings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HieroClientTest {

    @Test
    protected void testCreation() {
        final AccountId operatorAccountId = AccountId.from("0.0.1234");
        final PrivateKey operatorKey = PrivateKey.generate(KeyAlgorithm.ED25519);
        final Account operatorAccount = Account.of(operatorAccountId, operatorKey);
        final String networkName = "hedera-testnet";
        final NetworkSettings networkSettings = NetworkSettings.forIdentifier(networkName).
                orElseThrow();

        Assertions.assertThrows(NullPointerException.class, () -> {
            HieroClient.create(null, (String) null);
        });
        Assertions.assertThrows(NullPointerException.class, () -> {
            HieroClient.create(null, (NetworkSettings) null);
        });
        Assertions.assertThrows(NullPointerException.class, () -> {
            HieroClient.create(null, networkName);
        });
        Assertions.assertThrows(NullPointerException.class, () -> {
            HieroClient.create(null, networkSettings);
        });
        Assertions.assertThrows(NullPointerException.class, () -> {
            HieroClient.create(operatorAccount, (String) null);
        });
        Assertions.assertThrows(NullPointerException.class, () -> {
            HieroClient.create(operatorAccount, (NetworkSettings) null);
        });
        Assertions.assertDoesNotThrow(() -> {
            HieroClient.create(operatorAccount, networkName);
        });
        Assertions.assertDoesNotThrow(() -> {
            HieroClient.create(operatorAccount, networkSettings);
        });
    }
}
