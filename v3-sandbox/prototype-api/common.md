# Common API

This section defines the common datatypes that are used in the API.

## Description

This API defines the common datatypes that are used in the API.
All definitions must not depend on any other API.
The API defined in this layer can be shared between SDKs for consensus node, mirror node and block node.

## API Schema

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
    
    @@immutable symbol: string // symbol of the unit
    @@immutable tinybars: int64  // number of tinybars in one unit
    
    static list<HbarUnit> values()  // returns all HbarUnit values
}

// Hbar is a wrapper around int64 that represents a amount of Hbar based on a given unit.
Hbar {
    @@immutable amount: int64 // amount in the given unit
    @@immutable unit: HbarUnit // unit of the amount
    
    // Convert this Hbar to a different unit
    Hbar to(targetUnit: HbarUnit)
    
    // Get total amount in tinybars
    int64 toTinybars()
}

// Represents the exchange rate of Hbar in USD cents.
HBarExchangeRate {
    @@immutable expirationTime: zonedDateTime // expiration time of the exchange rate
    @@immutable exchangeRateInUsdCents: double // exchange rate of HBar in USD cents
    
    // Check if this exchange rate has expired
    // returns true if current time is past expirationTime
    bool isExpired()
}

// Represents a specific network instance (e.g. Hedera mainnet, Hedera testnet, a custom Hiero network)
Network {
    @@immutable id: bytes // identifier of the network, as returned by the consensus node
    @@immutable @@nullable name: string // human readable name of the network
}

// Represents a consensus node on a network.
ConsensusNode {
    @@immutable ip: string // ip address of the node
    @@immutable port: uint16 // port of the node
    @@immutable AccountId account // account of the node
}

// Represents a mirror node on a network.
MirrorNode {
    @@immutable restBaseUrl: string // base url of the mirror node REST API (scheme://host[:port]/api/v1)
}

// Represents the base of an address on a network.
abstraction Address {
    @@immutable shard: uint64 // shard number
    @@immutable realm: uint64 // realm number
    @@immutable num: uint64 // account number
    @@immutable checksum: string // checksum of the address
    
    // Validates the checksum of the address
    bool validateChecksum(network: Network)
    
    // returns address in format "shard.realm.num"
    string toString()
    
    // returns address in format "shard.realm.num-checksum"
    string toStringWithChecksum()
}

// AccountId is the most common type of address on a network.
AccountId extends Address {
}

ContractId extends Address {
}

FileId extends Address {
}

// factory methods of AccountId that should be added to the namespace in the best language dependent way

// Parses AccountId from string format: "shard.realm.num" or "shard.realm.num-checksum"
// @@throws(illegal-format) if format is invalid, values are negative, or parsing fails
// Supports optional checksum suffix after dash
@@throws(illegal-format) AccountId fromString(accountId: string)
```

## Questions & Comments

- [@hendrikebbers](https://github.com/hendrikebbers): Should we rename `Ledger` to `Network`?
  **Resolved**: Renamed to `Network`. Developer experience is a V3 design principle, and "Network" is immediately
  understood by any developer regardless of DLT background. It is also consistent with the naming already used
  in the `config` namespace (`NetworkSetting`, `getNetworkSetting`, `registerNetworkSetting`). All PoC authors
  referring to `common.Ledger` should update to `common.Network`.
- [@hendrikebbers](https://github.com/hendrikebbers): Do we want an abstraction for currency? HBAR is the only one for
  now and can be seen as Hedera specific.
- [@hendrikebbers](https://github.com/hendrikebbers): Do we want to have separate types for `AccountId`, `ContractId`
  and `FileId` or just one type `Address`?
  If we want separate types, should they all extend a common base type `Address`?