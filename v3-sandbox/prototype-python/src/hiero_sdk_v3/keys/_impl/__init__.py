from .ed25519_private_key import Ed25519PrivateKey
from .ed25519_public_key import Ed25519PublicKey
from .ecdsa_private_key import EcdsaPrivateKey
from .ecdsa_public_key import EcdsaPublicKey
from ..key_algorithm import KeyAlgorithm
from ..io.key_format import KeyFormat
from ..io.raw_format import RawFormat


def generate_private_key(algorithm: KeyAlgorithm):
    if algorithm == KeyAlgorithm.ED25519:
        return Ed25519PrivateKey.generate()
    if algorithm == KeyAlgorithm.ECDSA:
        return EcdsaPrivateKey.generate()
    raise ValueError(f"Unsupported algorithm: {algorithm}")


def create_private_key_from_raw(algorithm: KeyAlgorithm, raw_bytes: bytes):
    if algorithm == KeyAlgorithm.ED25519:
        return Ed25519PrivateKey.from_raw(raw_bytes)
    if algorithm == KeyAlgorithm.ECDSA:
        return EcdsaPrivateKey.from_raw(raw_bytes)
    raise ValueError(f"Unsupported algorithm: {algorithm}")


def create_public_key_from_raw(algorithm: KeyAlgorithm, raw_bytes: bytes):
    if algorithm == KeyAlgorithm.ED25519:
        return Ed25519PublicKey.from_raw(raw_bytes)
    if algorithm == KeyAlgorithm.ECDSA:
        return EcdsaPublicKey.from_raw(raw_bytes)
    raise ValueError(f"Unsupported algorithm: {algorithm}")


def create_private_key_from_format(format: KeyFormat, value):
    from ..key_type import KeyType
    if not format.supports_type(KeyType.PRIVATE):
        raise ValueError(f"Format {format} does not support private keys")
    if isinstance(value, (bytes, bytearray)):
        if format.raw_format != RawFormat.BYTES:
            raise ValueError(f"bytes value requires a BYTES format, got {format}")
        if format == KeyFormat.PKCS8_WITH_DER:
            return _parse_pkcs8_der(bytes(value))
    elif isinstance(value, str):
        if format.raw_format != RawFormat.STRING:
            raise ValueError(f"str value requires a STRING format, got {format}")
        if format == KeyFormat.PKCS8_WITH_PEM:
            return _parse_pkcs8_pem(value)
    raise ValueError(f"Unsupported format or value type: {format}")


def create_public_key_from_format(format: KeyFormat, value):
    from ..key_type import KeyType
    if not format.supports_type(KeyType.PUBLIC):
        raise ValueError(f"Format {format} does not support public keys")
    if isinstance(value, (bytes, bytearray)):
        if format.raw_format != RawFormat.BYTES:
            raise ValueError(f"bytes value requires a BYTES format, got {format}")
        if format == KeyFormat.SPKI_WITH_DER:
            return _parse_spki_der(bytes(value))
    elif isinstance(value, str):
        if format.raw_format != RawFormat.STRING:
            raise ValueError(f"str value requires a STRING format, got {format}")
        if format == KeyFormat.SPKI_WITH_PEM:
            return _parse_spki_pem(value)
    raise ValueError(f"Unsupported format or value type: {format}")


def _parse_pkcs8_der(der: bytes):
    from cryptography.hazmat.primitives.serialization import load_der_private_key
    from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey as _E
    from cryptography.hazmat.primitives.asymmetric.ec import EllipticCurvePrivateKey, SECP256K1
    key = load_der_private_key(der, password=None)  # nosec
    if isinstance(key, _E):
        return Ed25519PrivateKey(key)
    if isinstance(key, EllipticCurvePrivateKey) and isinstance(key.curve, SECP256K1):
        return EcdsaPrivateKey(key)
    raise ValueError("Unsupported key type in PKCS8 DER")


def _parse_pkcs8_pem(pem: str):
    from cryptography.hazmat.primitives.serialization import load_pem_private_key
    from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey as _E
    from cryptography.hazmat.primitives.asymmetric.ec import EllipticCurvePrivateKey, SECP256K1
    key = load_pem_private_key(pem.encode(), password=None)  # nosec
    if isinstance(key, _E):
        return Ed25519PrivateKey(key)
    if isinstance(key, EllipticCurvePrivateKey) and isinstance(key.curve, SECP256K1):
        return EcdsaPrivateKey(key)
    raise ValueError("Unsupported key type in PKCS8 PEM")


def _parse_spki_der(der: bytes):
    from cryptography.hazmat.primitives.serialization import load_der_public_key
    from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey as _E
    from cryptography.hazmat.primitives.asymmetric.ec import EllipticCurvePublicKey, SECP256K1
    key = load_der_public_key(der)
    if isinstance(key, _E):
        return Ed25519PublicKey(key)
    if isinstance(key, EllipticCurvePublicKey) and isinstance(key.curve, SECP256K1):
        return EcdsaPublicKey(key)
    raise ValueError("Unsupported key type in SPKI DER")


def _parse_spki_pem(pem: str):
    from cryptography.hazmat.primitives.serialization import load_pem_public_key
    from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey as _E
    from cryptography.hazmat.primitives.asymmetric.ec import EllipticCurvePublicKey, SECP256K1
    key = load_pem_public_key(pem.encode())
    if isinstance(key, _E):
        return Ed25519PublicKey(key)
    if isinstance(key, EllipticCurvePublicKey) and isinstance(key.curve, SECP256K1):
        return EcdsaPublicKey(key)
    raise ValueError("Unsupported key type in SPKI PEM")
