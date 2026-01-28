# Opt-In Receipt Query Node Failover

**Date Submitted:** 2026-01-28

---

## Summary

This design proposes an opt-in mechanism to allow `getReceipt` queries to fail over to other nodes when the submitting node is unresponsive, while preserving the current safe-by-default behavior.

Today, receipt queries are pinned to the submitting node across SDKs. This guarantees correctness in rare edge cases but can lead to timeouts and degraded reliability in high-concurrency scenarios. In particular, when many transactions are submitted simultaneously, the submitting node can become temporarily unhealthy or overloaded. In such cases, receipt retrieval may fail repeatedly even though other nodes are capable of serving the receipt.

Discussion with the Consensus Node (CN) team clarified that the network’s primary protection mechanism is billability: only transactions that can be paid for (i.e., have a valid payer and payer signature) are propagated and sent to consensus. Non-billable transactions are rejected locally and are never propagated, so only the submitting node will ever have visibility into those failures. This is a deliberate design to prevent denial-of-service vectors.

This proposal introduces a client-level configuration flag that allows advanced users (e.g., relay providers and high-throughput workloads) to explicitly opt into receipt failover behavior. When enabled, receipt queries will still start with the submitting node first but may advance to other available nodes if the submitting node is unresponsive. This preserves strict correctness by default while giving users a controlled way to trade strict correctness for improved availability and reduced timeouts when appropriate.

---

## New APIs

### New API #1 — Client.allowReceiptNodeFailover

- `boolean allowReceiptNodeFailover` : Enables receipt query failover to other nodes when the submitting node is unresponsive.
    - Default: `false`
    - When `false`, preserves existing behavior (receipt queries pinned to submitting node only).
    - When `true`, receipt queries may iterate across multiple nodes, starting with the submitting node first.

#### Getter
- `boolean client.allowReceiptNodeFailover`

#### Setter
- `Client setAllowReceiptNodeFailover(boolean allow)`

**Description:**  
Controls whether receipt queries are allowed to fail over to other nodes when the submitting node is unavailable.

**Notes:**
- This is an opt-in feature intended for high-throughput and relay use cases.
- The default value is `false` to preserve existing correctness guarantees.

**Example:**
```js
client.setAllowReceiptNodeFailover(true);
```

---

## Updated APIs

### Updated API #1 — TransactionResponse.getReceiptQuery(client?)

- `TransactionReceiptQuery getReceiptQuery(Client client?)`

**Description:**  
Adds an optional `client` parameter to allow `getReceiptQuery()` to apply client-level receipt failover configuration.

**Behavior:**
- Without `client`, or when `client.allowReceiptNodeFailover === false`:
  - Receipt query is pinned to the submitting node only (existing behavior).
- With `client` and `client.allowReceiptNodeFailover === true`:
  - Receipt query can iterate across multiple nodes.
  - The submitting node is always tried first.
  - Additional nodes are appended based on transaction-specific nodes or client network nodes.

**Node Selection Logic (when failover enabled):**
1. Submitting node (always first)
2. Transaction-specific nodes (if `setNodeAccountIds()` was used)
3. Client network nodes (fallback)
4. No duplicates

**Rationale:**  
Allows receipt queries to be resilient to temporary submitting-node unavailability while preserving existing semantics by default.

---

## Internal Changes

### Client.js

- Add private field `_allowReceiptNodeFailover` (default: `false`)
- Add public getter and setter
- Add inline documentation explaining correctness vs availability tradeoff

### TransactionResponse.js

- Add internal `transactionNodeAccountIds?: AccountId[]`
- Constructor accepts optional `transactionNodeAccountIds`
- Update `getReceiptQuery(client?)` with node precedence and failover logic
- Update `getReceipt()` and `getVerboseRecord()` to pass `client` to `getReceiptQuery()`

### Transaction.js

- `_mapResponse()` passes `this._nodeAccountIds.list` to `TransactionResponse` constructor

### Executable.js

- Update unhealthy node handling:
  - Single-node receipt queries: Retry same node with backoff (existing behavior)
  - Multi-node receipt queries: Advance to next node on retry (enables actual failover)

---

## Test Plan

1. Given default client configuration, when `getReceipt(client)` is called, then the receipt query is pinned to the submitting node only.
2. Given `allowReceiptNodeFailover === true`, when the submitting node is healthy, then the submitting node is queried first.
3. Given `allowReceiptNodeFailover === true` and the submitting node returns transport/unavailable, when `getReceipt(client)` is retried, then the query advances to the next available node.
4. Given a transaction with `setNodeAccountIds()`, when failover is enabled, then receipt queries iterate over `[submittingNode, ...transactionNodes]`.
5. Given a transaction without explicit nodes, when failover is enabled, then receipt queries iterate over `[submittingNode, ...clientNetworkNodes]`.
6. Given default behavior, when submitting node is unhealthy, then receipt retries remain pinned to the submitting node.

### TCK

Corresponding TCK issues should be created to cover:
- Default pinned receipt behavior
- Opt-in receipt failover behavior
- Submitting node unavailable with failover enabled
- Node ordering guarantees (submitting node first)

---

## SDK Example

```js
// Enable receipt node failover
client.setAllowReceiptNodeFailover(true);

// Scenario 1: Transaction with specific nodes
const tx1 = new AccountCreateTransaction()
  .setNodeAccountIds([node3, node4, node5])
  .freezeWith(client);

const response1 = await tx1.execute(client);
const receipt1 = await response1.getReceipt(client);

// Receipt query uses:
// [submittingNode (e.g. node3), node4, node5]

// Scenario 2: Transaction with default client nodes
const tx2 = new AccountCreateTransaction().freezeWith(client);

const response2 = await tx2.execute(client);
const receipt2 = await response2.getReceipt(client);

// Receipt query uses:
// [submittingNode, ...allClientNetworkNodes]

// Scenario 3: Get query directly
const receiptQuery = response2.getReceiptQuery(client);
// Configured for failover and can be further customized if needed
```
