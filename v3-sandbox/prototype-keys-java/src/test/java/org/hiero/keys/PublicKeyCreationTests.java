package org.hiero.keys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hiero.keys.TestKeyRepresentations.*;

public class PublicKeyCreationTests {

    private static Stream<Arguments> spkiVariants() {
        return Stream.of(
                Arguments.of(KeyFormat.SPKI_WITH_DER, SPKI_WITH_DER_VARIANT_1),
                Arguments.of(KeyFormat.SPKI_WITH_DER, SPKI_WITH_DER_VARIANT_2),
                Arguments.of(KeyFormat.SPKI_WITH_DER, SPKI_WITH_DER_VARIANT_3),
                Arguments.of(KeyFormat.SPKI_WITH_DER, SPKI_WITH_DER_VARIANT_4),
                Arguments.of(KeyFormat.SPKI_WITH_DER, SPKI_WITH_DER_VARIANT_5),
                Arguments.of(KeyFormat.SPKI_WITH_DER, SPKI_WITH_DER_VARIANT_6),
                Arguments.of(KeyFormat.SPKI_WITH_DER, SPKI_WITH_DER_VARIANT_7),
                Arguments.of(KeyFormat.SPKI_WITH_PEM, SPKI_WITH_PEM_VARIANT_1),
                Arguments.of(KeyFormat.SPKI_WITH_PEM, SPKI_WITH_PEM_VARIANT_2),
                Arguments.of(KeyFormat.SPKI_WITH_PEM, SPKI_WITH_PEM_VARIANT_3),
                Arguments.of(KeyFormat.SPKI_WITH_PEM, SPKI_WITH_PEM_VARIANT_4),
                Arguments.of(KeyFormat.SPKI_WITH_PEM, SPKI_WITH_PEM_VARIANT_5)
        );
    }

    @ParameterizedTest(name = "SPKI creation with {0}")
    @MethodSource("spkiVariants")
    void testPublicKeyReadingByContainerAndString(final KeyFormat container, final String input) {
        // when
        final PublicKey publicKey = PublicKey.create(container, input);

        // then
        Assertions.assertNotNull(publicKey, "PublicKey must not be null for container: " + container);
    }

}
