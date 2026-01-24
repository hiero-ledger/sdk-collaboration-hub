package org.hiero.keys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class PrivateKeyWriteTests {

    @ParameterizedTest(name = "Writing key with algorithm {0}")
    @EnumSource(KeyAlgorithm.class)
    void testPrivateKeyGenerationByContainerAndString(KeyAlgorithm keyAlgorithm) {
        // given
        final PrivateKey privateKey = PrivateKey.generate(keyAlgorithm);


        // when
        String pkcs8PemKey = privateKey.toString(KeyFormat.PKCS8_WITH_PEM);
        byte[] pkcs8DerKey = privateKey.toBytes(KeyFormat.PKCS8_WITH_DER);

        // then
        Assertions.assertNotNull(pkcs8PemKey);
        Assertions.assertTrue(pkcs8PemKey.startsWith("-----BEGIN PRIVATE KEY-----\n"));
        Assertions.assertTrue(pkcs8PemKey.endsWith("-----END PRIVATE KEY-----\n"));
        Assertions.assertNotNull(pkcs8DerKey);
        Assertions.assertTrue(pkcs8DerKey.length != 0);
    }

}
