# Client Initialization for Static Networks via Mirror Node Address Book (Async-First, Pre-v3)

**Date Submitted:** 2025-05-21

## Summary

This proposal describes a change in how Hiero SDK clients initialize static networks (for example, Hedera mainnet, testnet, and previewnet), and introduces **pre-v3 API alignment** to safely support this change.

Currently, Hiero SDKs initialize static networks using static files or hardcoded values that define node IPs, ports, and account IDs. After initialization, the SDKs typically wait approximately ten seconds before querying a mirror node to retrieve the most up-to-date address book. This approach has several drawbacks:

- Network topology changes require manual updates to SDK source files or bundled resources.
- The first window of client execution may use outdated or invalid node information, resulting in connection or execution errors.
- Delayed network refresh makes deterministic and async-safe client initialization difficult.

This proposal replaces static network bootstrapping with **immediate mirror node address book retrieval** and explicitly ties this behavior to **Async client factory methods**.

As part of this change:

- `forMainnet`, `forTestnet`, and `forPreviewnet` are **deprecated**.
- New or existing Async equivalents (`forMainnetAsync`, `forTestnetAsync`, `forPreviewnetAsync`) become the **authoritative initialization path**.
- Async factory methods will initialize the client using the **latest network state from the mirror node** before returning.

This work is fully backward compatible and is intended as **preparatory work prior to v3**, enabling future removal of deprecated synchronous methods and eventual simplification of client initialization APIs.

---

## New APIs

### New API #1 – Async Static Network Factory Methods

Each SDK must expose Async-suffixed factory methods for static network initialization (where they do not already exist):

- `Client.forMainnetAsync()`
- `Client.forTestnetAsync()`
- `Client.forPreviewnetAsync()`

#### Behavior

- These methods **must perform a mirror node address book query** during initialization.
- The consensus node network must be constructed directly from the retrieved address book.
- The method returns only after the client is fully initialized with the latest network topology.

#### Example (pseudo-code)

const client = await Client.forMainnetAsync();

---

## Updated APIs

### Updated API #1 – Deprecation of Synchronous Static Network Factories

The following synchronous client factory methods are deprecated:

- `Client.forMainnet()`
- `Client.forTestnet()`
- `Client.forPreviewnet()`

#### Deprecation Details

- Methods are marked with `@deprecated` (or language-equivalent).
- Documentation must point users to the Async alternatives.
- Runtime behavior remains unchanged until a future major release.

#### Rationale

- Synchronous initialization cannot safely perform network I/O required to retrieve the latest address book.
- Async factory methods provide a clear, explicit contract for network-dependent initialization.
- Deprecation allows these methods to be removed or renamed in v3 without violating semantic versioning.

#### Example Deprecation Annotation

```
/// @deprecated Use Client.forMainnetAsync(...) instead.
/// This method will be removed in a future major release.
Client.forMainnet()
```

---

## Internal Changes

### Current Behavior

1. Client initializes using static or hardcoded network data.
2. Client schedules a delayed (~10 second) mirror node query.
3. Network topology is updated after the delay.

### Proposed Behavior

1. Async factory method initializes mirror node network.
2. Async factory method immediately queries the mirror node address book.
3. Consensus node network is constructed from the retrieved address book.
4. Delayed network update mechanism is removed.

Example (illustrative, C++):

```cpp
Client Client::forMainnetAsync() {
    Client client;
    client.mImpl->setMirrorNetwork(internal::MirrorNetwork::forMainnet());
    client.mImpl->mNetwork =
        std::make_shared<internal::Network>(
            internal::Network::forNetwork(
                internal::Network::getNetworkFromAddressBook(
                    AddressBookQuery().execute(client),
                    internal::BaseNodeAddress::PORT_NODE_PLAIN)));
    return client;
}
```

SDKs that cannot safely change synchronous behavior (e.g., Go, C++) must introduce Async factory methods without altering existing synchronous implementations.

---

## Test Plan

1. **Async Factory Initializes from Mirror**

   - Given an uninitialized client,
     when `forMainnetAsync` is called,
     then the client network matches the mirror node address book at initialization.

2. **Deprecated Sync Factory Still Works**

   - Given `forMainnet` is called,
     when the client initializes,
     then initialization succeeds and a deprecation warning is emitted.

3. **No Delayed Network Update**

   - Given a client initialized via Async factory,
     when the client starts,
     then no scheduled delayed network refresh occurs.

4. **Latest Network Reflected Immediately**

   - Given recent network topology changes,
     when `forTestnetAsync` is called,
     then the client reflects the updated topology immediately.

5. **SDK Parity**
   - Given different SDK implementations,
     when Async factory methods are used,
     then client initialization behavior is consistent across SDKs.

---

## SDK Example

### Example – Recommended Async Initialization

```javascript
import { Client } from "@hiero-sdk-js";

async function main() {
  const client = await Client.forMainnetAsync();

  const info = await new NetworkVersionInfoQuery().execute(client);
  console.log(info);
}

main();
```
