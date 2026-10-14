from dataclasses import dataclass
from .public_key import PublicKey
from .private_key import PrivateKey


@dataclass(frozen=True)
class KeyPair:
    public_key: PublicKey
    private_key: PrivateKey
