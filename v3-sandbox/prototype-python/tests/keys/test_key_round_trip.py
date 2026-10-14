from unittest import TestCase

import pytest

from hiero_sdk_v3.keys.key_algorithm import KeyAlgorithm
from hiero_sdk_v3.keys.io.key_format import KeyFormat
from hiero_sdk_v3.keys.private_key import PrivateKey
from hiero_sdk_v3.keys.public_key import PublicKey

MESSAGE = b"The quick brown fox jumps over the lazy dog"
CHECKS = TestCase()


@pytest.mark.parametrize("algorithm", [KeyAlgorithm.ED25519, KeyAlgorithm.ECDSA])
def test_generate_sign_verify(algorithm):
    private_key = PrivateKey.generate(algorithm)
    public_key = private_key.create_public_key()
    signature = private_key.sign(MESSAGE)
    CHECKS.assertTrue(public_key.verify(MESSAGE, signature))


@pytest.mark.parametrize("algorithm", [KeyAlgorithm.ED25519, KeyAlgorithm.ECDSA])
def test_pem_round_trip(algorithm):
    private_key = PrivateKey.generate(algorithm)
    public_key = private_key.create_public_key()

    private_pem = private_key.to_string(KeyFormat.PKCS8_WITH_PEM)
    public_pem = public_key.to_string(KeyFormat.SPKI_WITH_PEM)

    restored_private = PrivateKey.from_pem(private_pem)
    restored_public = PublicKey.from_pem(public_pem)

    CHECKS.assertEqual(restored_private, private_key)
    CHECKS.assertEqual(restored_public, public_key)

    signature = restored_private.sign(MESSAGE)
    CHECKS.assertTrue(restored_public.verify(MESSAGE, signature))


@pytest.mark.parametrize("algorithm", [KeyAlgorithm.ED25519, KeyAlgorithm.ECDSA])
def test_raw_bytes_round_trip(algorithm):
    private_key = PrivateKey.generate(algorithm)
    public_key = private_key.create_public_key()

    restored_private = PrivateKey.create(algorithm, private_key.raw_bytes)
    restored_public = PublicKey.create(algorithm, public_key.raw_bytes)

    CHECKS.assertEqual(restored_private, private_key)
    CHECKS.assertEqual(restored_public, public_key)


@pytest.mark.parametrize("algorithm", [KeyAlgorithm.ED25519, KeyAlgorithm.ECDSA])
def test_der_round_trip(algorithm):
    private_key = PrivateKey.generate(algorithm)
    public_key = private_key.create_public_key()

    private_der = private_key.to_bytes(KeyFormat.PKCS8_WITH_DER)
    public_der = public_key.to_bytes(KeyFormat.SPKI_WITH_DER)

    restored_private = PrivateKey.create_from_format(KeyFormat.PKCS8_WITH_DER, private_der)
    restored_public = PublicKey.create_from_format(KeyFormat.SPKI_WITH_DER, public_der)

    CHECKS.assertEqual(restored_private, private_key)
    CHECKS.assertEqual(restored_public, public_key)


@pytest.mark.parametrize("algorithm", [KeyAlgorithm.ED25519, KeyAlgorithm.ECDSA])
def test_public_key_derived_from_private(algorithm):
    private_key = PrivateKey.generate(algorithm)
    public_key_a = private_key.create_public_key()
    public_key_b = PublicKey.create(algorithm, public_key_a.raw_bytes)
    CHECKS.assertEqual(public_key_a, public_key_b)


def test_wrong_signature_does_not_verify():
    private_key = PrivateKey.generate(KeyAlgorithm.ED25519)
    public_key = private_key.create_public_key()
    signature = private_key.sign(MESSAGE)
    CHECKS.assertFalse(public_key.verify(b"tampered message", signature))
