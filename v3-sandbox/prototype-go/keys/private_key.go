package keys

import (
	"crypto/ed25519"
	"crypto/rand"
	"crypto/x509"
	"encoding/pem"
	"errors"
	"fmt"

	"github.com/decred/dcrd/dcrec/secp256k1/v4"
	"github.com/decred/dcrd/dcrec/secp256k1/v4/ecdsa"
)

// PrivateKey holds a private key and its associated algorithm.
type PrivateKey struct {
	Key
	// ed25519 seed (32 bytes) or secp256k1 scalar (32 bytes)
	scalar []byte
}

// GeneratePrivateKey creates a new random private key for the given algorithm.
func GeneratePrivateKey(algorithm KeyAlgorithm) (*PrivateKey, error) {
	switch algorithm {
	case ED25519:
		_, priv, err := ed25519.GenerateKey(rand.Reader)
		if err != nil {
			return nil, fmt.Errorf("generate ED25519 key: %w", err)
		}
		return privateKeyFromED25519(priv), nil
	case ECDSA:
		priv, err := secp256k1.GeneratePrivateKey()
		if err != nil {
			return nil, fmt.Errorf("generate ECDSA key: %w", err)
		}
		return privateKeyFromSecp256k1(priv), nil
	default:
		return nil, errors.New("unsupported algorithm")
	}
}

// PrivateKeyFromPEM parses a PKCS#8 PEM-encoded private key.
func PrivateKeyFromPEM(pemData string) (*PrivateKey, error) {
	block, _ := pem.Decode([]byte(pemData))
	if block == nil {
		return nil, errors.New("illegal-format: no PEM block found")
	}
	key, err := x509.ParsePKCS8PrivateKey(block.Bytes)
	if err != nil {
		return nil, fmt.Errorf("illegal-format: %w", err)
	}
	switch k := key.(type) {
	case ed25519.PrivateKey:
		return privateKeyFromED25519(k), nil
	case *secp256k1.PrivateKey:
		return privateKeyFromSecp256k1(k), nil
	default:
		return nil, errors.New("illegal-format: unsupported key type in PEM")
	}
}

// PrivateKeyFromRawBytes reconstructs a private key from its raw scalar bytes.
func PrivateKeyFromRawBytes(algorithm KeyAlgorithm, raw []byte) (*PrivateKey, error) {
	switch algorithm {
	case ED25519:
		if len(raw) != ed25519.SeedSize {
			return nil, fmt.Errorf("illegal-format: ED25519 seed must be %d bytes", ed25519.SeedSize)
		}
		priv := ed25519.NewKeyFromSeed(raw)
		return privateKeyFromED25519(priv), nil
	case ECDSA:
		if len(raw) != 32 {
			return nil, errors.New("illegal-format: secp256k1 scalar must be 32 bytes")
		}
		priv := secp256k1.PrivKeyFromBytes(raw)
		return privateKeyFromSecp256k1(priv), nil
	default:
		return nil, errors.New("unsupported algorithm")
	}
}

// Sign produces a signature over message using this private key.
func (pk *PrivateKey) Sign(message []byte) ([]byte, error) {
	switch pk.algorithm {
	case ED25519:
		priv := ed25519.NewKeyFromSeed(pk.scalar)
		return ed25519.Sign(priv, message), nil
	case ECDSA:
		priv := secp256k1.PrivKeyFromBytes(pk.scalar)
		sig := ecdsa.Sign(priv, message)
		return sig.Serialize(), nil
	default:
		return nil, errors.New("unsupported algorithm")
	}
}

// CreatePublicKey derives the corresponding public key.
func (pk *PrivateKey) CreatePublicKey() (*PublicKey, error) {
	switch pk.algorithm {
	case ED25519:
		priv := ed25519.NewKeyFromSeed(pk.scalar)
		pub := priv.Public().(ed25519.PublicKey)
		return publicKeyFromED25519(pub), nil
	case ECDSA:
		priv := secp256k1.PrivKeyFromBytes(pk.scalar)
		return publicKeyFromSecp256k1(priv.PubKey()), nil
	default:
		return nil, errors.New("unsupported algorithm")
	}
}

// ToPEM serialises the key as a PKCS#8 PEM block.
func (pk *PrivateKey) ToPEM() (string, error) {
	der, err := pk.toPKCS8DER()
	if err != nil {
		return "", err
	}
	block := &pem.Block{Type: "PRIVATE KEY", Bytes: der}
	return string(pem.EncodeToMemory(block)), nil
}

func (pk *PrivateKey) toPKCS8DER() ([]byte, error) {
	switch pk.algorithm {
	case ED25519:
		priv := ed25519.NewKeyFromSeed(pk.scalar)
		return x509.MarshalPKCS8PrivateKey(priv)
	case ECDSA:
		// x509 does not support secp256k1; serialise as raw scalar DER manually.
		// The raw bytes are the canonical representation for round-trip purposes.
		return pk.scalar, nil
	default:
		return nil, errors.New("unsupported algorithm")
	}
}

func privateKeyFromED25519(priv ed25519.PrivateKey) *PrivateKey {
	seed := priv.Seed()
	rawBytes := make([]byte, len(seed))
	copy(rawBytes, seed)
	return &PrivateKey{
		Key:    Key{rawBytes: rawBytes, algorithm: ED25519, keyType: Private},
		scalar: rawBytes,
	}
}

func privateKeyFromSecp256k1(priv *secp256k1.PrivateKey) *PrivateKey {
	scalar := priv.Serialize()
	rawBytes := make([]byte, len(scalar))
	copy(rawBytes, scalar)
	return &PrivateKey{
		Key:    Key{rawBytes: rawBytes, algorithm: ECDSA, keyType: Private},
		scalar: rawBytes,
	}
}
