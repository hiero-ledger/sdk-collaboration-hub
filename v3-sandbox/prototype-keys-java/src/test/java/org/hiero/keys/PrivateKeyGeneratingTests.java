package org.hiero.keys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class PrivateKeyGeneratingTests {


    private static Stream<Arguments> keyAlgorithms() {
        return Stream.of(
                Arguments.of(KeyAlgorithm.ED25519),
                Arguments.of(KeyAlgorithm.ECDSA)
        );
    }


    @ParameterizedTest(name = "PrivateKey generation with {0}")
    @MethodSource("keyAlgorithms")
    void testPrivateKeyGeneration(KeyAlgorithm keyAlgorithm) {
        // given
        final PrivateKey privateKey = PrivateKey.generate(keyAlgorithm);
        final String message = "I'm a test message";
        final byte[] messageBytes = message.getBytes();

        // when
        final PublicKey publicKey = privateKey.createPublicKey();
        final byte[] signature = privateKey.sign(messageBytes);
        final boolean verified = publicKey.verify(messageBytes, signature);
        final boolean shouldNotVerified1 = publicKey.verify("I'm a test message123".getBytes(), signature);
        final boolean shouldNotVerified2 = publicKey.verify(new byte[]{}, signature);

        // then
        Assertions.assertNotNull(privateKey);
        Assertions.assertNotNull(publicKey);
        Assertions.assertNotNull(signature);
        Assertions.assertTrue(signature.length > 0);
        Assertions.assertTrue(verified);
        Assertions.assertFalse(shouldNotVerified1);
        Assertions.assertFalse(shouldNotVerified2);
    }
}
