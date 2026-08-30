from __future__ import annotations
from abc import abstractmethod
from .key import Key
from .key_algorithm import KeyAlgorithm
from .io.key_format import KeyFormat


class PublicKey(Key):
    @abstractmethod
    def verify(self, message: bytes, signature: bytes) -> bool:
        ...

    @staticmethod
    def create(algorithm: KeyAlgorithm, raw_bytes: bytes) -> PublicKey:
        from ._impl import create_public_key_from_raw
        return create_public_key_from_raw(algorithm, raw_bytes)

    @staticmethod
    def create_from_format(format: KeyFormat, value: bytes | str) -> PublicKey:
        from ._impl import create_public_key_from_format
        return create_public_key_from_format(format, value)

    @staticmethod
    def from_pem(pem: str) -> PublicKey:
        return PublicKey.create_from_format(KeyFormat.SPKI_WITH_PEM, pem)
