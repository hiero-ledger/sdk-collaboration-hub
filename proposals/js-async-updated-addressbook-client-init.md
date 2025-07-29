# Hiero SDK Design Doc: Async Client Initialization with Updated Address Book

**Date Submitted:** 2025-07-29

## Summary

This proposal introduces a new set of asynchronous static methods to the `WebClient` and `NodeClient` classes in the `hiero-js-sdk` that initialize a Hedera client with the latest address book fetched from the network. The goal is to improve stability and reliability by ensuring that newly instantiated clients use the most up-to-date node information when making transactions.

This enhancement avoids breaking existing synchronous APIs by introducing new `Async`-suffixed methods (e.g., `forMainnetAsync()`) instead of overloading existing methods with dynamic options.

These async methods will perform the address book update by default unless the `scheduleNetworkUpdate` configuration is explicitly set to `false`. This aligns with the goal of providing the freshest address book while still allowing opt-out behavior consistent with other SDKs.

This proposal addresses issues similar to those raised in: [sdk-collaboration-hub PR #23](https://github.com/hiero-ledger/sdk-collaboration-hub/pull/23).

## New APIs

### New API #1: `forMainnetAsync()`

- `static async forMainnetAsync(): Promise<WebClient>`
  - Constructs a WebClient pre-configured for Mainnet and performs `updateNetwork()`

### New API #2: `forTestnetAsync()`

- `static async forTestnetAsync(): Promise<WebClient>`
  - Same behavior as `forMainnetAsync()`, but for Testnet.

### New API #3: `forPreviewnetAsync()`

- `static async forPreviewnetAsync(): Promise<WebClient>`
  - Same behavior as above, for Previewnet.

### New API #4: `forNameAsync(network: string): Promise<WebClient>`

- Accepts a network name ("mainnet", "testnet", "previewnet"). If a local/unsupported name is passed, no network update will be triggered.

## Updated APIs

### Updated API: `WebClient.updateNetwork()`

- Will return the client wrapped in a promise e.g `Promise<WebClient>` to be consistent in the SDK with the builder pattern as until not it returned `Promise<void>`

## Test Plan

1. **Given** `WebClient.forMainnetAsync()` is called, **when** the client initializes, **then** `updateNetwork()` is executed and the client reflects the latest address book.
2. **Given** `WebClient.forTestnetAsync()` is called, **when** the client initializes, **then** no update is triggered and the client uses the static, built-in address book.
3. **Given** `WebClient.forNameAsync("mainnet")` is called, **when** a valid network is specified, **then** update behavior matches other methods.
4. **Given** `WebClient.forNameAsync("local")` is called, **then** no update is performed.
5. **Given** user uses `forMainnet()` (sync), **then** the existing behavior remains unchanged (no initial network update).

### TCK

Tests described above should be replicated in the [TCK repository](https://github.com/hiero-ledger/hiero-sdk-tck) to ensure platform-wide consistency.

## SDK Example

Examples should be added to the SDK codebase as part of this feature. In addition, corresponding examples and migration guidance should be included in the SDK documentation to help users adopt the new async initialization behavior. This ensures clarity for users transitioning from synchronous client instantiation to the enhanced, up-to-date model.

### Example 1: Recommended usage

```js
const client = await WebClient.forMainnetAsync();
// client now has the freshest node address book
```

### Example 2: Default (backward-compatible) usage

```js
const client = WebClient.forMainnet();
// client uses static address book, no async needed
```

### Example 3: Conditionally loaded network

```js
const client = await WebClient.forNameAsync("previewnet");
```

## Conclusion

This design preserves full backward compatibility while allowing users who opt in to enjoy more reliable and up-to-date client behavior.
