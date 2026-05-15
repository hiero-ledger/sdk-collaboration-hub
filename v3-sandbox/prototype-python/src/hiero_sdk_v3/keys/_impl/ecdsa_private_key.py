from cryptography.hazmat.primitives.asymmetric.ec import (
    derive_private_key,
    generate_private_key,
    EllipticCurvePrivateKey,
    SECP256K1,
    ECDSA,
)
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.serialization import Encoding, PrivateFormat, NoEncryption

from ..key_algorithm import KeyAlgorithm
from ..key_type import KeyType
from ..io.key_format import KeyFormat
from ..io.raw_format import RawFormat
from ..private_key import PrivateKey


class EcdsaPrivateKey(PrivateKey):
    def __init__(self, key: EllipticCurvePrivateKey) -> None:
        self._key = key

    @classmethod
    def generate(cls) -> "EcdsaPrivateKey":
        return cls(generate_private_key(SECP256K1()))

    @classmethod
    def from_raw(cls, raw: bytes) -> "EcdsaPrivateKey":
        if len(raw) != 32:
            raise ValueError(f"ECDSA private key must be 32 bytes, got {len(raw)}")
        scalar = int.from_bytes(raw, "big")
        return cls(derive_private_key(scalar, SECP256K1()))

    @property
    def raw_bytes(self) -> bytes:
        return self._key.private_numbers().private_value.to_bytes(32, "big")

    @property
    def algorithm(self) -> KeyAlgorithm:
        return KeyAlgorithm.ECDSA

    @property
    def type(self) -> KeyType:
        return KeyType.PRIVATE

    def sign(self, message: bytes) -> bytes:
        # DER-encoded signature; verify() expects the same format
        return self._key.sign(message, ECDSA(hashes.SHA256()))

    def create_public_key(self) -> "EcdsaPublicKey":
        from .ecdsa_public_key import EcdsaPublicKey
        return EcdsaPublicKey(self._key.public_key())

    def to_bytes(self, format: KeyFormat) -> bytes:
        if not format.supports_type(KeyType.PRIVATE):
            raise ValueError(f"Format {format} does not support private keys")
        if format.raw_format != RawFormat.BYTES:
            raise ValueError(f"to_bytes requires a BYTES format, got {format}")
        if format == KeyFormat.PKCS8_WITH_DER:
            return self._key.private_bytes(Encoding.DER, PrivateFormat.PKCS8, NoEncryption())  # nosec
        raise ValueError(f"Unsupported format for ECDSA private key: {format}")

    def to_string(self, format: KeyFormat) -> str:
        if not format.supports_type(KeyType.PRIVATE):
            raise ValueError(f"Format {format} does not support private keys")
        if format.raw_format != RawFormat.STRING:
            raise ValueError(f"to_string requires a STRING format, got {format}")
        if format == KeyFormat.PKCS8_WITH_PEM:
            return self._key.private_bytes(Encoding.PEM, PrivateFormat.PKCS8, NoEncryption()).decode()  # nosec
        raise ValueError(f"Unsupported format for ECDSA private key: {format}")
