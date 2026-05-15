package keys

// KeyType distinguishes public keys from private keys.
type KeyType int

const (
	Public KeyType = iota
	Private
)

func (t KeyType) String() string {
	switch t {
	case Public:
		return "PUBLIC"
	case Private:
		return "PRIVATE"
	default:
		return "unknown"
	}
}
