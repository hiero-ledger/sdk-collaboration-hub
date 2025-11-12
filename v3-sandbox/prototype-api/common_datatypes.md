# Keys

This section defines the common datatypes that are used in the API.

```
// Definition of different units of Hbar
enum HbarUnit {
    TINYBAR  // tℏ
    MICROBAR // μℏ
    MILLIBAR // mℏ
    HBAR     // ℏ
    KILOBAR  // kℏ
    MEGABAR  // Mℏ
    GIGABAR  // Gℏ
    
    @@immutable symbol: String // symbol of the unit
    @@immutable tinybars: int64  // number of tinybars in one unit
}

// Hbar is a wrapper around int64 that represents a amount of Hbar based on a given unit.
Hbar {
    @@immutable amount: int64 // amount in the given unit
    @@immutable unit: HbarUnit // unit of the amount
}

// Represents a specific ledger instance
Ledger {
    @@immutable id: bytes // identifier of the ledger
}

// Represents the base of an address on the Hedera network.
abstraction Address {
    @@immutable shard: uint64 // shard number
    @@immutable realm: uint64 // realm number
    @@immutable num: uint64 // account number
    @@immutable checksum: string // checksum of the address
    boolean validateChecksum(ledger:Ledger) // validates the checksum of the address
}
```

## Questions

Should we rename `Ledger` to `Network`?