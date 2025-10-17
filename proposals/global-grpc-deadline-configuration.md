# Global gRPC Deadline Configuration

**Date Submitted:** 2025-10-17

## Summary

This design proposal introduces a **global `grpcDeadline` configuration** to the **`Client` class** of the Hiero SDKs. The purpose is to align timeout behavior across SDKs, improve consistency, and provide developers with more control over gRPC request handling.

Currently, the Java SDK supports a configurable per-request gRPC deadline via `Client.setGrpcDeadline(Duration)`, while other SDKs (e.g., JavaScript, Go, Rust...) require setting deadlines on individual transactions (e.g., `Transaction.setGrpcDeadline()`). The proposed design adds a **global client-level gRPC deadline property** on the `Client` class, providing a default timeout for all gRPC requests that can be overridden on a per-transaction basis.

This global deadline will also be applied during the **warmup RPC call (initial TCP handshake)** to ensure consistent timeout handling across both initialization and execution flows. The intention is to expose this as a configurable parameter so developers can fine-tune network behavior based on their latency and reliability requirements.

Additionally, this proposal ensures that all SDKs consistently include the **`requestTimeout` property** on the `Client` class. This timeout governs the **overall execution duration** of a `Transaction` or `Query` (including retries, backoff, and node rotation). If not already implemented in a specific SDK, it should be added with a default value of **2 minutes (120,000 ms)**.

This change promotes consistent client behavior across SDKs, simplifies configuration, and enhances usability by allowing global control of both **per-request** and **overall operation** timeouts.

---

## New APIs

### New API #1: `Client.grpcDeadline`

- `number grpcDeadline` : Defines the maximum duration in milliseconds for a gRPC request before timing out.
  - Default: **10,000 milliseconds (10 seconds)**
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

### New API #2: `Client.requestTimeout`

- `number requestTimeout` : Defines the maximum duration in milliseconds for a **complete `Transaction` or `Query` execution operation**, including all retries and backoff logic.
  - Default: **120,000 milliseconds (2 minutes)**
  - Optional property — if not set, the default value applies.
  - Added to all SDKs where it does not already exist to ensure consistent timeout handling across the ecosystem.

#### Methods

- `Client setRequestTimeout(number requestTimeoutMs)`  
  Sets the maximum allowed time for a complete execution operation.

- `number getRequestTimeout()`  
  Returns the currently configured overall execution timeout in milliseconds.

#### Example Usage

```ts
const client = Client.forMainnet();
client.setRequestTimeout(60000); // 1 minute timeout for full execute() operation
console.log(client.getRequestTimeout()); // 60000
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
   - The default global request timeout will be **2 minutes**, defining the upper bound for the total execution lifecycle.
   - `requestTimeout` should always be **greater than or equal to** the configured `grpcDeadline` to ensure logical consistency.

4. **Exposed Configurability**

   - Developers can adjust both global timeouts (`grpcDeadline` and `requestTimeout`) via the `Client` API to fine-tune network tolerance.

5. **Browser Environment (JavaScript SDK)**
   - In the **JavaScript browser environment**, the SDK uses **gRPC Envoy proxies** to replicate behavior consistent with Node and other SDKs.
   - Before performing an actual gRPC request, the SDK will issue an **initial HTTP healthcheck request** to the proxy endpoint.
   - The healthcheck will have a **timeout equal to the configured `grpcDeadline` in milliseconds**.
   - The healthcheck result will be **cached per node address** so subsequent calls to the same node skip repeating it.
   - All other timeout and retry behavior will remain consistent with other SDKs.

---

## Test Plan

1. **Given** a Client with no `grpcDeadline` set, **when** a Transaction executes, **then** it should use the default 10-second global deadline.
2. **Given** a Client with a custom `grpcDeadline` set, **when** a Transaction executes, **then** it should apply the configured deadline to all gRPC calls.
3. **Given** a Transaction with its own `grpcDeadline`, **when** it executes, **then** it should override the client-level deadline.
4. **Given** a warmup RPC call during Client initialization, **when** it exceeds the global `grpcDeadline`, **then** the SDK should abort the connection and mark the node as temporarily unhealthy.
5. **Given** a browser environment, **when** a healthcheck request times out, **then** the node should be marked unhealthy and the SDK should skip the transaction attempt.
6. **Given** multiple browser requests to the same node, **when** a cached healthcheck exists, **then** no duplicate healthcheck requests should be made.
7. **Given** network delays or simulated timeouts, **when** multiple SDKs (Java, JS, Go, etc.) perform the same operation, **then** behavior and timeout enforcement should be consistent across implementations.
8. **Given** a `requestTimeout` smaller than `grpcDeadline`, **then** initialization should throw or adjust automatically to ensure logical ordering.
