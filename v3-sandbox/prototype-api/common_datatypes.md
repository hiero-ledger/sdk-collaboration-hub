# Keys

This section defines the common datatypes that are used in the API.

```
namespace common

// Definition of different units of Hbar
enum HbarUnit {
    TINYBAR  // tℏ
    MICROBAR // μℏ
    MILLIBAR // mℏ
    HBAR     // ℏ
    KILOBAR  // kℏ
    MEGABAR  // Mℏ
    GIGABAR  // Gℏ
    
    @immutable symbol: String // symbol of the unit
    @immutable tinybars: int64  // number of tinybars in one unit
}

// Hbar is a wrapper around int64 that represents a amount of Hbar based on a given unit.
Hbar {
    @immutable amount: int64 // amount in the given unit
    @immutable unit: HbarUnit // unit of the amount
}

HBarExchangeRate {
    @immutable expirationTime: zonedDateTime // expiration time of the exchange rate
    @immutable exchangeRateInUsdCents: double // exchange rate of HBar in USD cents
}

// Represents a specific ledger instance
Ledger {
    @immutable id: bytes // identifier of the ledger
}

// Represents the base of an address on the Hedera network.
@abstraction
Address {
    @immutable shard: uint64 // shard number
    @immutable realm: uint64 // realm number
    @immutable num: uint64 // account number
    @immutable checksum: string // checksum of the address
    boolean validateChecksum(ledger: Ledger) // validates the checksum of the address
    string toString() // returns address in format "shard.realm.num"
    string toStringWithChecksum() // returns address in format "shard.realm.num-checksum"
}

AccountId extends Address {
}

// factory methods of AccountId that should be added to the namespace in the best language dependent way

AccountId fromString(accountId: string)
```

## Questions

@hendrikebbers: Should we rename `Ledger` to `Network`?