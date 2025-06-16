# Persistent Shard and Realm Support in Client for Address Book Queries

**Date Submitted**: 2025-06-03

## Overview

This proposal enhances the `Client` behavior in Hiero SDKs to support persistent storage and usage of `shard` and `realm` values. It builds upon the "Non-Zero Realm and Shard File IDs for Static Files" design by ensuring these values persist throughout the client lifecycle, preventing network inconsistencies during operations like address book refreshes.

## Updated APIs

### Client Constructor and Configuration

The `Client` class will be enhanced to store and utilize shard and realm values throughout its lifecycle. These values can be used in network operations, including scheduled updates.

### `Client.forNetwork`
- Extracts shard and realm values from the provided consensus node network
- Validates that all nodes in the network share the same shard and realm values
- Persists these values in the client instance

### `Client.fromConfig`
Shard and realm values will be added into the expected json schema and will be persisted into the client.
Note that the SDK will assume these values are correct.

Example usage:
```json
{
    "network": {
		"35.242.233.154:50211": "3.5.10",
		"35.240.118.96:50211": "3.5.11",
    },
    "mirrorNetwork": "testnet",
    "shard": 3,
  	"realm": 5,
}
```

### New APIs

#### `getShard()`
```typescript
getShard(): number
```
Returns the currently stored shard value.

#### `getRealm()`
```typescript
getRealm(): number
```
Returns the currently stored realm value.

## Compatibility

This enhancement is fully backward compatible. All new functionality is additive and promotes type-safe configuration practices for non-default network contexts.

## Rejected Ideas

Modifying factory methods such as `forMainnet/Testnet/Previewnet/Name`, since they have predefined shard and realm.