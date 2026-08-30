from __future__ import annotations
from abc import abstractmethod
from .key import Key
from .key_algorithm import KeyAlgorithm
from .io.key_format import KeyFormat
from .public_key import PublicKey


class PrivateKey(Key):
    @abstractmethod
    def sign(self, message: bytes) -> bytes:
        ...

    @abstractmethod
    def create_public_key(self) -> PublicKey:
        ...

    @staticmethod
    def generate(algorithm: KeyAlgorithm) -> PrivateKey:
        from ._impl import generate_private_key
        return generate_private_key(algorithm)

    @staticmethod
    def create(algorithm: KeyAlgorithm, raw_bytes: bytes) -> PrivateKey:
        from ._impl import create_private_key_from_raw
        return create_private_key_from_raw(algorithm, raw_bytes)

    @staticmethod
    def create_from_format(format: KeyFormat, value: bytes | str) -> PrivateKey:
        from ._impl import create_private_key_from_format
        return create_private_key_from_format(format, value)

    @staticmethod
    def from_pem(pem: str) -> PrivateKey:
        return PrivateKey.create_from_format(KeyFormat.PKCS8_WITH_PEM, pem)
