package keys

import "fmt"

// KeyPair holds a matching public and private key.
type KeyPair struct {
	publicKey  *PublicKey
	privateKey *PrivateKey
}

func (kp *KeyPair) PublicKey() *PublicKey   { return kp.publicKey }
func (kp *KeyPair) PrivateKey() *PrivateKey { return kp.privateKey }

// GenerateKeyPair creates a new random key pair for the given algorithm.
func GenerateKeyPair(algorithm KeyAlgorithm) (*KeyPair, error) {
	priv, err := GeneratePrivateKey(algorithm)
	if err != nil {
		return nil, fmt.Errorf("generate key pair: %w", err)
	}
	pub, err := priv.CreatePublicKey()
	if err != nil {
		return nil, fmt.Errorf("derive public key: %w", err)
	}
	return &KeyPair{publicKey: pub, privateKey: priv}, nil
}
