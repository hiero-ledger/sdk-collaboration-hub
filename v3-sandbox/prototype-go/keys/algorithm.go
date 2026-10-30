package keys

// KeyAlgorithm identifies the cryptographic algorithm used by a key.
type KeyAlgorithm int

const (
	ED25519 KeyAlgorithm = iota
	ECDSA
)

func (a KeyAlgorithm) String() string {
	switch a {
	case ED25519:
		return "ED25519"
	case ECDSA:
		return "ECDSA"
	default:
		return "unknown"
	}
}
