package org.hiero.keys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hiero.keys.TestKeyRepresentations.*;

public class PrivateKeyReadingTests {

    private static Stream<Arguments> pkcs8Variants() {
        return Stream.of(
                Arguments.of(EncodedKeyContainer.PKCS8_WITH_DER, PKCS8_WITH_DER_VARIANT_1),
                Arguments.of(EncodedKeyContainer.PKCS8_WITH_DER, PKCS8_WITH_DER_VARIANT_2),
                Arguments.of(EncodedKeyContainer.PKCS8_WITH_DER, PKCS8_WITH_DER_VARIANT_3),
                Arguments.of(EncodedKeyContainer.PKCS8_WITH_DER, PKCS8_WITH_DER_VARIANT_4),
                Arguments.of(EncodedKeyContainer.PKCS8_WITH_DER, PKCS8_WITH_DER_VARIANT_5),
                Arguments.of(EncodedKeyContainer.PKCS8_WITH_DER, PKCS8_WITH_DER_VARIANT_6),
                Arguments.of(EncodedKeyContainer.PKCS8_WITH_DER, PKCS8_WITH_DER_VARIANT_7),
                Arguments.of(EncodedKeyContainer.PKCS8_WITH_PEM, PKCS8_WITH_PEM_VARIANT_1),
                Arguments.of(EncodedKeyContainer.PKCS8_WITH_PEM, PKCS8_WITH_PEM_VARIANT_2),
                Arguments.of(EncodedKeyContainer.PKCS8_WITH_PEM, PKCS8_WITH_PEM_VARIANT_3),
                Arguments.of(EncodedKeyContainer.PKCS8_WITH_PEM, PKCS8_WITH_PEM_VARIANT_4),
                Arguments.of(EncodedKeyContainer.PKCS8_WITH_PEM, PKCS8_WITH_PEM_VARIANT_5)
        );
    }

    @ParameterizedTest(name = "PKCS#8 roundtrip with {0}")
    @MethodSource("pkcs8Variants")
    void testPrivateKeyGenerationByContainerAndString(final EncodedKeyContainer container, final String input) {
        // given
        final String message = "I'm a test message";
        final byte[] messageBytes = message.getBytes();

        // when
        final PrivateKey privateKey = PrivateKey.create(container, input);
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
