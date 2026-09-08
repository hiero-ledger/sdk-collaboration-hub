# AccountBalanceQuery Deprecation

## Summary

The Hedera network is retiring the consensus node `CryptoService/cryptoGetBalance` endpoint used by `AccountBalanceQuery`. In consensus node release 77 the throttle for this endpoint will be reduced to zero, making all calls to it fail. The estimated schedule is testnet on **August 13, 2026** and mainnet on **September 9, 2026**; all dates are estimates and subject to change.

This proposal deprecates `AccountBalanceQuery` across all Hiero SDKs: the class emits a runtime warning on construction and throws a hard error on execution, directing developers to `MirrorNodeAccountBalanceQuery` or the mirror node REST API. Removal of the class is out of scope and will be addressed in a future proposal. Keeping the class present but non-functional preserves reversibility: if deprecation causes significant user churn, the consensus node path can be restored by reverting a single internal override with no public API changes.

### Delivery stages

**Stage 1 — Replace the `ping()` / `pingAll()` probe (before August 13, 2026)**
Replace the `AccountBalanceQuery` liveness probe inside `Client.ping()` and `Client.pingAll()` with a `COST_ANSWER` query. This is a purely internal change with no public API impact. It must ship before the release 77 testnet rollout to avoid breaking Solo readiness checks and connectivity tests.

**Stage 2 — Deprecate `AccountBalanceQuery` in the SDK (at or before September 9, 2026)**
Mark `AccountBalanceQuery` deprecated and override `execute()` to throw immediately. Stage 2 can ship independently of Stage 1 in any release, but Stage 1 must be complete first.

**Date Submitted:** 2026-07-15

**Target deprecation date:** September 9, 2026 — estimated mainnet rollout of consensus node release 77 (subject to change; see [hiero-ledger/hiero-consensus-node #26457](https://github.com/hiero-ledger/hiero-consensus-node/issues/26457))

**Related references:**
- [Hedera blog: Migrating from AccountBalanceQuery](https://hedera.com/blog/migrating-from-accountbalancequery-what-you-need-to-know/)
- [Companion proposal: Mirror node account balance query](./account-balance-query-mirror-node-migration.md)
- [Network node health report proposal](./network-node-health-report.md) — standardises `ping()` / `pingAll()` across SDKs
- Precedent in JS SDK: `src/account/AccountAllowanceAdjustTransaction.js`

---

## New APIs

None.

---

## Updated APIs

### `AccountBalanceQuery`

No fields or methods added or removed. The class is marked deprecated in each SDK's idiomatic way and `execute()` now throws.

```
@@oneOrNoneOf(accountId, contractId)
AccountBalanceQuery {
    @@nullable accountId: AccountId
    @@nullable contractId: ContractId

    AccountBalanceQuery setAccountId(accountId: AccountId)
    AccountBalanceQuery setContractId(contractId: ContractId)

    @@async
    @@throws(deprecated-query-error)
    AccountBalance execute(client: Client)
}
```

---

## Internal Changes

### Constructor — deprecation warning

Each SDK emits its idiomatic deprecation warning on construction (JS precedent: `console.warn`, as used by `Executable.setMaxRetries`). The message is the same one `execute()` throws with, so a developer sees one string wherever the query surfaces:

```
Deprecated: AccountBalanceQuery is no longer supported. Use MirrorNodeAccountBalanceQuery or the mirror node REST API (GET /api/v1/accounts/{id}) to retrieve account balances.
```

The message names the SDK replacement first because every SDK now ships `MirrorNodeAccountBalanceQuery` (see the companion proposal); the REST endpoint is kept for developers who call the mirror node directly.

Each SDK also marks the class with its idiomatic annotation (JSDoc `@deprecated`, Java `@Deprecated`, Go `// Deprecated:`) so IDEs and linters surface a diagnostic at every call site.

### Execute — hard error

The internal execution path is overridden to fail immediately without making any network call (JS precedent: `AccountAllowanceAdjustTransaction` overriding `_execute()`), rejecting with the same message as the constructor warning:

```
Error: Deprecated: AccountBalanceQuery is no longer supported. Use MirrorNodeAccountBalanceQuery or the mirror node REST API (GET /api/v1/accounts/{id}) to retrieve account balances.
```

### `Client.ping()` / `Client.pingAll()` — Stage 1

All SDKs must replace the `AccountBalanceQuery` probe inside `ping()` and `pingAll()` with a `CryptoService/getAccountInfo` request for account `0.0.2` with `ResponseType = COST_ANSWER`. Setting `COST_ANSWER` instructs the node to return the query fee without executing it — no HBAR is charged. A healthy node returns a cost response, which the probe treats as success. A node that is degraded or unreachable fails at the gRPC layer, which the probe treats as failure.

The standard gRPC health check protocol (`grpc.health.v1.Health/Check`) was evaluated and confirmed not implemented on Hedera consensus nodes (verified against 5 mainnet nodes, July 2026).

An XTS dry run (July 2025) confirmed that failing to make this replacement breaks Solo's consensus-node readiness checks. SDK teams should audit all internal uses of `AccountBalanceQuery` — not only the public `ping()` path.

### Response Codes / Transaction Retry

Not applicable — no network request is made by `execute()`.

---

## Test Plan

### Stage 1 — Ping probe replacement (must pass before August 13, 2026)

1. Given a reachable node, when `client.ping(nodeId)` is called, then the probe sends `CryptoService/getAccountInfo` for account `0.0.2` with `ResponseType = COST_ANSWER` and not `CryptoService/cryptoGetBalance`.
2. Given a reachable node, when `client.ping(nodeId)` is called, then a cost response is returned and treated as success.
3. Given `client.pingAll()` is called, then all nodes are probed using `CryptoService/getAccountInfo` with `ResponseType = COST_ANSWER`.
4. Given a reachable node, when `client.ping(nodeId)` completes, then the node's backoff state is updated (existing ping behaviour is preserved).
5. Given Solo's readiness checks run after Stage 1 is deployed, then all connectivity tests pass.

#### TCK — Stage 1

Tests 1–4 should have corresponding issues in `hiero-ledger/hiero-sdk-tck` and must be validated against a live network. Test 1 is the critical integration check — it confirms the probe has switched. Test 5 is a Solo-level integration check and is not a TCK test.

### Stage 2 — AccountBalanceQuery deprecation (must pass before September 9, 2026)

1. Given an `AccountBalanceQuery` is constructed, then a deprecation warning is emitted containing `"AccountBalanceQuery is no longer supported"`.
2. Given `execute()` is called on an `AccountBalanceQuery`, then the call rejects with an error containing `"AccountBalanceQuery is no longer supported"`.
3. Given `execute()` is called on an `AccountBalanceQuery`, then no network call to any consensus node is made.
4. Given code migrated to `MirrorNodeAccountBalanceQuery` or the mirror node REST API, then no deprecation warning or error is emitted.

#### TCK — Stage 2

Tests 1–4 should have corresponding issues in `hiero-ledger/hiero-sdk-tck`. Test 3 is the critical integration check — it confirms the gRPC path is fully bypassed on execute. Stage 2 tests should not be run until Stage 1 tests pass.

---

## SDK Example

```javascript
// Stage 2 — warns on construction, throws on execute
import { AccountBalanceQuery } from "@hiero-ledger/sdk";
const query = new AccountBalanceQuery().setAccountId("0.0.12345"); // emits deprecation warning
await query.execute(client); // throws: "AccountBalanceQuery is no longer supported..."

// Replacement — query the mirror node REST API directly or via MirrorNodeAccountBalanceQuery
// GET /api/v1/accounts/0.0.12345
```
