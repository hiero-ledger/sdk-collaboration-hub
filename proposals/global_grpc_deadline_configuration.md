# Hiero SDK Design Doc: Global gRPC Deadline Configuration

**Date Submitted:** 2025-10-17

## Summary

This design proposal introduces a **global `grpcDeadline` configuration** to the **`Client` class** of the Hiero SDKs. The purpose is to align timeout behavior across SDKs, improve consistency, and provide developers with more control over gRPC request handling.

Currently, the Java SDK supports a configurable per-request gRPC deadline via `Client.setGrpcDeadline(Duration)`, while other SDKs (e.g., JavaScript, Go, Rust...) require setting deadlines on individual transactions (e.g., `Transaction.setGrpcDeadline()`). The proposed design adds a **global client-level gRPC deadline property** on the `Client` class, providing a default timeout for all gRPC requests that can be overridden on a per-transaction basis.

This global deadline will also be applied during the **warmup RPC call (initial TCP handshake)** to ensure consistent timeout handling across both initialization and execution flows. The intention is to expose this as a configurable parameter so developers can fine-tune network behavior based on their latency and reliability requirements.

This change promotes consistent client behavior across SDKs, simplifies configuration, and enhances usability by allowing global control of network timeouts.

---

## New APIs

### New API #1: `Client.grpcDeadline`

- `number grpcDeadline` : Defines the maximum duration in milliseconds for a gRPC request before timing out.
  - Default: **10,000 milliseconds (10 seconds)**
  - Minimum value: **1,000 milliseconds (1 second)**
  - Optional property — if not set, the default value applies.
  - Added as a property of the **`Client` class** across all Hiero SDKs.

#### Methods

- `Client setGrpcDeadline(number grpcDeadlineMs)`  
  Sets the global gRPC deadline in milliseconds on the `Client` instance.

- `number getGrpcDeadline()`  
  Returns the currently configured global gRPC deadline in milliseconds.

#### Example Usage

```ts
const client = Client.forMainnet();
client.setGrpcDeadline(5000); // Set global gRPC deadline to 5 seconds

console.log(client.getGrpcDeadline()); // 5000
```

---

## Updated APIs

### Updated API #1: `Transaction`

- The existing `Transaction.setGrpcDeadline(number deadlineMs)` remains unchanged.
- The logic will be updated so that if no explicit `grpcDeadline` is set on the Transaction level, the **Client-level global `grpcDeadline`** will be used as the fallback.

#### Example Behavior

```ts
const client = Client.forTestnet();
client.setGrpcDeadline(8000);

const tx = new TransferTransaction()
  .addHbarTransfer(accountId, new Hbar(1))
  .addHbarTransfer(recipientId, new Hbar(-1));

// No tx-specific deadline set → uses client-level deadline (8000 ms)
await tx.execute(client);

// This transaction will override the global deadline
tx.setGrpcDeadline(2000);
await tx.execute(client); // Uses 2000 ms
```

---

## Internal Changes

1. **Warmup RPC Call**

   - The global `grpcDeadline` defined on the `Client` class will be used during the initial TCP handshake / warmup RPC call.
   - This ensures consistent timeout behavior during node connection initialization.

2. **Executable Class Integration**

   - The internal `Executable` base class (used by `Transaction`, `Query`, etc.) will use the global `grpcDeadline` as a fallback if no transaction-specific value is set.
   - The existing transaction-level deadline will **take precedence** over the client-level deadline.

3. **Default Configuration**

   - The default global gRPC deadline will be **10 seconds**, matching standard gRPC developer defaults for cross-SDK consistency.
   - This aligns the SDK’s network behavior with official gRPC expectations rather than introducing custom aggressive timeout values.

4. **Exposed Configurability**

   - Developers will be able to customize the timeout globally through `Client.setGrpcDeadline()` to accommodate network distance or latency tolerance.

5. **Browser Environment (JavaScript SDK)**
   - In the **JavaScript browser environment**, the SDK uses **gRPC Envoy proxies** to replicate behavior consistent with Node and other SDKs.
   - Before performing an actual gRPC request, the SDK will issue an **initial HTTP healthcheck request** to the proxy endpoint.
   - The healthcheck will have a **timeout equal to the configured `grpcDeadline` in milliseconds**.
   - The healthcheck result will be **cached per node address** so subsequent calls to the same node skip repeating it.
   - All other timeout and retry behavior will remain consistent with other SDKs.

---

### Response Codes

No new response codes are introduced as part of this change.

#### Transaction Retry

No new retry logic is introduced directly by this proposal. However, consistent timeout enforcement across SDKs will improve predictability of retry handling.

---

## Test Plan

1. **Given** a Client with no `grpcDeadline` set, **when** a Transaction executes, **then** it should use the default 10-second global deadline.
2. **Given** a Client with a custom `grpcDeadline` set, **when** a Transaction executes, **then** it should apply the configured deadline to all gRPC calls.
3. **Given** a Transaction with its own `grpcDeadline`, **when** it executes, **then** it should override the client-level deadline.
4. **Given** a warmup RPC call during Client initialization, **when** it exceeds the global `grpcDeadline`, **then** the SDK should abort the connection and mark the node as temporarily unhealthy.
5. **Given** a browser environment, **when** a healthcheck request times out, **then** the node should be marked unhealthy and the SDK should skip the transaction attempt.
6. **Given** multiple browser requests to the same node, **when** a cached healthcheck exists, **then** no duplicate healthcheck requests should be made.
7. **Given** network delays or simulated timeouts, **when** multiple SDKs (Java, JS, Go, etc.) perform the same operation, **then** behavior and timeout enforcement should be consistent across implementations.
8. **Given** an invalid (negative or zero) `grpcDeadline`, **when** the client is initialized, **then** an error or validation exception should occur.

---

**Labels:** `enhancement`, `timeout`, `cross-sdk`, `network`, `grpc`, `configuration`, `browser-support`
