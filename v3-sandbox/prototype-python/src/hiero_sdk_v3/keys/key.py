from abc import ABC, abstractmethod
from .key_algorithm import KeyAlgorithm
from .key_type import KeyType
from .io.key_format import KeyFormat


class Key(ABC):
    @property
    @abstractmethod
    def raw_bytes(self) -> bytes:
        ...

    @property
    @abstractmethod
    def algorithm(self) -> KeyAlgorithm:
        ...

    @property
    @abstractmethod
    def type(self) -> KeyType:
        ...

    @abstractmethod
    def to_bytes(self, format: KeyFormat) -> bytes:
        ...

    @abstractmethod
    def to_string(self, format: KeyFormat) -> str:
        ...

    def to_raw_bytes(self) -> bytes:
        return self.raw_bytes

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Key):
            return NotImplemented
        return self.type == other.type and self.algorithm == other.algorithm and self.raw_bytes == other.raw_bytes

    def __hash__(self) -> int:
        return hash((self.type, self.algorithm, self.raw_bytes))
