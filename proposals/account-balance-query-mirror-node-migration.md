# MirrorNodeAccountBalanceQuery

## Summary

`AccountBalanceQuery` currently retrieves HBAR balances by issuing a free gRPC call to a consensus node (`CryptoService/cryptoGetBalance`). The Hedera network is deprecating this endpoint. This proposal introduces a replacement query, `MirrorNodeAccountBalanceQuery`, that fetches HBAR balance from the mirror node REST API (`GET /api/v1/balances?account.id={id}`), and simultaneously deprecates `AccountBalanceQuery`.

The new class follows the naming and structural convention already established in the SDKs for mirror node REST queries (`MirrorNodeContractCallQuery`, `MirrorNodeContractEstimateQuery`). The existing `AccountBalanceQuery` is marked deprecated but not removed, giving developers a migration window. A future `BlockNodeAccountBalanceQuery` is noted as a named placeholder for when block node infrastructure is available across the network.

**Date Submitted:** 2026-07-15

**Related references:**
- [Hedera blog: Migrating from AccountBalanceQuery](https://hedera.com/blog/migrating-from-accountbalancequery-what-you-need-to-know/)
- [Mirror node REST docs: GET /api/v1/balances](https://docs.hedera.com/hedera/sdks-and-apis/rest-api#balances)
- [Follow-on proposal: MirrorNodeTokenBalanceQuery](./mirror-node-token-balance-query.md)

---

## New APIs

### `MirrorNodeAccountBalance`

A new read-only data class returned by `MirrorNodeAccountBalanceQuery`. Returns HBAR balance only; token balances are not included (see Token Balances below).

```
@@finalType
MirrorNodeAccountBalance {
    @@immutable hbars: Hbar
}
```

### `MirrorNodeAccountBalanceQuery`

A new standalone query class that fetches HBAR balance from the mirror node REST API. Does not extend the base `Query` class (which carries consensus-node gRPC machinery). Follows the same pattern as `MirrorNodeContractCallQuery`.

`setAccountId` accepts `shard.realm.num`, EVM address (`0x...`), or public key alias — all resolved natively by the mirror node. Contract IDs are also accepted as the balances endpoint supports them; no separate `setContractId` method is needed.

```
MirrorNodeAccountBalanceQuery {
    @@nullable accountId: AccountId

    MirrorNodeAccountBalanceQuery setAccountId(accountId: AccountId)

    @@async
    @@throws(INVALID_ACCOUNT_ID)
    MirrorNodeAccountBalance execute(client: Client)
}
```

### Future placeholder: `BlockNodeAccountBalanceQuery`

A `BlockNodeAccountBalanceQuery` class is reserved for a future proposal once block node infrastructure is broadly available. Its public API is expected to mirror `MirrorNodeAccountBalanceQuery`. No implementation is defined here.

---

## Updated APIs

### `AccountBalanceQuery` — deprecated

`AccountBalanceQuery` is marked deprecated across all SDKs. Its implementation and behavior are unchanged — it continues to issue gRPC calls to consensus nodes for the duration of the deprecation window. No fields or methods are removed. SDKs should emit a deprecation warning on construction pointing developers to `MirrorNodeAccountBalanceQuery`.

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

### `MirrorNodeAccountBalanceQuery` implementation

The class does not extend `Query`. It uses `fetch` (or the SDK's equivalent HTTP abstraction) and `client.mirrorRestApiBaseUrl`, following the same structure as `MirrorNodeContractCallQuery` and `FeeEstimateQuery`.

**Endpoint:** `GET /api/v1/balances?account.id={accountId}`

The `account.id` parameter accepts `shard.realm.num`, EVM address, public key alias, and contract ID — the mirror node resolves all forms. Check `balances.length` first (see Non-existent account below); if non-empty, parse `balances[0].balance` (tinybars) → `MirrorNodeAccountBalance.hbars`.

**Mirror response shape — account exists:**

```json
{
  "timestamp": "1234567890.000000000",
  "balances": [
    {
      "account": "0.0.12345",
      "balance": 123456789
    }
  ],
  "links": { "next": null }
}
```

**Mirror response shape — account does not exist:**

```json
{
  "timestamp": "1234567890.000000000",
  "balances": [],
  "links": { "next": null }
}
```

### Non-existent account

The `/api/v1/balances` endpoint returns HTTP 200 with an empty `balances` array when the account does not exist — there is no 404. The two cases are distinguishable:

| Mirror node response | Meaning |
|---|---|
| `balances: []` | Account does not exist |
| `balances: [{ balance: 0 }]` | Account exists, genuine zero HBAR balance |

`MirrorNodeAccountBalanceQuery` must treat an empty `balances` array as an error condition — it throws an SDK-appropriate `INVALID_ACCOUNT_ID` error. A real account with a zero balance returns `hbars = 0` without error. No additional network call is required; the distinction is made from the same response.

**Pseudocode:**
```
if balances.length == 0:
    throw INVALID_ACCOUNT_ID
return MirrorNodeAccountBalance(hbars: balances[0].balance)
```

This restores the error-signalling behaviour that callers previously relied on from `AccountBalanceQuery` (which threw `INVALID_ACCOUNT_ID` for non-existent accounts via the consensus node path).

### Eventual consistency

The mirror node reflects network state with a small lag (typically seconds). Applications that require immediate post-transaction balance confirmation should allow for this lag. This should be documented in SDK release notes.

### Free query — no change

Both `AccountBalanceQuery` and `MirrorNodeAccountBalanceQuery` are free. No payment logic is involved in either path.

### Response Codes

No consensus node response codes apply to `MirrorNodeAccountBalanceQuery`. Mirror node HTTP errors:

- `200 OK, balances: []` — account does not exist. Surface as `INVALID_ACCOUNT_ID`. Do not retry.
- `400 Bad Request` — invalid ID format. Surface as an SDK-appropriate error. Do not retry.
- `500 / 503 / 504` — transient mirror node error. Retry with the same backoff policy used by `FeeEstimateQuery`.

#### Retry Policy

Mirror node retries follow existing mirror REST retry policy (`isRetryableNetworkError`): retry on 500/503/504 and network-level failures; do not retry on 400.

---

## Test Plan

Tests apply to `MirrorNodeAccountBalanceQuery` unless otherwise noted.

1. Given a valid account ID with a non-zero HBAR balance, when `MirrorNodeAccountBalanceQuery` is executed, then `MirrorNodeAccountBalance.hbars` matches the account's current HBAR balance as returned by the mirror node.
2. Given a valid account ID expressed as an EVM address, when `MirrorNodeAccountBalanceQuery` is executed, then the query resolves correctly and returns the HBAR balance.
3. Given a valid account ID expressed as a public key alias, when `MirrorNodeAccountBalanceQuery` is executed, then the query resolves correctly and returns the HBAR balance.
4. Given a valid contract ID passed as `accountId`, when `MirrorNodeAccountBalanceQuery` is executed, then `MirrorNodeAccountBalance.hbars` reflects the contract's current HBAR balance.
5. Given a non-existent account ID, when `MirrorNodeAccountBalanceQuery` is executed, then an `INVALID_ACCOUNT_ID` error is thrown (mirror node returns 200 with empty `balances` array).
6. Given a valid account that holds exactly 0 HBAR, when `MirrorNodeAccountBalanceQuery` is executed, then `MirrorNodeAccountBalance.hbars` is zero and no error is thrown (mirror node returns `[{ balance: 0 }]`, not an empty array).
7. Given a malformed account ID string, when `MirrorNodeAccountBalanceQuery` is executed, then the SDK throws an error before making a network call.
8. Given a mirror node that returns a transient 503 error on the first attempt, when `MirrorNodeAccountBalanceQuery` is executed, then the SDK retries and returns the correct result on a subsequent attempt.
9. Given a call to the deprecated `AccountBalanceQuery`, when it is constructed, then the SDK emits a deprecation warning directing the developer to `MirrorNodeAccountBalanceQuery`.
10. Given a call to the deprecated `AccountBalanceQuery`, when it is executed, then it returns a correct result via the consensus node gRPC path (no behavioral regression during the deprecation window).

### TCK

Tests 1–8 should each have a corresponding issue in `hiero-ledger/hiero-sdk-tck`. Tests 2 and 3 exercise identifier formats not covered by the legacy consensus-node path and should be prioritized.

---

## SDK Example

### HBAR balance (new API)

```javascript
import { MirrorNodeAccountBalanceQuery, Client } from "@hiero-ledger/sdk";

const client = Client.forTestnet();

const balance = await new MirrorNodeAccountBalanceQuery()
    .setAccountId("0.0.12345")
    .execute(client);

console.log(`HBAR balance: ${balance.hbars.toString()}`);
```

### Lookup by EVM address or alias

```javascript
// EVM address
const evmBalance = await new MirrorNodeAccountBalanceQuery()
    .setAccountId("0x0000000000000000000000000000000000bc614e")
    .execute(client);

// Public key alias
const aliasBalance = await new MirrorNodeAccountBalanceQuery()
    .setAccountId("0.0.302a300506032b6570032100e0c8ec2758a5879ffac226a13c0c516b799e72e35141a905d7822d6526b870d")
    .execute(client);
```

### Contract HBAR balance

```javascript
// Pass a contract ID via setAccountId — the balances endpoint supports contract IDs
const balance = await new MirrorNodeAccountBalanceQuery()
    .setAccountId("0.0.98765")
    .execute(client);

console.log(`Contract HBAR balance: ${balance.hbars.toString()}`);
```

### Migration from `AccountBalanceQuery`

```javascript
// Before — deprecated
import { AccountBalanceQuery } from "@hiero-ledger/sdk";
const balance = await new AccountBalanceQuery()  // emits deprecation warning
    .setAccountId(accountId)
    .execute(client);

// After
import { MirrorNodeAccountBalanceQuery } from "@hiero-ledger/sdk";
const balance = await new MirrorNodeAccountBalanceQuery()
    .setAccountId(accountId)
    .execute(client);

console.log(balance.hbars.toString());
```

---

## Token Balances

Token balance retrieval is intentionally out of scope for this class. `MirrorNodeAccountBalanceQuery` returns HBAR balance only. Token balances require pagination against a separate endpoint and are addressed in the follow-on proposal:

**[MirrorNodeTokenBalanceQuery](./mirror-node-token-balance-query.md)** — fetches token balances from `GET /api/v1/accounts/{id}/tokens`, returning one page of up to 100 token relationships per `execute()` call with caller-controlled pagination.
