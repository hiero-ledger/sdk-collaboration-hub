# Persistent Realm and Shard Support in Client for Address Book Queries

**Date Submitted**: 2025-06-03

## Summary

This proposal builds on the functionality introduced in the “Non-Zero Realm and Shard File IDs for Static Files” design by enhancing the `Client` behavior in Hiero SDKs. Specifically, it ensures persistent storage and use of `realm` and `shard` values within the `Client` instance. The original proposal enabled non-zero realm/shard usage but failed to persist these values in the client lifecycle, causing network inconsistency during dynamic operations like address book refreshes.

This update introduces a new type, `MirrorNetworkConfiguration`, to be used with `Client.forMirrorNetwork()`. This type is derived from `ClientConfiguration` but omits the `network` and `mirrorNetwork` properties—since `forMirrorNetwork()` determines these internally based on the queried address book. The previously proposed overload of `forMirrorNetwork(mirrorNetwork, realm, shard)` will be **scheduled for deprecation** in favor of this clearer, more maintainable approach.

---

## New APIs

### `BaseClientConfiguration`

The base configuration object with shared properties from `ClientConfiguration` is introduced for client factory methods used to instantiate a `Client` and is extended to support `realm` and `shard`, both defaulting to `0` if unspecified.

```typescript
/**
 * @typedef {object} BaseClientConfiguration
 * @property {{[key: string]: (string | AccountId)} | string} network
 * @property {string[] | string} [mirrorNetwork]
 * @property {Operator} [operator]
 * @property {boolean} [scheduleNetworkUpdate]
 * @property {number} [realm] - Defaults to 0
 * @property {number} [shard] - Defaults to 0
 */
```

### `getRealm(): number`

- Returns the currently stored `realm` value used for address book queries.
  - Example:
    ```javascript
    const realm = client.getRealm();
    ```

### `getShard(): number`

- Returns the currently stored `shard` value used for address book queries.
  - Example:
    ```javascript
    const shard = client.getShard();
    ```

### `setRealm(realm: number): Client`

- Sets the `realm` for future address book queries.
  - Example:
    ```javascript
    client.setRealm(1);
    ```

### `setShard(shard: number): Client`

- Sets the `shard` for future address book queries.
  - Example:
    ```javascript
    client.setShard(2);
    ```

### `MirrorNetworkConfiguration`

Defines a configuration type for `Client.forMirrorNetwork()` calls that excludes properties resolved internally (`network`, `mirrorNetwork`).

```typescript
/**
 * @typedef {Omit<ClientConfiguration, "network" | "mirrorNetwork">} MirrorNetworkConfiguration
 */
```

---

## Updated APIs

### `ClientConfiguration` (Updated)

The `ClientConfiguration` object now extends the `BaseClientConfiguration` which includes `realm` and `shard` support, both defaulting to `0` if unspecified.

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
  - `config`: A config object derived from `ClientConfiguration`, omitting `network` and `mirrorNetwork`.

```javascript
const client = Client.forMirrorNetwork(
  ["https://testnet.mirrornode.hedera.com"],
  {
    scheduleNetworkUpdate: true,
    realm: 1,
    shard: 2,
  }
);
```

### Additional support for realm and shard in factory methods

The following factory methods should also be confirmed to support `realm` and `shard` as part of their `ClientConfiguration`:

- `Client.fromConfig(data: string | ClientConfiguration): Client`
- `Client.fromConfigFile(filename: string): Promise<Client>`
- `Client.forNetwork(network: {[key: string]: (string | AccountId)}, props?: ClientConfiguration): Client`
- `Client.forName(network: string, props?: ClientConfiguration): Client`
- `Client.forMainnet(props?: ClientConfiguration): Client`
- `Client.forTestnet(props?: ClientConfiguration): Client`
- `Client.forPreviewnet(props?: ClientConfiguration): Client`

These methods already accept a `ClientConfiguration` object and should pass the `realm` and `shard` values into the client constructor for persistence and usage in all future address book queries.

### Deprecation Notice

The previously proposed overload:

```javascript
Client.forMirrorNetwork(mirrorNetwork, (realm = 0), (shard = 0));
```

will be **scheduled for deprecation**. Users are encouraged to adopt the `ClientConfiguration` approach going forward for clarity and consistency. This new API pattern aligns with modern configuration best practices and avoids ambiguity in usage.

---

## SDK Example (JavaScript)

### Using `forMirrorNetwork`

```javascript
const realm = parseInt(process.env.REALM);
const shard = parseInt(process.env.SHARD);
const mirrorNodeUrl = process.env.MIRROR_NODE_URL;

const client = Client.forMirrorNetwork([mirrorNodeUrl], {
  realm,
  shard,
  scheduleNetworkUpdate: true,
});

console.log("Initial Realm:", client.getRealm());
console.log("Initial Shard:", client.getShard());

// Update values at runtime
client.setRealm(1);
client.setShard(2);

// Perform a network update using the updated context
await client.updateNetwork();
```

### Using `fromConfig`

```javascript
const client = Client.fromConfig({
  network: "testnet",
  operator: myOperator,
  realm: 2,
  shard: 1,
  scheduleNetworkUpdate: true,
});
```

### Using `forMainnet`

```javascript
const client = Client.forMainnet({
  operator: myOperator,
  realm: 1,
  shard: 3,
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
  "realm": 5,
  "shard": 2
}
```

```javascript
const client = await Client.fromConfigFile("config.json");

console.log(client.getRealm()); // 5
console.log(client.getShard()); // 2
```

---

## Compatibility

- Fully backward compatible.
- `forMirrorNetwork(mirrorNetwork)` and the deprecated `(mirrorNetwork, realm, shard)` overload will continue to work for now.
- All new functionality is additive and encourages cleaner, type-safe configuration practices for users operating in non-default network contexts.

---

## Conclusion

This enhancement finalizes support for persistent realm and shard values in the Hiero SDK `Client`. The introduction of `MirrorNetworkConfiguration` promotes cleaner usage while scheduling deprecation of redundant overloads. These updates ensure better consistency, maintainability, and long-term compatibility for applications operating across diverse Hedera network topologies.
