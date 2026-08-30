from enum import Enum
from .raw_format import RawFormat


class KeyEncoding(Enum):
    DER = "DER"
    PEM = "PEM"

    @property
    def raw_format(self) -> RawFormat:
        return RawFormat.BYTES if self == KeyEncoding.DER else RawFormat.STRING
