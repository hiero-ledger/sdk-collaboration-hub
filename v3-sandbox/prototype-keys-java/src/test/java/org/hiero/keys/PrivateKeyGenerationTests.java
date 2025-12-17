package org.hiero.keys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PrivateKeyGenerationTests {


    @Test
    void test_PKCS8_WITH_DER() {
        //given
        final String input = "30 2E 02 01 00 30 05 06 03 2B 65 70 04 22 04 20D3 67 1A 1E 98 BB 22 F0 11 C0 E4 BC F5 12 55 90\n" +
                "E1 5D 8F 21 A7 01 73 09 BB 55 88 52 03 9B C7 5C";
        final String message = "I'm a test message";
        final byte[] messagebytes = message.getBytes();


        //when
        final PrivateKey privateKey = PrivateKey.create(EncodedKeyContainer.PKCS8_WITH_DER, input);
        final PublicKey publicKey = privateKey.createPublicKey();
        final byte[] signed = privateKey.sign(messagebytes);
        final boolean verified = publicKey.verify(messagebytes, signed);
        final boolean shouldNotVerified1 = publicKey.verify("I'm a test message123".getBytes(), signed);
        final boolean shouldNotVerified2 = publicKey.verify(new byte[]{}, signed);

        //then
        Assertions.assertNotNull(privateKey);
        Assertions.assertNotNull(publicKey);
        Assertions.assertNotNull(signed);
        Assertions.assertFalse(signed.length == 0);
        Assertions.assertTrue(verified);
        Assertions.assertFalse(shouldNotVerified1);
        Assertions.assertFalse(shouldNotVerified2);
    }
}
