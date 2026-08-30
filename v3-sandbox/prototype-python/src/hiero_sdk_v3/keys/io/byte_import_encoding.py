import base64
from enum import Enum


class ByteImportEncoding(Enum):
    HEX = "HEX"
    BASE64 = "BASE64"

    def decode(self, value: str) -> bytes:
        if self == ByteImportEncoding.HEX:
            return bytes.fromhex(value)
        return base64.b64decode(value)
