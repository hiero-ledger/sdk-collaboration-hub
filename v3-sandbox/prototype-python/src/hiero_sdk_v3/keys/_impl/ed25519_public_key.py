from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey as _CryptoKey
from cryptography.hazmat.primitives.serialization import Encoding, PublicFormat

from ..key_algorithm import KeyAlgorithm
from ..key_type import KeyType
from ..io.key_format import KeyFormat
from ..io.raw_format import RawFormat
from ..public_key import PublicKey


class Ed25519PublicKey(PublicKey):
    def __init__(self, key: _CryptoKey) -> None:
        self._key = key

    @classmethod
    def from_raw(cls, raw: bytes) -> "Ed25519PublicKey":
        if len(raw) != 32:
            raise ValueError(f"Ed25519 public key must be 32 bytes, got {len(raw)}")
        return cls(_CryptoKey.from_public_bytes(raw))

    @property
    def raw_bytes(self) -> bytes:
        return self._key.public_bytes(Encoding.Raw, PublicFormat.Raw)

    @property
    def algorithm(self) -> KeyAlgorithm:
        return KeyAlgorithm.ED25519

    @property
    def type(self) -> KeyType:
        return KeyType.PUBLIC

    def verify(self, message: bytes, signature: bytes) -> bool:
        try:
            self._key.verify(signature, message)
            return True
        except InvalidSignature:
            return False

    def to_bytes(self, format: KeyFormat) -> bytes:
        if not format.supports_type(KeyType.PUBLIC):
            raise ValueError(f"Format {format} does not support public keys")
        if format.raw_format != RawFormat.BYTES:
            raise ValueError(f"to_bytes requires a BYTES format, got {format}")
        if format == KeyFormat.SPKI_WITH_DER:
            return self._key.public_bytes(Encoding.DER, PublicFormat.SubjectPublicKeyInfo)
        raise ValueError(f"Unsupported format for Ed25519 public key: {format}")

    def to_string(self, format: KeyFormat) -> str:
        if not format.supports_type(KeyType.PUBLIC):
            raise ValueError(f"Format {format} does not support public keys")
        if format.raw_format != RawFormat.STRING:
            raise ValueError(f"to_string requires a STRING format, got {format}")
        if format == KeyFormat.SPKI_WITH_PEM:
            return self._key.public_bytes(Encoding.PEM, PublicFormat.SubjectPublicKeyInfo).decode()
        raise ValueError(f"Unsupported format for Ed25519 public key: {format}")
