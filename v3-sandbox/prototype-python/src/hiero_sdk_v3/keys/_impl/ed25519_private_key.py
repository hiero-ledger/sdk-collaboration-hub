from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey as _CryptoKey
from cryptography.hazmat.primitives.serialization import Encoding, PrivateFormat, NoEncryption

from ..key_algorithm import KeyAlgorithm
from ..key_type import KeyType
from ..io.key_format import KeyFormat
from ..io.raw_format import RawFormat
from ..private_key import PrivateKey


class Ed25519PrivateKey(PrivateKey):
    def __init__(self, key: _CryptoKey) -> None:
        self._key = key

    @classmethod
    def generate(cls) -> "Ed25519PrivateKey":
        return cls(_CryptoKey.generate())

    @classmethod
    def from_raw(cls, raw: bytes) -> "Ed25519PrivateKey":
        if len(raw) != 32:
            raise ValueError(f"Ed25519 private key must be 32 bytes, got {len(raw)}")
        return cls(_CryptoKey.from_private_bytes(raw))

    @property
    def raw_bytes(self) -> bytes:
        return self._key.private_bytes_raw()

    @property
    def algorithm(self) -> KeyAlgorithm:
        return KeyAlgorithm.ED25519

    @property
    def type(self) -> KeyType:
        return KeyType.PRIVATE

    def sign(self, message: bytes) -> bytes:
        return self._key.sign(message)

    def create_public_key(self) -> "Ed25519PublicKey":
        from .ed25519_public_key import Ed25519PublicKey
        return Ed25519PublicKey(self._key.public_key())

    def to_bytes(self, format: KeyFormat) -> bytes:
        if not format.supports_type(KeyType.PRIVATE):
            raise ValueError(f"Format {format} does not support private keys")
        if format.raw_format != RawFormat.BYTES:
            raise ValueError(f"to_bytes requires a BYTES format, got {format}")
        if format == KeyFormat.PKCS8_WITH_DER:
            return self._key.private_bytes(Encoding.DER, PrivateFormat.PKCS8, NoEncryption())  # nosec
        raise ValueError(f"Unsupported format for Ed25519 private key: {format}")

    def to_string(self, format: KeyFormat) -> str:
        if not format.supports_type(KeyType.PRIVATE):
            raise ValueError(f"Format {format} does not support private keys")
        if format.raw_format != RawFormat.STRING:
            raise ValueError(f"to_string requires a STRING format, got {format}")
        if format == KeyFormat.PKCS8_WITH_PEM:
            return self._key.private_bytes(Encoding.PEM, PrivateFormat.PKCS8, NoEncryption()).decode()  # nosec
        raise ValueError(f"Unsupported format for Ed25519 private key: {format}")
