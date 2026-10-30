package keys

// Key is the base type for all cryptographic keys.
// Fields are unexported to enforce immutability per the Go best-practices guide.
type Key struct {
	rawBytes  []byte
	algorithm KeyAlgorithm
	keyType   KeyType
}

func (k *Key) RawBytes() []byte {
	cp := make([]byte, len(k.rawBytes))
	copy(cp, k.rawBytes)
	return cp
}

func (k *Key) Algorithm() KeyAlgorithm {
	return k.algorithm
}

func (k *Key) KeyType() KeyType {
	return k.keyType
}
