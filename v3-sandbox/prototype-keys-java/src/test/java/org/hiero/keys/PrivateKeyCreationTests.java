package org.hiero.keys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hiero.keys.TestKeyRepresentations.*;

public class PrivateKeyCreationTests {

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

    @ParameterizedTest(name = "PrivateKey generation with {0}")
    @EnumSource(KeyAlgorithm.class)
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

    @ParameterizedTest(name = "generate() mit Algorithmus {0}")
    @EnumSource(KeyAlgorithm.class)
    void testGenerateForAllAlgorithms(KeyAlgorithm keyAlgorithm) {
        //when
        final PrivateKey privateKey = PrivateKey.generate(keyAlgorithm);

        //then
        Assertions.assertNotNull(privateKey, "PrivateKey darf nicht null sein");
    }

    @ParameterizedTest(name = "create(algorithm, rawBytes) mit Algorithmus {0}")
    @EnumSource(KeyAlgorithm.class)
    void testCreateFromBytesForAllAlgorithms(KeyAlgorithm keyAlgorithm) {
        //given
        final byte[] message = "I'm a test message".getBytes();
        final PrivateKey original = PrivateKey.generate(keyAlgorithm);
        final byte[] signed = original.sign(message);

        //when
        final PrivateKey recreated = PrivateKey.create(keyAlgorithm, original.toRawBytes());

        //then
        Assertions.assertNotNull(recreated, "Wiederhergestellter PrivateKey darf nicht null sein");
        Assertions.assertNotNull(recreated.createPublicKey(), "PublicKey des wiederhergestellten Schlüssels darf nicht null sein");
        Assertions.assertArrayEquals(signed, recreated.sign(message), "Signaturen müssen übereinstimmen");
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
