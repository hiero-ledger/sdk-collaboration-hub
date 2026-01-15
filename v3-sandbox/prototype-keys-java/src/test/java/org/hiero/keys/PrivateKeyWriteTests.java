package org.hiero.keys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hiero.keys.TestKeyRepresentations.*;

public class PrivateKeyWriteTests {

    @ParameterizedTest(name = "Writing key with algorithm {0}")
    @EnumSource(KeyAlgorithm.class)
    void testPrivateKeyGenerationByContainerAndString(KeyAlgorithm keyAlgorithm) {
        // given
        final PrivateKey privateKey = PrivateKey.generate(keyAlgorithm);


        // when
        String pkcs8PemKey = privateKey.toString(EncodedKeyContainer.PKCS8_WITH_PEM);
        byte[] pkcs8DerKey = privateKey.toBytes(EncodedKeyContainer.PKCS8_WITH_DER);

        // then
        Assertions.assertNotNull(pkcs8PemKey);
        Assertions.assertTrue(pkcs8PemKey.startsWith("-----BEGIN PRIVATE KEY-----\n"));
        Assertions.assertTrue(pkcs8PemKey.endsWith("-----END PRIVATE KEY-----\n"));
        Assertions.assertNotNull(pkcs8DerKey);
        Assertions.assertTrue(pkcs8DerKey.length != 0);
    }

}
