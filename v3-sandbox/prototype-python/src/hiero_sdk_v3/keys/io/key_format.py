from enum import Enum
from .key_container import KeyContainer
from .key_encoding import KeyEncoding
from .raw_format import RawFormat
from ..key_type import KeyType


class KeyFormat(Enum):
    def __init__(self, container: KeyContainer, encoding: KeyEncoding) -> None:
        self.container = container
        self.encoding = encoding

    PKCS8_WITH_DER = (KeyContainer.PKCS8, KeyEncoding.DER)
    SPKI_WITH_DER = (KeyContainer.SPKI, KeyEncoding.DER)
    PKCS8_WITH_PEM = (KeyContainer.PKCS8, KeyEncoding.PEM)
    SPKI_WITH_PEM = (KeyContainer.SPKI, KeyEncoding.PEM)

    @property
    def raw_format(self) -> RawFormat:
        return self.encoding.raw_format

    def supports_type(self, key_type: KeyType) -> bool:
        return self.container.supports_type(key_type)
