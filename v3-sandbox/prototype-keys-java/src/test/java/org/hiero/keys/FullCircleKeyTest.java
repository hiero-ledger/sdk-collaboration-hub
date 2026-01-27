package org.hiero.keys;

import org.hiero.keys.impl.Hex;
import org.hiero.keys.io.KeyFormat;
import org.hiero.keys.io.RawFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class FullCircleKeyTest {

    private static Stream<Arguments> allVariants() {
        List<Arguments> variants = new ArrayList<>();
        Arrays.stream(KeyAlgorithm.values()).forEach(algorithm -> {
            Arrays.stream(KeyFormat.values())
                    .filter(keyFormat -> keyFormat.supportsType(KeyType.PRIVATE))
                    .forEach(privateKeyFormat -> {
                        Arrays.stream(KeyFormat.values())
                                .filter(keyFormat -> keyFormat.supportsType(KeyType.PUBLIC))
                                .forEach(publicKeyFormat -> {
                                    variants.add(Arguments.of(algorithm, privateKeyFormat, publicKeyFormat));
                                });
                    });
        });
        return variants.stream();
    }

    @ParameterizedTest(name = "Testing {0} / {1} / {2}")
    @MethodSource("allVariants")
    public void testWithByteFormat(KeyAlgorithm keyAlgorithm, KeyFormat privateKeyFormat, KeyFormat publicKeyFormat) {
        final byte[] message = "The quick brown fox jumps over the lazy dog".getBytes();
        final PrivateKey privateKey = PrivateKey.generate(keyAlgorithm);
        final PublicKey publicKey = privateKey.createPublicKey();
        final byte[] signed = privateKey.sign(message);

        //when
        final PrivateKey recreatedPrivateKey;
        final PublicKey recreatedPublicKey;
        if (privateKeyFormat.getRawFormat().equals(RawFormat.BYTES)) {
            final byte[] privateKeyBytes = privateKey.toBytes(privateKeyFormat);
            System.getLogger(FullCircleKeyTest.class.getName()).log(System.Logger.Level.INFO, "Private key: {0}", Hex.encode(privateKeyBytes));
            recreatedPrivateKey = PrivateKey.create(privateKeyFormat, privateKeyBytes);
        } else {
            final String privateKeyString = privateKey.toString(privateKeyFormat);
            System.getLogger(FullCircleKeyTest.class.getName()).log(System.Logger.Level.INFO, "Private key: {0}", privateKeyString);
            recreatedPrivateKey = PrivateKey.create(privateKeyFormat, privateKeyString);
        }
        if (publicKeyFormat.getRawFormat().equals(RawFormat.BYTES)) {
            final byte[] publicKeyBytes = publicKey.toBytes(publicKeyFormat);
            System.getLogger(FullCircleKeyTest.class.getName()).log(System.Logger.Level.INFO, "Public key: {0}", Hex.encode(publicKeyBytes));
            recreatedPublicKey = PublicKey.create(publicKeyFormat, publicKeyBytes);
        } else {
            final String publicKeyString = publicKey.toString(publicKeyFormat);
            System.getLogger(FullCircleKeyTest.class.getName()).log(System.Logger.Level.INFO, "Public key: {0}", publicKeyString);
            recreatedPublicKey = PublicKey.create(publicKeyFormat, publicKeyString);
        }
        final byte[] recreatedSigned = recreatedPrivateKey.sign(message);

        //then
        Assertions.assertEquals(privateKey, recreatedPrivateKey);
        Assertions.assertEquals(publicKey, recreatedPublicKey);
        Assertions.assertEquals(publicKey, PublicKey.create(keyAlgorithm, publicKey.toRawBytes()));
        Assertions.assertArrayEquals(signed, recreatedSigned);
        Assertions.assertTrue(publicKey.verify(message, signed));
        Assertions.assertTrue(recreatedPublicKey.verify(message, signed));
    }

}
