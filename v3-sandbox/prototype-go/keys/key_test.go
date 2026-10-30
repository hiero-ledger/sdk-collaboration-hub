package keys_test

import (
	"testing"

	"github.com/hiero-ledger/sdk-v3-poc-go/keys"
)

var algorithms = []keys.KeyAlgorithm{keys.ED25519, keys.ECDSA}

var message = []byte("hiero sdk v3 prototype")

func TestGenerateSignVerify(t *testing.T) {
	for _, alg := range algorithms {
		alg := alg
		t.Run(alg.String(), func(t *testing.T) {
			priv, err := keys.GeneratePrivateKey(alg)
			if err != nil {
				t.Fatalf("generate: %v", err)
			}
			pub, err := priv.CreatePublicKey()
			if err != nil {
				t.Fatalf("derive public key: %v", err)
			}
			sig, err := priv.Sign(message)
			if err != nil {
				t.Fatalf("sign: %v", err)
			}
			if !pub.Verify(message, sig) {
				t.Fatal("signature did not verify")
			}
		})
	}
}

func TestRawBytesRoundTrip(t *testing.T) {
	for _, alg := range algorithms {
		alg := alg
		t.Run(alg.String(), func(t *testing.T) {
			priv, _ := keys.GeneratePrivateKey(alg)
			pub, _ := priv.CreatePublicKey()

			priv2, err := keys.PrivateKeyFromRawBytes(alg, priv.RawBytes())
			if err != nil {
				t.Fatalf("PrivateKeyFromRawBytes: %v", err)
			}
			pub2, err := keys.PublicKeyFromRawBytes(alg, pub.RawBytes())
			if err != nil {
				t.Fatalf("PublicKeyFromRawBytes: %v", err)
			}

			sig, _ := priv2.Sign(message)
			if !pub2.Verify(message, sig) {
				t.Fatal("signature from restored key did not verify")
			}
		})
	}
}

func TestPEMRoundTrip(t *testing.T) {
	priv, _ := keys.GeneratePrivateKey(keys.ED25519)
	pub, _ := priv.CreatePublicKey()

	privPEM, err := priv.ToPEM()
	if err != nil {
		t.Fatalf("ToPEM private: %v", err)
	}
	pubPEM, err := pub.ToPEM()
	if err != nil {
		t.Fatalf("ToPEM public: %v", err)
	}

	priv2, err := keys.PrivateKeyFromPEM(privPEM)
	if err != nil {
		t.Fatalf("PrivateKeyFromPEM: %v", err)
	}
	pub2, err := keys.PublicKeyFromPEM(pubPEM)
	if err != nil {
		t.Fatalf("PublicKeyFromPEM: %v", err)
	}

	sig, _ := priv2.Sign(message)
	if !pub2.Verify(message, sig) {
		t.Fatal("signature from PEM-restored key did not verify")
	}
}

func TestKeyPairGenerate(t *testing.T) {
	for _, alg := range algorithms {
		alg := alg
		t.Run(alg.String(), func(t *testing.T) {
			pair, err := keys.GenerateKeyPair(alg)
			if err != nil {
				t.Fatalf("GenerateKeyPair: %v", err)
			}
			sig, err := pair.PrivateKey().Sign(message)
			if err != nil {
				t.Fatalf("sign: %v", err)
			}
			if !pair.PublicKey().Verify(message, sig) {
				t.Fatal("KeyPair signature did not verify")
			}
		})
	}
}

func TestKeyMetadata(t *testing.T) {
	priv, _ := keys.GeneratePrivateKey(keys.ED25519)
	pub, _ := priv.CreatePublicKey()

	if priv.Algorithm() != keys.ED25519 {
		t.Errorf("expected ED25519, got %v", priv.Algorithm())
	}
	if priv.KeyType() != keys.Private {
		t.Errorf("expected Private, got %v", priv.KeyType())
	}
	if pub.KeyType() != keys.Public {
		t.Errorf("expected Public, got %v", pub.KeyType())
	}
}
