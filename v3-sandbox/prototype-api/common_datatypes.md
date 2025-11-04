# Keys

This section defines the common datatypes that are used in the API.

```
enum HbarUnit {
    TINYBAR
    MICROBAR
    MILLIBAR
    HBAR
    KILOBAR
    MEGABAR
    GIGABAR
    
    @immutable symbol: String
    @immutable tinybars: int64 
}

Hbar {
    @immutable value: int64 
    @immutable unit: HbarUnit
}

Ledger {
    @immutable id: bytes
}

@abstraction
Address {
    @immutable shard: uint64
    @immutable realm: uint64
    @immutable num: uint64
    @immutable checksum: string
    boolean validateChecksum(ledger:Ledger)
}

```
