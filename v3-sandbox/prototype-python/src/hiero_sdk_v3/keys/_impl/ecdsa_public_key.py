from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives.asymmetric.ec import (
    EllipticCurvePublicKey,
    SECP256K1,
    ECDSA,
)
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.serialization import Encoding, PublicFormat

from ..key_algorithm import KeyAlgorithm
from ..key_type import KeyType
from ..io.key_format import KeyFormat
from ..io.raw_format import RawFormat
from ..public_key import PublicKey


class EcdsaPublicKey(PublicKey):
    def __init__(self, key: EllipticCurvePublicKey) -> None:
        self._key = key

    @classmethod
    def from_raw(cls, raw: bytes) -> "EcdsaPublicKey":
        if len(raw) not in (33, 65):
            raise ValueError(f"ECDSA public key must be 33 or 65 bytes, got {len(raw)}")
        return cls(EllipticCurvePublicKey.from_encoded_point(SECP256K1(), raw))

    @property
    def raw_bytes(self) -> bytes:
        return self._key.public_bytes(Encoding.X962, PublicFormat.CompressedPoint)

    @property
    def algorithm(self) -> KeyAlgorithm:
        return KeyAlgorithm.ECDSA

    @property
    def type(self) -> KeyType:
        return KeyType.PUBLIC

    def verify(self, message: bytes, signature: bytes) -> bool:
        try:
            self._key.verify(signature, message, ECDSA(hashes.SHA256()))
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
        raise ValueError(f"Unsupported format for ECDSA public key: {format}")

    def to_string(self, format: KeyFormat) -> str:
        if not format.supports_type(KeyType.PUBLIC):
            raise ValueError(f"Format {format} does not support public keys")
        if format.raw_format != RawFormat.STRING:
            raise ValueError(f"to_string requires a STRING format, got {format}")
        if format == KeyFormat.SPKI_WITH_PEM:
            return self._key.public_bytes(Encoding.PEM, PublicFormat.SubjectPublicKeyInfo).decode()
        raise ValueError(f"Unsupported format for ECDSA public key: {format}")
