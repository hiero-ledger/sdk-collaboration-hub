# Persistent Shard and Realm Support in Client for Address Book Queries

**Date Submitted**: 2025-06-03

## JavaScript SDK
## Summary

This proposal builds on the functionality introduced in the “Non-Zero Realm and Shard File IDs for Static Files” design by enhancing the `Client` behavior in Hiero SDKs. Specifically, it ensures persistent storage and use of `shard` and `realm` values within the `Client` instance. The original proposal enabled non-zero shard/realm usage but failed to persist these values in the client lifecycle, causing network inconsistency during dynamic operations like address book refreshes.

This update introduces a new type, `BaseNetworkConfiguration`, to be used with `Client.forMirrorNetwork()`. This type is derived from `ClientConfiguration` but omits the `network` and `mirrorNetwork` properties—since `forMirrorNetwork()` determines these internally based on the queried address book. 
---

## New APIs

### `BaseClientConfiguration`

The base configuration object with shared properties from `ClientConfiguration` is introduced for client factory methods used to instantiate a `Client` and is extended to support `shard` and `realm`, both defaulting to `0` if unspecified.

```typescript
/**
 * @typedef {object} BaseClientConfiguration
 * @property {{[key: string]: (string | AccountId)} | string} network
 * @property {string[] | string} [mirrorNetwork]
 * @property {Operator} [operator]
 * @property {boolean} [scheduleNetworkUpdate]
 * @property {number} [shard] - Defaults to 0
 * @property {number} [realm] - Defaults to 0
 */
```
### `getShard(): number`

- Returns the currently stored `shard` value used for address book queries.
  - Example:
    ```javascript
    const shard = client.getShard();
    ```

### `getRealm(): number`

- Returns the currently stored `realm` value used for address book queries.
  - Example:
    ```javascript
    const realm = client.getRealm();
    ```

---

## Updated APIs

### `ClientConfiguration` (Updated)

The `ClientConfiguration` object now extends the `BaseClientConfiguration` which includes `shard` and `realm` support, both defaulting to `0` if unspecified.

```typescript
/**
 * @typedef {BaseClientConfiguration & {
 *   network?: {[key: string]: (string | AccountId)} | string,
 *   mirrorNetwork?: string[] | string
 * }} ClientConfiguration
 */
```

### `Client.forMirrorNetwork(mirrorNetwork: string[], config: BaseClientConfiguration): Client`

- Accepts:
  - `mirrorNetwork`: An array of mirror node URLs.
  - `props`: A config object derived from `BaseClientConfiguration`

```javascript
const client = Client.forMirrorNetwork(
  ["https://testnet.mirrornode.hedera.com"],
  {
    scheduleNetworkUpdate: true,
    shard: 2,
    realm: 1
  }
);
```

### Additional support for shard and realm in factory methods

The following factory methods should also be confirmed to support `shard` and `realm` as part of their `ClientConfiguration`:

- `Client.fromConfig(data: string | ClientConfiguration): Client`
- `Client.fromConfigFile(filename: string): Promise<Client>`
- `Client.forNetwork(network: {[key: string]: (string | AccountId)}, props?: BaseClientConfiguration): Client`
- `Client.forName(network: string, props?: BaseClientConfiguration): Client`
- `Client.forMainnet(props?: BaseClientConfiguration): Client`
- `Client.forTestnet(props?: BaseClientConfiguration): Client`
- `Client.forPreviewnet(props?: BaseClientConfiguration): Client`

These methods already accept a `BaseClientConfiguration` object and should pass the `shard` and `realm` values into the client constructor for persistence and usage in all future address book queries.

---

## SDK Example (JavaScript)

### Using `forMirrorNetwork`

```javascript
const shard = parseInt(process.env.SHARD);
const realm = parseInt(process.env.REALM);
const mirrorNodeUrl = process.env.MIRROR_NODE_URL;

const client = Client.forMirrorNetwork([mirrorNodeUrl], {
  shard,
  realm,
  scheduleNetworkUpdate: true,
});

console.log("Initial Shard:", client.getShard());
console.log("Initial Realm:", client.getRealm());

// Update values at runtime
client.setShard(2);
client.setRealm(1);

// Perform a network update using the updated context
await client.updateNetwork();
```

### Using `fromConfig`

```javascript
const client = Client.fromConfig({
  network: "testnet",
  operator: myOperator,
  shard: 1,
  realm: 2
  scheduleNetworkUpdate: true,
});
```

### Using `forMainnet`

```javascript
const client = Client.forMainnet({
  operator: myOperator,
  shard: 3,
  realm: 1
});
```

### Using `fromConfigFile`

`config.json`:

```json
{
  "network": "previewnet",
  "operator": {
    "accountId": "0.0.1234",
    "privateKey": "302e020100300506032b657004220420..."
  },
  "shard": 2,
  "realm": 5
}
```

```javascript
const client = await Client.fromConfigFile("config.json");

console.log(client.getShard()); // 2
console.log(client.getRealm()); // 5
```

---

## Compatibility

- Fully backward compatible.
- All new functionality is additive and encourages cleaner, type-safe configuration practices for users operating in non-default network contexts.

## Other SDKs
## Summary

For SDKs outside of JavaScript (e.g., Java, Go, C++, etc.), persistent support for shard and realm will be added via method overloading in their respective factory methods.

## Expected Changes
Each SDK Client should:

Overload existing factory methods like forMainnet, forTestnet, fromConfig, etc.

Include variants that accept shard and realm parameters directly or via an overloaded configuration object.

Expose getShard() and getRealm() methods (or properties) as appropriate.