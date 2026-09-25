# MirrorNodeTokenBalanceQuery

## Summary

`AccountBalanceQuery` originally returned HBAR balance and token balances together. Token balance support was deprecated in Services release 0.50 (HIP-367), and `AccountBalanceQuery` itself is deprecated and will be removed with consensus node release 77 (estimated mainnet September 9, 2026). `MirrorNodeAccountBalanceQuery` replaces the HBAR path but intentionally omits token balances — the mirror node `/api/v1/accounts/{id}/tokens` endpoint paginates, and fetching a full token portfolio implicitly inside a single `execute()` call would make the cost invisible to the caller.

This proposal introduces `MirrorNodeTokenBalanceQuery`: an SDK query for fetching token balances from the mirror node REST API. `execute()` returns one page of results (up to 100 tokens). Pagination is the caller's responsibility — the returned page exposes a cursor that the caller uses to fetch subsequent pages if needed. An optional `tokenId` filter narrows the result to a single token, eliminating pagination entirely.

This is a follow-on to `MirrorNodeAccountBalanceQuery` and should be implemented after that class ships in all SDKs.

**Date Submitted:** 2026-08-14

**Related references:**
- [Companion proposal: MirrorNodeAccountBalanceQuery](./account-balance-query-mirror-node-migration.md)
- [Companion proposal: AccountBalanceQuery Deprecation](./account-balance-query-deprecation.md)
- [Mirror node REST docs: GET /api/v1/accounts/{id}/tokens](https://docs.hedera.com/hedera/sdks-and-apis/rest-api#tokens-2)

---

## New APIs

### `MirrorNodeTokenBalance`

A read-only data class representing a single token relationship entry as returned by the mirror node. Fields map 1:1 to the mirror node `TokenRelationship` schema — no fields are filtered or projected. A developer using the SDK gets the same data as one calling the REST API directly.

```
@@finalType
MirrorNodeTokenBalance {
    @@immutable tokenId:                TokenId
    @@immutable balance:                Long      // raw units in the token's smallest denomination
    @@nullable @@immutable decimals:    int       // decimal places; null for NFTs
    @@nullable @@immutable automaticAssociation: Boolean
    @@nullable @@immutable createdTimestamp:     String    // e.g. "1234567890.000000000"
    @@nullable @@immutable freezeStatus:         String    // NOT_APPLICABLE | FROZEN | UNFROZEN
    @@immutable kycStatus:             String    // NOT_APPLICABLE | GRANTED | REVOKED
}
```

`balance` is the raw on-chain value. For fungible tokens, divide by `10^decimals` for a human-readable amount. For NFTs, `balance` is the count of NFTs held and `decimals` is null.

**Design note — consolidation of balance and decimals:**
The existing `AccountBalance` type split token data across two separate maps: `tokens: TokenBalanceMap` (balance keyed by `TokenId`) and `tokenDecimals: TokenDecimalMap` (decimals keyed by `TokenId`). `MirrorNodeTokenBalance` consolidates both into a single object per token alongside the full token relationship context. SDK teams implementing this type do not need to replicate the split-map pattern.

---

### `MirrorNodeTokenBalancePage`

A read-only page of token balance results. Returned by `MirrorNodeTokenBalanceQuery.execute()`.

```
@@finalType
MirrorNodeTokenBalancePage {
    @@immutable List<MirrorNodeTokenBalance> tokens

    // Null when there are no more pages. Pass to a new execute() call via
    // setNextPage() to fetch the next page.
    @@nullable @@immutable String next
}
```

---

### `MirrorNodeTokenBalanceQuery`

A new standalone query class that fetches one page of token balances from the mirror node REST API. Does not extend the base `Query` class. Follows the same pattern as `MirrorNodeAccountBalanceQuery`.

`setAccountId` accepts `shard.realm.num`, EVM address (`0x...`), or public key alias — all resolved natively by the mirror node.

```
MirrorNodeTokenBalanceQuery {
    @@nullable accountId: AccountId
    @@nullable tokenId: TokenId
    @@nullable nextPage: String

    // Required — throws before any network call if not set.
    MirrorNodeTokenBalanceQuery setAccountId(accountId: AccountId)

    // Optional. If set, fetches only this token (single request, next = null).
    MirrorNodeTokenBalanceQuery setTokenId(tokenId: TokenId)

    // Optional. Set to MirrorNodeTokenBalancePage.next to fetch a subsequent page.
    // Must not be combined with setTokenId.
    MirrorNodeTokenBalanceQuery setNextPage(next: String)

    @@async
    MirrorNodeTokenBalancePage execute(client: Client)
}
```

**`execute` behaviour:**
- Returns one page of up to 100 token balances.
- When `tokenId` is set: single request, `page.next` is always `null`.
- When `nextPage` is set: fetches the page at that cursor URL verbatim; `page.next` is `null` when the last page is reached. `accountId` is not required in this case — the account is already embedded in the cursor URL.
- When neither is set: fetches the first page ordered ascending by token ID. `accountId` is required.
- `setNextPage` and `setTokenId` must not be used together — throws before any network call if both are set.

**Design note — return type:**
`execute` returns `MirrorNodeTokenBalancePage` rather than a flat `List`. This makes the pagination cursor explicit and gives callers full control over how many pages they consume. SDK teams that have a strong convention around map-keyed balance types may choose to wrap `page.tokens` in a `MirrorNodeTokenBalanceMap` type that exposes a `get(tokenId)` accessor.

---

## Internal Changes

### `MirrorNodeTokenBalanceQuery` implementation

The class does not extend `Query`. It uses `fetch` (or the SDK's equivalent HTTP abstraction) and `client.mirrorRestApiBaseUrl`, following the same structure as `MirrorNodeAccountBalanceQuery` and `FeeEstimateQuery`.

**Endpoint (single-token, `tokenId` set):**
```
GET /api/v1/accounts/{accountId}/tokens?token.id={tokenId}&limit=100
```

**Endpoint (first page, no filter):**
```
GET /api/v1/accounts/{accountId}/tokens?limit=100&order=asc
```

**Endpoint (subsequent page, `nextPage` set):**
```
GET {nextPage}   // use the links.next URL verbatim — do not reconstruct it
```

**Mirror response shape per token (`TokenRelationship` schema):**
```json
{
  "token_id": "0.0.12345",
  "balance": 5000000,
  "decimals": 6,
  "automatic_association": true,
  "created_timestamp": "1234567890.000000000",
  "freeze_status": "NOT_APPLICABLE",
  "kyc_status": "NOT_APPLICABLE"
}
```

All fields are mapped to `MirrorNodeTokenBalance` with no filtering:

| Mirror node field       | SDK field             | Type                        |
|-------------------------|-----------------------|-----------------------------|
| `token_id`              | `tokenId`             | `TokenId`                   |
| `balance`               | `balance`             | `Long`                      |
| `decimals`              | `decimals`            | `int` (nullable for NFTs)   |
| `automatic_association` | `automaticAssociation`| `Boolean` (nullable)        |
| `created_timestamp`     | `createdTimestamp`    | `String` (nullable)         |
| `freeze_status`         | `freezeStatus`        | `String` (nullable)         |
| `kyc_status`            | `kycStatus`           | `String`                    |

Map `links.next` → `MirrorNodeTokenBalancePage.next` (null when absent). This type corresponds directly to the mirror node's `TokenRelationship` OpenAPI schema — SDK teams may choose to generate it from the spec rather than hand-writing it.

### Free query

`MirrorNodeTokenBalanceQuery` is free. No query payment or operator signing is required.

### Non-existent account

The mirror node `/api/v1/accounts/{id}/tokens` endpoint returns HTTP 404 when the account does not exist (verified against testnet and mainnet). The SDK surfaces this as an SDK-appropriate error. A valid account that does not hold the specified `tokenId` returns an empty `tokens` array with `next = null` (no error).

### Eventual consistency

The mirror node reflects consensus state with a small lag (typically seconds). Token balances read immediately after a transfer may still show pre-transfer values. This should be documented in SDK release notes.

### Response Codes

No consensus node response codes apply. Mirror node HTTP errors:

- `400 Bad Request` — invalid ID format. Surface as an SDK-appropriate error. Do not retry.
- `404 Not Found` — account does not exist. Surface as an SDK-appropriate error. Do not retry.
- `500 / 503` — transient mirror node error. Retry with the same backoff policy used by `MirrorNodeAccountBalanceQuery`.

---

## Test Plan

1. Given a valid account ID with token associations and no `tokenId` filter, when `execute()` is called, then `page.tokens` contains up to 100 items and `page.next` is non-null when more pages exist.
2. Given `page.next` is non-null, when a new `execute()` is called with `setNextPage(page.next)`, then the next page of tokens is returned with no overlap with the previous page.
3. Given the last page, when `execute()` returns, then `page.next` is null.
4. Given a valid account ID and a `tokenId` the account holds, when `execute()` is called with `setTokenId`, then `page.tokens` contains exactly one item with the correct balance and decimals, and `page.next` is null.
5. Given a valid account ID and a `tokenId` the account does not hold, when `execute()` is called with `setTokenId`, then `page.tokens` is empty and `page.next` is null.
6. Given a non-existent account ID, when `execute()` is called, then an error is thrown (mirror node returns 404).
7. Given no `accountId` is set, when `execute()` is called, then an error is thrown before any network call.
8. Given a malformed account ID string, when `execute()` is called, then an SDK error is thrown before any network call.
9. Given both `setTokenId` and `setNextPage` are set, when `execute()` is called, then an error is thrown before any network call.
10. Given a mirror node that returns a transient 503, when `execute()` is called, then the SDK retries and returns the correct result.

### TCK

Tests 1–10 should have corresponding issues in `hiero-ledger/hiero-sdk-tck`. Tests 1–3 (pagination flow) and test 9 (invalid combination) are the critical integration checks for the new design.

---

## SDK Example

### Single page — first 100 tokens

```javascript
import { MirrorNodeTokenBalanceQuery, Client } from "@hiero-ledger/sdk";

const client = Client.forMainnet();

const page = await new MirrorNodeTokenBalanceQuery()
    .setAccountId("0.0.12345")
    .execute(client);

for (const token of page.tokens) {
    const humanReadable = token.decimals != null
        ? token.balance / Math.pow(10, token.decimals)
        : token.balance; // NFT — balance is count
    console.log(`${token.tokenId}: ${humanReadable} (freeze: ${token.freezeStatus}, kyc: ${token.kycStatus})`);
}
```

### All pages — wallet portfolio

```javascript
let page = await new MirrorNodeTokenBalanceQuery()
    .setAccountId("0.0.12345")
    .execute(client);

while (true) {
    for (const token of page.tokens) {
        const humanReadable = token.balance / Math.pow(10, token.decimals);
        console.log(`${token.tokenId}: ${humanReadable}`);
    }
    if (page.next == null) break;
    page = await new MirrorNodeTokenBalanceQuery()
        .setNextPage(page.next)
        .execute(client);
}
```

### Single-token lookup

```javascript
const page = await new MirrorNodeTokenBalanceQuery()
    .setAccountId("0.0.12345")
    .setTokenId("0.0.98765")
    .execute(client);

if (page.tokens.length > 0) {
    const token = page.tokens[0];
    console.log(`Balance: ${token.balance} (${token.decimals} decimals)`);
} else {
    console.log("Account does not hold this token");
}
```

### Migration from `AccountBalanceQuery`

```javascript
// Before — deprecated
import { AccountBalanceQuery } from "@hiero-ledger/sdk";
const balance = await new AccountBalanceQuery()
    .setAccountId(accountId)
    .execute(client);
const hbars = balance.hbars;       // Hbar
const tokens = balance.tokens;     // Map<TokenId, Long> — deprecated since HIP-367

// After — HBAR balance
import { MirrorNodeAccountBalanceQuery } from "@hiero-ledger/sdk";
const hbarBalance = await new MirrorNodeAccountBalanceQuery()
    .setAccountId(accountId)
    .execute(client);

// After — token balances (first page)
import { MirrorNodeTokenBalanceQuery } from "@hiero-ledger/sdk";
const page = await new MirrorNodeTokenBalanceQuery()
    .setAccountId(accountId)
    .execute(client);
// page.tokens: List<MirrorNodeTokenBalance>, each with .tokenId, .balance, .decimals
// page.next: String cursor for next page, or null if all tokens fit in one page
```
