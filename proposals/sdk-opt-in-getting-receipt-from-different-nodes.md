# Opt-In getting receipt from different nodes

**Date Submitted:** 2026-01-28

---

## Summary

Тhis design proposes an opt-in mechanism to allow `getReceipt` queries to fail over to other nodes when the submitting node is unresponsive.

Receipt query routing is not uniform across SDKs today. Several SDKs (JS, Java, Swift, C++, Go) construct getReceiptQuery() by explicitly pinning the query to the submitting node (e.g., setNodeAccountIds([submittingNode])), which preserves strict correctness for submitting-node-only edge cases but can increase the likelihood of timeouts when that node is temporarily unhealthy or overloaded under high concurrency.

Other SDKs (e.g., Python and Rust) do not currently pin receipt queries to the submitting node and instead rely on default client node selection. This may improve availability in some scenarios, but it does not provide the same strict “submitting-node-first / submitting-node-only” correctness semantics by default.

This proposal therefore needs to explicitly define the intended baseline behavior per SDK (or align SDKs to a shared baseline) and then introduce an opt-in mechanism to allow receipt queries to iterate across additional nodes while still starting from the submitting node when applicable.

The network’s primary protection mechanism is billability: only transactions that can be paid for (i.e., have a valid payer balance and payer signature) are propagated and sent to consensus. Non-billable transactions are rejected locally and are never propagated, so only the submitting node will ever have visibility into those failures. This is a deliberate design to prevent denial-of-service vectors.

This proposal introduces a client-level configuration flag that allows advanced users (e.g., relay providers and high-throughput workloads) to explicitly opt into receipt failover behavior. When enabled, receipt queries will still start with the submitting node first but may advance to other available nodes if the submitting node is unresponsive. This preserves strict correctness by default while giving users a controlled way to trade strict correctness for improved availability and reduced timeouts when appropriate.

---

## New APIs

### Client

```
// Enables opt-in receipt query failover to other nodes
Client {
    @@default(false)
    allowReceiptNodeFailover: bool

    Client setAllowReceiptNodeFailover(allow: bool)
    bool getAllowReceiptNodeFailover()
}
```

**Description:**

- `allowReceiptNodeFailover` controls whether receipt queries are allowed to advance to other nodes when the submitting node is unresponsive.
- Default value is `false` to preserve existing strict correctness semantics.
- When set to `true`, receipt queries may iterate across multiple nodes, starting with the submitting node first.

**Notes:**

- This flag is intended for advanced/high-throughput use cases (e.g., relay providers).
- Enabling this flag explicitly accepts the rare risk that certain submitting-node-only failures may not be observable from other nodes.

---

## Updated APIs

### TransactionResponse

```
// Represents the response returned after submitting a transaction
TransactionResponse {

    @@async
    TransactionReceiptQuery getReceiptQuery(@@nullable client: Client)

    @@async
    TransactionReceipt getReceipt(@@nullable client: Client)
}
```

**Updated Method: `getReceiptQuery(client)`**

**Behavior:**

- When `client` is `null` OR `client.allowReceiptNodeFailover == false`:
  - Receipt query is pinned to the submitting node only.

- When `client.allowReceiptNodeFailover == true`:
  - Receipt query may iterate across multiple nodes.
  - The submitting node is always tried first.
  - Additional nodes are appended based on transaction-specific nodes `([submittingNode, ...remainingTransactionNodes])` or client network nodes `(([submittingNode, ...clientNetworkNodes))`, where duplicates are removed by preserving the first occurrence.

**Updated Method: `getReceipt(client)`**

- Delegates to `getReceiptQuery(client)` and applies the same node selection and failover semantics.

**Notes:**

- This is a backwards-compatible change. Existing calls to getReceiptQuery() without arguments will continue to work with the current pinned behavior.

---

## Internal Changes

### Client

- Add a private/internal field to store the receipt failover configuration (default: `false`)
- Add public getter and setter for configuring receipt failover behavior
- Add inline/internal documentation explaining the correctness vs availability tradeoff

### TransactionResponse

- Store the node account IDs used during transaction execution
- Extend the constructor to accept optional transaction node account IDs
- Update `getReceiptQuery(client?)` to apply node precedence and optional failover logic
- Update `getReceipt(client?)` to pass the client context to receipt query construction

### Transaction

- Propagate the transaction’s configured node account IDs into the corresponding `TransactionResponse`

### Executable / Retry Handling

- Update unhealthy node handling for receipt queries:
  - Single-node receipt queries: Retry against the same node using existing backoff behavior
  - Multi-node receipt queries (opt-in): Advance to the next eligible node on retry to enable true failover


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

```
// Default behavior (failover disabled)

client.allowReceiptNodeFailover = false   // default

transaction = new AccountCreateTransaction()
transaction.freezeWith(client)

response = transaction.execute(client)
receipt = response.getReceipt(client)

// Receipt query behavior:
// - Uses submittingNode only
// - Retries against the same submittingNode on failure
// - Does NOT advance to other nodes


// With receipt failover enabled (opt-in)

client.allowReceiptNodeFailover = true

transaction2 = new AccountCreateTransaction()
transaction2.freezeWith(client)

response2 = transaction2.execute(client)
receipt2 = response2.getReceipt(client)

// Receipt query behavior:
// - Starts with submittingNode
// - On failure/unavailable, may advance to other eligible nodes
// - Node order: [submittingNode, ...otherClientNetworkNodes]


// With transaction-specific nodes configured

client.allowReceiptNodeFailover = true

transaction3 = new AccountCreateTransaction()
transaction3.setNodeAccountIds([txNodeA, txNodeB, txNodeC])
transaction3.freezeWith(client)

response3 = transaction3.execute(client)
receipt3 = response3.getReceipt(client)

// Receipt query behavior:
// - Starts with submittingNode (e.g., nodeA)
// - Then tries remaining transaction nodes
// - Node order: [submittingTxNode, ...restOfTxNodes]
```