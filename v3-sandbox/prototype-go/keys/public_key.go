package keys

import (
	"crypto/ed25519"
	"crypto/x509"
	"encoding/pem"
	"errors"
	"fmt"

	"github.com/decred/dcrd/dcrec/secp256k1/v4"
	"github.com/decred/dcrd/dcrec/secp256k1/v4/ecdsa"
)

// PublicKey holds a public key and its associated algorithm.
type PublicKey struct {
	Key
	// raw uncompressed/compressed point or ed25519 public key bytes
	pubBytes []byte
}

// PublicKeyFromPEM parses an SPKI PEM-encoded public key.
func PublicKeyFromPEM(pemData string) (*PublicKey, error) {
	block, _ := pem.Decode([]byte(pemData))
	if block == nil {
		return nil, errors.New("illegal-format: no PEM block found")
	}
	key, err := x509.ParsePKIXPublicKey(block.Bytes)
	if err != nil {
		return nil, fmt.Errorf("illegal-format: %w", err)
	}
	switch k := key.(type) {
	case ed25519.PublicKey:
		return publicKeyFromED25519(k), nil
	default:
		return nil, fmt.Errorf("illegal-format: unsupported key type %T", k)
	}
}

// PublicKeyFromRawBytes reconstructs a public key from raw bytes.
// For ED25519 pass the 32-byte public key; for ECDSA pass the 33-byte compressed point.
func PublicKeyFromRawBytes(algorithm KeyAlgorithm, raw []byte) (*PublicKey, error) {
	switch algorithm {
	case ED25519:
		if len(raw) != ed25519.PublicKeySize {
			return nil, fmt.Errorf("illegal-format: ED25519 public key must be %d bytes", ed25519.PublicKeySize)
		}
		return publicKeyFromED25519(ed25519.PublicKey(raw)), nil
	case ECDSA:
		pub, err := secp256k1.ParsePubKey(raw)
		if err != nil {
			return nil, fmt.Errorf("illegal-format: %w", err)
		}
		return publicKeyFromSecp256k1(pub), nil
	default:
		return nil, errors.New("unsupported algorithm")
	}
}

// Verify returns true if signature is a valid signature of message under this key.
func (pk *PublicKey) Verify(message, signature []byte) bool {
	switch pk.algorithm {
	case ED25519:
		return ed25519.Verify(ed25519.PublicKey(pk.pubBytes), message, signature)
	case ECDSA:
		pub, err := secp256k1.ParsePubKey(pk.pubBytes)
		if err != nil {
			return false
		}
		sig, err := ecdsa.ParseDERSignature(signature)
		if err != nil {
			return false
		}
		return sig.Verify(message, pub)
	default:
		return false
	}
}

// ToPEM serialises the public key as an SPKI PEM block (ED25519 only).
// ECDSA keys are returned as compressed-point hex for portability.
func (pk *PublicKey) ToPEM() (string, error) {
	if pk.algorithm != ED25519 {
		return "", errors.New("illegal-format: PEM export is only supported for ED25519 public keys")
	}
	der, err := x509.MarshalPKIXPublicKey(ed25519.PublicKey(pk.pubBytes))
	if err != nil {
		return "", fmt.Errorf("marshal SPKI: %w", err)
	}
	block := &pem.Block{Type: "PUBLIC KEY", Bytes: der}
	return string(pem.EncodeToMemory(block)), nil
}

func publicKeyFromED25519(pub ed25519.PublicKey) *PublicKey {
	raw := make([]byte, len(pub))
	copy(raw, pub)
	return &PublicKey{
		Key:      Key{rawBytes: raw, algorithm: ED25519, keyType: Public},
		pubBytes: raw,
	}
}

func publicKeyFromSecp256k1(pub *secp256k1.PublicKey) *PublicKey {
	compressed := pub.SerializeCompressed()
	raw := make([]byte, len(compressed))
	copy(raw, compressed)
	return &PublicKey{
		Key:      Key{rawBytes: raw, algorithm: ECDSA, keyType: Public},
		pubBytes: raw,
	}
}
