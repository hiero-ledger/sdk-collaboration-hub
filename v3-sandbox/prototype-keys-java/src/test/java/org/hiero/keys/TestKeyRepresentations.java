package org.hiero.keys;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.hiero.keys.impl.Hex;
import org.hiero.keys.impl.PemUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class TestKeyRepresentations {

    public static final String PKCS8_WITH_DER_VARIANT_1 = "30 2E 02 01 00 30 05 06 03 2B 65 70 04 22 04 20D3 67 1A 1E 98 BB 22 F0 11 C0 E4 BC F5 12 55 90\n" +
            "E1 5D 8F 21 A7 01 73 09 BB 55 88 52 03 9B C7 5C";

    public static final String PKCS8_WITH_DER_VARIANT_2 = "30 2E 02 01 00 30 05 06 03 2B 65 70 04 22 04 20D3 67 1A 1E 98 BB 22 F0 11 C0 E4 BC F5 12 55 90 E1 5D 8F 21 A7 01 73 09 BB 55 88 52 03 9B C7 5C";

    public static final String PKCS8_WITH_DER_VARIANT_3 = "302E020100300506032B657004220420D3671A1E98BB22F011C0E4BCF5125590E15D8F21A7017309BB558852039BC75C";

    public static final String PKCS8_WITH_DER_VARIANT_4 = "0x302E020100300506032B657004220420D3671A1E98BB22F011C0E4BCF5125590E15D8F21A7017309BB558852039BC75C";

    public static final String PKCS8_WITH_DER_VARIANT_5 = "302e020100300506032b657004220420d3671a1e98bb22f011c0e4bcf5125590e15d8f21a7017309bb558852039bc75c";

    public static final String PKCS8_WITH_DER_VARIANT_6 = "30 2E 02 01 00 30 05 06 03 2B 65 70 04 22 04 20D3 67 1A 1E 98\nBB 22 F0 11 C0 E4 BC F5 12 55 90 E1 5D 8F 21 A7 01 73 09 BB 55 88 52 03 9B C7 5C";

    public static final String PKCS8_WITH_DER_VARIANT_7 = "0x302e020100300506032b657004220420d3671a1e98bb22f011c0e4bcf5125590e15d8f21a7017309bb558852039bc75c";

    public static final String PKCS8_WITH_PEM_VARIANT_1 = "-----BEGIN PRIVATE KEY-----\n" +
            "MC4CAQAwBQYDK2VwBCIEINNnGh6YuyLwEcDkvPUSVZDhXY8hpwFzCbtViFIDm8dc\n" +
            "-----END PRIVATE KEY-----";

    public static final String PKCS8_WITH_PEM_VARIANT_2 = "-----BEGIN PRIVATE KEY-----\n" +
            "MC4CAQAwBQYDK2VwBCIEINNnGh6YuyLwEcDkvPUSVZDhXY8hpwFzCbtViFIDm8dc\n" +
            "-----END PRIVATE KEY-----\n";

    public static final String PKCS8_WITH_PEM_VARIANT_3 = "-----BEGIN PRIVATE KEY-----\r\n" +
            "MC4CAQAwBQYDK2VwBCIEINNnGh6YuyLwEcDkvPUSVZDhXY8hpwFzCbtViFIDm8dc\r\n" +
            "-----END PRIVATE KEY-----";

    public static final String PKCS8_WITH_PEM_VARIANT_4 = "-----BEGIN PRIVATE KEY-----\n" +
            "MC4CAQAwBQYDK2VwBCIEINNnGh6YuyLwEcDkvPUSVZDhXY8hpwFzCbtViFIDm8dc\n\n" +
            "-----END PRIVATE KEY-----";

    public static final String PKCS8_WITH_PEM_VARIANT_5 = "-----BEGIN PRIVATE KEY-----\n" +
            "MC4CAQAwBQYDK2VwBCIEIL+7z6z7l54s8n5p3k5t5y7p9d7j6l3j4p5s3q7c2q1z\n" +
            "-----END PRIVATE KEY-----\n";

    public static final String SPKI_WITH_DER_VARIANT_1 = "30 2A 30 05 06 03 2B 65 70 03 21 00 20D3 67 1A 1E 98 BB 22 F0 11 C0 E4 BC F5 12 55 90 E1 5D 8F 21 A7 01 73 09 BB 55 88 52 03 9B C7 5C";

    public static final String SPKI_WITH_DER_VARIANT_2 = "30 2A 30 05 06 03 2B 65 70 03 21 00 20D3 67 1A 1E 98 BB 22 F0 11 C0 E4 BC F5 12 55 90 E1 5D 8F 21 A7 01 73 09 BB 55 88 52 03 9B C7 5C";

    public static final String SPKI_WITH_DER_VARIANT_3 = "302A300506032B6570032100D3671A1E98BB22F011C0E4BCF5125590E15D8F21A7017309BB558852039BC75C";

    public static final String SPKI_WITH_DER_VARIANT_4 = "0x302A300506032B6570032100D3671A1E98BB22F011C0E4BCF5125590E15D8F21A7017309BB558852039BC75C";

    public static final String SPKI_WITH_DER_VARIANT_5 = "302a300506032b6570032100d3671a1e98bb22f011c0e4bcf5125590e15d8f21a7017309bb558852039bc75c";

    public static final String SPKI_WITH_DER_VARIANT_6 = "30 2A 30 05 06 03 2B 65 70 03 21 00 20D3 67 1A 1E 98\nBB 22 F0 11 C0 E4 BC F5 12 55 90 E1 5D 8F 21 A7 01 73 09 BB 55 88 52 03 9B C7 5C";

    public static final String SPKI_WITH_DER_VARIANT_7 = "0x302a300506032b6570032100d3671a1e98bb22f011c0e4bcf5125590e15d8f21a7017309bb558852039bc75c";

    public static final String SPKI_WITH_PEM_VARIANT_1 = "-----BEGIN PUBLIC KEY-----\n" +
            "MCowBQYDK2VwAyEAINNnGh6YuyLwEcDkvPUJVZDhXY8hpwBzCbtViFIDm8dc\n" +
            "-----END PUBLIC KEY-----";

    public static final String SPKI_WITH_PEM_VARIANT_2 = "-----BEGIN PUBLIC KEY-----\n" +
            "MCowBQYDK2VwAyEAINNnGh6YuyLwEcDkvPUJVZDhXY8hpwBzCbtViFIDm8dc\n" +
            "-----END PUBLIC KEY-----\n";

    public static final String SPKI_WITH_PEM_VARIANT_3 = "-----BEGIN PUBLIC KEY-----\r\n" +
            "MCowBQYDK2VwAyEAINNnGh6YuyLwEcDkvPUJVZDhXY8hpwBzCbtViFIDm8dc\r\n" +
            "-----END PUBLIC KEY-----";

    public static final String SPKI_WITH_PEM_VARIANT_4 = "-----BEGIN PUBLIC KEY-----\n" +
            "MCowBQYDK2VwAyEAINNnGh6YuyLwEcDkvPUJVZDhXY8hpwBzCbtViFIDm8dc\n\n" +
            "-----END PUBLIC KEY-----";

    public static final String SPKI_WITH_PEM_VARIANT_5 = "-----BEGIN PUBLIC KEY-----\n" +
            "MCowBQYDK2VwAyEAIL+7z6z7l54s8n5p3k5t5y7p9d7j6l3j4p5s3q7c2q1z\n" +
            "-----END PUBLIC KEY-----\n";

    private static Stream<Arguments> Pkcs8WithPemVariants() {
        return Stream.of(
                Arguments.of(PKCS8_WITH_PEM_VARIANT_1),
                Arguments.of(PKCS8_WITH_PEM_VARIANT_2),
                Arguments.of(PKCS8_WITH_PEM_VARIANT_3),
                Arguments.of(PKCS8_WITH_PEM_VARIANT_4),
                Arguments.of(PKCS8_WITH_PEM_VARIANT_5)
        );
    }

    private static Stream<Arguments> Pkcs8WithDerVariants() {
        return Stream.of(
                Arguments.of(PKCS8_WITH_DER_VARIANT_1),
                Arguments.of(PKCS8_WITH_DER_VARIANT_2),
                Arguments.of(PKCS8_WITH_DER_VARIANT_3),
                Arguments.of(PKCS8_WITH_DER_VARIANT_4),
                Arguments.of(PKCS8_WITH_DER_VARIANT_5),
                Arguments.of(PKCS8_WITH_DER_VARIANT_6),
                Arguments.of(PKCS8_WITH_DER_VARIANT_7)
        );
    }

    private static Stream<Arguments> spkiDerVariants() {
        return Stream.of(
                Arguments.of(TestKeyRepresentations.SPKI_WITH_DER_VARIANT_1),
                Arguments.of(TestKeyRepresentations.SPKI_WITH_DER_VARIANT_2),
                Arguments.of(TestKeyRepresentations.SPKI_WITH_DER_VARIANT_3),
                Arguments.of(TestKeyRepresentations.SPKI_WITH_DER_VARIANT_4),
                Arguments.of(TestKeyRepresentations.SPKI_WITH_DER_VARIANT_5),
                Arguments.of(TestKeyRepresentations.SPKI_WITH_DER_VARIANT_6),
                Arguments.of(TestKeyRepresentations.SPKI_WITH_DER_VARIANT_7)
        );
    }

    private static Stream<Arguments> spkiPemVariants() {
        return Stream.of(
                Arguments.of(TestKeyRepresentations.SPKI_WITH_PEM_VARIANT_1),
                Arguments.of(TestKeyRepresentations.SPKI_WITH_PEM_VARIANT_2),
                Arguments.of(TestKeyRepresentations.SPKI_WITH_PEM_VARIANT_3),
                Arguments.of(TestKeyRepresentations.SPKI_WITH_PEM_VARIANT_4),
                Arguments.of(TestKeyRepresentations.SPKI_WITH_PEM_VARIANT_5)
        );
    }

    @ParameterizedTest
    @MethodSource("Pkcs8WithPemVariants")
    public void checkPkcs8WithPem(String input) throws Exception {
        byte[] der = PemUtil.fromPem(KeyType.PRIVATE, input);
        checkLowLevelASN1ForPrivateKey(der);
    }

    @ParameterizedTest
    @MethodSource("spkiPemVariants")
    public void checkSpkiWithPem(String input) throws Exception {
        byte[] der = PemUtil.fromPem(KeyType.PUBLIC, input);
        checkLowLevelASN1ForPublicKey(der);
    }

    @ParameterizedTest
    @MethodSource("Pkcs8WithDerVariants")
    public void checkPkcs8WithDer(String input) throws Exception {
        byte[] der = Hex.decode(input);
        checkLowLevelASN1ForPrivateKey(der);
    }

    @ParameterizedTest
    @MethodSource("spkiDerVariants")
    public void checkSpkiWithDer(String input) throws Exception {
        byte[] der = Hex.decode(input);
        checkLowLevelASN1ForPublicKey(der);
    }

    private void checkLowLevelASN1ForPrivateKey(byte[] der) throws Exception {
        try (ASN1InputStream ais = new ASN1InputStream(der)) {
            Object obj = ais.readObject();
            assertNotNull(obj, "Not valid ASN.1-Object");
            ASN1Object pki = PrivateKeyInfo.getInstance(obj);
            assertNotNull(pki, "Not valid ASN.1 Private Key");
        } catch (Exception e) {
            fail("Not valid ASN.1 Private Key", e);
        }
    }

    private void checkLowLevelASN1ForPublicKey(byte[] der) throws Exception {
        try (ASN1InputStream ais = new ASN1InputStream(der)) {
            Object obj = ais.readObject();
            assertNotNull(obj, "Not valid ASN.1-Object");
            ASN1Object pki = SubjectPublicKeyInfo.getInstance(obj);
            assertNotNull(pki, "Not valid ASN.1 Public Key");
        } catch (Exception e) {
            fail("Not valid ASN.1 Public Key", e);
        }
    }

}
