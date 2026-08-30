from enum import Enum
from ..key_type import KeyType


class KeyContainer(Enum):
    PKCS8 = "PKCS8"
    SPKI = "SPKI"

    def supports_type(self, key_type: KeyType) -> bool:
        return key_type == KeyType.PRIVATE if self == KeyContainer.PKCS8 else key_type == KeyType.PUBLIC
