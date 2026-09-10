# Updating the Mirror Node HTTP Client Architecture

## Summary

Mirror-node REST is now a first-class SDK transport. `FeeEstimateQuery`, `RegisteredNodeAddressBookQuery`, `MirrorNodeAccountBalanceQuery`, contract-call / estimate-gas, entity-id resolution, and the in-flight `MirrorNodeTokenBalanceQuery` each open HTTP themselves — a static `HttpClient`, a one-off `fetch` / `requests.post` / `URLSession`, or a new client per call.

The failure modes are documented with [hiero-sdk-java#2883](https://github.com/hiero-ledger/hiero-sdk-java/pull/2883). The same gaps appear, in different shapes, in every Hiero SDK. The SDKs also have differences on **query behavior** (freeze, chunks, missing-entity, integer precision, timeouts). This proposal covers both: one owned transport, and one product contract.

**Date Submitted:** 2026-08-21

**Related references:**
- [hiero-sdk-java#2883](https://github.com/hiero-ledger/hiero-sdk-java/pull/2883) — Hendrik's review
- [sdk-collaboration-hub#281](https://github.com/hiero-ledger/sdk-collaboration-hub/pull/281)
- [Mirror Node ingress / local defaults](https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/proposals/mirror-node-ingress-endpoint-standardization.md) — URL rewriting; out of scope here
- [MirrorNodeAccountBalanceQuery](./account-balance-query-mirror-node-migration.md)
- [MirrorNodeTokenBalanceQuery](./mirror-node-token-balance-query.md)
- [Mirror-node eventual consistency](./mirror-node-eventual-consistency.md)
- [hiero-sdk-js#4336](https://github.com/hiero-ledger/hiero-sdk-js/issues/4336) — JS JSON int64 precision
- HIP-1261 — fee-estimate retry / chunk aggregation baseline

---

## Problems

### 1. Duplicated, unowned clients

Java:

```java
private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
```

appears in `FeeEstimateQuery`, `RegisteredNodeAddressBookQuery`, and the in-flight balance-query classes. `EntityIdHelper.performQueryToMirrorNodeAsync` builds a **new** `HttpClient` per call. None participate in `Client.close()`. (`HttpClient` is `AutoCloseable` since Java 21.)

Each Java static client has its own selector thread and pool. "Leaks transport threads / blocks JVM shutdown" is overstated: on JDK 11+ the selector thread is a daemon. The real bug is ownership — a short-lived `Client` should not leave process-lifetime pools, and `close()` should be enough.

### 2. Executor / thread-context

`RegisteredNodeAddressBookQuery.executeAsync` uses `CompletableFuture.supplyAsync(..., client.executor)` while I/O still runs on `HttpClient`'s threads. `FeeEstimateQuery.executeAsync` uses `sendAsync().whenComplete(...)` and retries with `Thread.sleep` on that callback thread — no `client.executor` at all. Tracing / MDC set on `Client` do not apply. No production bug is tied to this yet.

### 3. Transport configurability

Bare `HttpClient.newHttpClient()` / `fetch()` / `http.Client` cannot take a corporate proxy, custom truststore or mTLS, an `Authorization` header, pool / HTTP-version settings, an SDK `User-Agent`, or tracing interceptors. Real for private-mirror / enterprise users; no reported production blocker found. These should be *possible* once a transport exists; they do not all need a cross-SDK setter in v1.

### 4. Android (Java, confirmed)

`java.net.http` is not on Android libcore at any API level. Classes with a static `HttpClient` throw `NoClassDefFoundError` on load. Android is a maintained target (README badge, `example-android/`, CI, changelog). The SDK already tells Android users to swap `grpc-okhttp` for Netty. HTTP needs the same shape of fix. This is broken today.

`EntityIdHelper`'s javadoc says "API 33+". That is wrong: the class fails on every Android API, not only below 33.

### 5. Query behavior drift

Every SDK exposes the same query classes, but each one implements them differently — so the *same* call returns *different* results depending on the language. Unifying the HTTP transport (Problems 1–4) does not fix this; the normative behavior each SDK should converge on is in [Behavioral alignment](#behavioral-alignment-beyond-transport). The divergences today:

- **Java** does not auto-freeze on execute and has no chunk loop (`setTransaction(Transaction)` snapshots via `makeRequest()`; unfrozen txs fail there).
- **Rust** estimates the first chunk only and sends `highVolumeThrottle` / `application/x-protobuf`.
- **JS** maps `/balances` `[]` to 0 HBAR and parses money fields with `response.json()` (IEEE-754; values above `2^53−1` truncate). `AccountId.populateAccountNum` / `populateAccountEvmAddress` sleep 3 s then `fetch` with no retry or timeout. `ContractId.populateAccountNum` is the same `fetch` **without** the sleep.
- **Swift** parses fees via `JSONSerialization` → `NSNumber.uint64Value` (same precision *risk* as JS; not separately reproduced) and derives chunk `total` by summing per-chunk totals rather than from the aggregated formula.
- **Go** fee estimate passes timeout `0` into the retry helper (no per-request timeout).
- **Python** always sends `high_volume_throttle=0` and mutates the caller's transaction during the chunk loop.

---

## Goals

1. **One HTTP layer per `Client`.** All mirror REST (queries, entity-id, address book, fee estimate, contract-call, populate) goes through it.
2. **`Client.close()` closes HTTP.** In-flight calls are waited out, then aborted (same budget as gRPC shutdown). After close, new calls fail; they do not construct a new client.
3. **Default transport is platform-safe.** Java must not load `java.net.http` on the Android class-init path.
4. **Identity and headers.** Every request sends `User-Agent: hiero-sdk-<language>/<semver>`. Callers can add headers on `Client`.
5. **One retry policy** for transient HTTP.
6. **Existing knobs apply.** `requestTimeout` (and `client.executor` where the SDK has one) cover HTTP as they cover gRPC.
7. **Consistent query behavior across SDKs.** Freeze, chunks, missing-entity, lossless integers, timeouts, and wire format behave the same way in every language.

## Non-goals

- URL / port / localhost rewriting — [ingress proposal](https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/proposals/mirror-node-ingress-endpoint-standardization.md). This doc only records current drift.
- A gRPC `Query` subclass for mirror REST. Queries stay plain classes.
- One HTTP implementation or a required extra dependency in every SDK.
- Identical proxy / mTLS / HTTP2 / interceptor setters across languages. Those stay native on the injected transport.
- A built-in "wait for the mirror" sleep. Callers use [eventual-consistency patterns](./mirror-node-eventual-consistency.md).

---

## New APIs

Types below use a language-neutral notation: `@@finalType` = immutable value type, `abstraction` = interface/trait/protocol to implement, `@@immutable` = read-only field, `@@nullable` = may be absent, `@@async` = returns a future/promise/task. Concrete names and shapes are language-native (see the default-transport table).

### `MirrorNodeHttpHeaders`

Extra headers the `Client` sends on every mirror HTTP request. The SDK `User-Agent` is always present. A caller `User-Agent` is appended (space-separated), not replaced.

```
@@finalType
MirrorNodeHttpHeaders {
    @@immutable map<string, string> values
}
```

### Transport SPI (language-native name)

Public so Android, tests, and private-mirror users can inject an implementation. Type names are language-native; behavior is not.

```
abstraction MirrorNodeHttpTransport {
    // GET or POST. `body` / `contentType` are null on GET.
    // MUST apply Client headers and User-Agent; MUST honour request.timeout.
    // MUST be safe to call concurrently from multiple threads/tasks.
    @@async
    MirrorNodeHttpResponse execute(request: MirrorNodeHttpRequest)

    // Client.close(). After close, execute() fails fast (see error contract).
    // In-flight: wait up to closeTimeout, then abort. Idempotent (second close is a no-op).
    close(closeTimeout: duration): void
}

@@finalType
MirrorNodeHttpRequest {
    @@immutable method: string          // "GET" | "POST"
    @@immutable url: string
    @@nullable @@immutable body: bytes
    @@nullable @@immutable contentType: string
    @@immutable timeout: duration       // per-attempt deadline; see Timeouts
    @@immutable headers: map<string, string>
}

@@finalType
MirrorNodeHttpResponse {
    @@immutable statusCode: int
    @@immutable body: bytes
    @@immutable headers: map<string, string>
}
```

Swift may keep `URLSession`; JS a custom `fetch`. They do not need a class literally named `MirrorNodeHttpTransport`.

#### Transport contract

The SPI is the transport boundary; **classification and retry live in the shared helper above it, never in the transport.**

- **Non-2xx is not an error at the SPI.** `execute()` returns any completed HTTP exchange — including `4xx` / `5xx` — as a `MirrorNodeHttpResponse` with its `statusCode`. The transport MUST NOT throw on status code alone. The helper reads `statusCode` and decides retry vs. surface (see [retry table](#internal-changes)).
- **`execute()` throws only on no-response conditions:** connection reset, DNS failure, TLS handshake failure, per-attempt timeout, or request cancellation. These map to the language-native transport-error type and are what the helper treats as "transport failure — retry, then surface."
- **Concurrency.** `execute()` MUST be safe to call concurrently; one `Client` shares one transport across all queries.
- **After `close()`**, `execute()` fails fast with the language-native "client closed" error and MUST NOT construct a new client or open a new connection.
- **Thread-safety of `close()`.** `close()` is idempotent and safe to race with in-flight `execute()` calls; those calls either complete within `closeTimeout` or are aborted with a transport-error.

Query classes keep their existing public error types. A transport-error or a retry-exhausted non-2xx is wrapped into that per-query type at the helper boundary, preferring the mirror `_status` envelope for the message (see [Response Codes](#response-codes)).

| SDK | Default transport |
|---|---|
| Java | Android-safe (OkHttp *or* `HttpURLConnection` + SPI). **Not** `java.net.http` on the Android class-load path. See Open Questions. |
| JS | Platform `fetch` |
| Go | Reused `http.Client` on `Client` |
| Python | `requests.Session` on `Client` |
| Swift | `URLSession` on `Client` (lift the FeeEstimateQuery override) |
| Rust | Reused Hyper client on `Client` |
| C++ | Reused client behind existing `internal::HttpClient` |

---

## Updated APIs

### `Client`

```
Client {
    Client setMirrorHttpHeaders(headers: map<string, string>)
    Client addMirrorHttpHeader(name: string, value: string)
    MirrorNodeHttpHeaders getMirrorHttpHeaders()

    // Null / unset restores the SDK default. Language-native parameter type.
    Client setMirrorHttpTransport(transport: MirrorNodeHttpTransport)
    MirrorNodeHttpTransport getMirrorHttpTransport()

    // Existing setRequestTimeout: also the default PER-ATTEMPT HTTP timeout
    // passed as MirrorNodeHttpRequest.timeout (see Timeouts).
    // Existing close(): also closes the HTTP transport. The closeTimeout handed
    // to MirrorNodeHttpTransport.close() is the SAME shutdown budget the Client
    // already applies to gRPC channel shutdown (close() and close(timeout)
    // overloads both feed that value through). No new close knob is introduced.
}
```

**Header precedence.** On each request the transport builds the header map in this order, later steps overriding earlier ones:
1. Caller headers from `setMirrorHttpHeaders` / `addMirrorHttpHeader` (e.g. `Authorization`, custom tracing headers).
2. Per-call headers the helper sets for the endpoint (e.g. `Content-Type: application/protobuf` on the fee-estimate / contract POST). These win over caller headers of the same name because the endpoint requires them — a caller cannot break a POST by setting a global `Content-Type`.
3. `User-Agent`, which is special-cased: the SDK token is always present and a caller-supplied `User-Agent` is **appended** space-separated, not replaced (see [`MirrorNodeHttpHeaders`](#mirrornodehttpheaders)).

So a caller freely sets `Authorization` and any header the SDK does not set per-call; the SDK owns the per-endpoint `Content-Type` and the `User-Agent` token.

`setMirrorHttpTransport` is the extension point for Android, proxy / mTLS, mocks, and tracing wrappers. JS may take a `fetch` function or `https.Agent` and document it as this method.

### Existing query classes

No public signature changes. `FeeEstimateQuery`, `RegisteredNodeAddressBookQuery`, `MirrorNodeAccountBalanceQuery`, `MirrorNodeContractCallQuery`, `MirrorNodeContractEstimateGasQuery`, `MirrorNodeTokenBalanceQuery`, `EntityIdHelper`, and populate helpers become callers of the `Client`-owned transport.

### REST call sites (endpoints)

Every call site below routes through the helper. Paths are informative — each query already owns its own path and local-URL rewriting stays on the [ingress](https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/proposals/mirror-node-ingress-endpoint-standardization.md) timeline — but they fix the full surface an implementer must migrate. All are relative to `client.mirrorRestApiBaseUrl`.

| Call site | Method | Path | Body | Notes |
|---|---|---|---|---|
| `MirrorNodeAccountBalanceQuery` | GET | `/api/v1/balances?account.id={id}` | — | `[]` → `INVALID_ACCOUNT_ID` (HTTP 200) |
| `MirrorNodeTokenBalanceQuery` | GET | `/api/v1/accounts/{id}/tokens?limit=100` | — | Paginated; 404 → not-found |
| `RegisteredNodeAddressBookQuery` | GET | `/api/v1/network/nodes` | — | Paginated |
| `MirrorNodeContractCallQuery` | POST | `/api/v1/contracts/call` | JSON | — |
| `MirrorNodeContractEstimateGasQuery` | POST | `/api/v1/contracts/call` | JSON (`estimate: true`) | — |
| `FeeEstimateQuery` | POST | HIP-1261 fee-estimate endpoint | protobuf | `Content-Type: application/protobuf`; one POST per chunk |
| `AccountId` populate | GET | `/api/v1/accounts/{idOrEvmAddress}` | — | No sleep, no bare `fetch` |
| `ContractId` populate | GET | `/api/v1/contracts/{id}` | — | No bare `fetch` |
| `EntityIdHelper` resolve | GET | `/api/v1/accounts/{id}` (or `/contracts/{id}`) | — | No new `HttpClient` per call |

The fee-estimate path itself is defined by HIP-1261; this proposal does not fix it, only that the request goes through the shared transport with the protobuf `Content-Type`.

---

## Internal Changes

- Default transport is created with the `Client` or lazily on first use. Closed exactly once from `Client.close()`; a second close is a no-op.
- **Default transport policy (all SDKs, overridable via `setMirrorHttpTransport`):** verify TLS certificates against the platform trust store; follow up to 5 HTTP redirects, but drop the `Authorization` and other caller headers on a cross-origin redirect; allow plaintext `http://` when the mirror base URL is explicitly `http://` (required for the local-mirror path and the C++ hardcoded-`https://` bug), i.e. do not force-upgrade the scheme. Connection reuse / keep-alive is enabled; pool sizing stays at the platform default unless an injected transport overrides it.
- Where the SDK has `client.executor`, **completion and retry scheduling** run on it. I/O threads may be the transport's own (same as Netty / okhttp for gRPC). JS / Python keep the platform default.
- `User-Agent: hiero-sdk-<language>/<semver>` on every request (e.g. `hiero-sdk-java/2.51.0`). Caller `User-Agent` is appended.
- Retry (all mirror REST, overridable via existing `maxAttempts` / backoff setters):

  | Condition | Retry? |
  |---|---|
  | 408, 429, 500, 502, 503, 504 | Yes |
  | Timeout, connection reset, DNS failure | Yes |
  | 400, 404, other 4xx | No |
  | 2xx | No |

  Backoff: `250 ms × 2^n` with full jitter (`random(0, 250 ms × 2^n)`), cap 8 s, where `n` is the zero-based retry index — `n = 0` on the first retry (up to 250 ms), `n = 1` on the second (up to 500 ms), and so on. On a `429` (and on any `5xx` that carries `Retry-After`), if the header is present and parses to a delay `≤` the cap, use it instead of the computed backoff; otherwise fall back to jittered backoff. Honour `Client` cancellation during sleep (Go already uses `networkUpdateContext`). Do **not** change each SDK's `maxAttempts` default (they differ: 10 / 5 / 3).

  **Retry safety.** Every mirror REST call in scope is read-only — `GET`s plus the fee-estimate / contract-call / estimate-gas `POST`s, none of which mutate mirror state — so retrying a `POST` on a `5xx`/timeout is safe. The transport MUST NOT assume `POST` is non-idempotent and suppress retries.

  **Timeouts.** `requestTimeout` is a **per-attempt** deadline (it is `MirrorNodeHttpRequest.timeout`), not a budget across the whole retry loop. Each attempt gets the full `requestTimeout`; total wall-clock is bounded by `maxAttempts × (requestTimeout + backoff)`. A per-attempt timeout is classified as a transport failure and retried like any other. SDKs that also expose an overall deadline (e.g. Go `context`) still pass it through, and it wins when it is the tighter bound.

- Query classes do not construct or hold an HTTP client, do not implement retry once the helper exists (chunk aggregation stays in the query), do not set `User-Agent`, do not sleep for mirror lag, and do not parse money fields as IEEE-754 doubles.
- **Java:** no `java.net.http` on any class Android loads. `example-android/` must compile-and-load at least one mirror REST query.
- **All REST call sites** use the helper — not just the query classes. The populate / entity-id paths bypass even the retry helpers that exist today: Java `EntityIdHelper` (new `HttpClient` per call), JS `AccountId` / `ContractId` populate and `MirrorNodeContractQuery` (bare `fetch`), Go `AccountID._MirrorNodeRequest` / `ContractID.PopulateContract` (raw `http.Get` `#nosec`, no retry/timeout), C++ `AccountId::populateAccountEvmAddress` (`invokeREST`, no status/retry, hardcoded `https://`), and the Python `requests.get` equivalents. No private client, no private `fetch`.
- In-flight feature PRs (`MirrorNodeTokenBalanceQuery`, etc.) are not blocked. They migrate when the layer exists. A Java Android-safe helper may land first; it must still move to `Client` ownership in this initiative. After that move, `execute()` following `close()` fails — a fix, changelog-worthy (today the static client still works).

---

## Behavioral alignment (beyond transport)

Verified against the different versions of the SDK (`FeeEstimateQuery` in all seven; JS account-balance / `AccountId` / `ContractId`; Java `EntityIdHelper`).

### Contract

| Topic | Default | Drift to close |
|---|---|---|
| Auto-freeze | Freeze on `execute()` if needed (HIP-1261) | Java: no; proto is snapshotted in `setTransaction(Transaction)` via `makeRequest()` |
| Chunks | Every chunk; formula below | Java: none. Rust: chunk 0 only. Swift: recomputes `network.subtotal` but sets `total` = sum of per-chunk `total` (equivalent only while the multiplier is constant across chunks; diverges if it is not). Go: type-switch on `FileAppend` / `TopicMessageSubmit` only |
| Missing entity | Settled in [account-balance migration](./account-balance-query-mirror-node-migration.md): `/balances` → `[]` is `INVALID_ACCOUNT_ID` (HTTP 200, not 404). `[{ balance: 0 }]` is a real zero-HBAR account. HTTP 404 on other endpoints is still not-found. | JS account-balance still maps `[]` → 0 HBAR — implementation drift, not an open design question. Token-balance proposal errors on 404. |
| JSON integers | Lossless int64/uint64 in the shared helper | JS: `response.json()` above `2^53−1`. Swift: `NSNumber` — same *risk*, confirm with test 16. Java `getAsLong()`, Go `uint64`, Python `int`, Rust `u64` are fine for HBAR |
| Timeouts | `Client.requestTimeout` unless the query sets its own | Go fee estimate: timeout `0`. JS populate: none. Python fee estimate: hardcoded 10 s |
| Query param | `high_volume_throttle`; omit when 0 | Rust: `highVolumeThrottle`. Python: always sends `=0` |
| Content-Type | `application/protobuf` on fee-estimate POST | Rust: `application/x-protobuf` |
| Error body | `_status.messages[].detail`, then `.message`, else truncated raw body | Only JS `readErrorDetail` unwraps |
| Local URLs | No rewrite ([ingress](https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/proposals/mirror-node-ingress-endpoint-standardization.md)) | JS `5551→8084`. Go forces `localhost:8084`. Python `38081→8084`. Rust `5600→5551`. Swift/C++ → `8084`. Java fee estimate uses `getMirrorRestBaseUrl()` as-is |
| Mirror lag | Never sleep in the SDK | JS `AccountId` populate: **3 s** then `fetch`. `ContractId` populate: `fetch` only |

HIP-1261 aggregation when `usedChunks > 1`:

```
aggregatedNode    = sum(chunk.node.base + sum(chunk.node.extras.subtotal))
aggregatedService = sum(chunk.service.base + sum(chunk.service.extras.subtotal))
network.subtotal  = aggregatedNode * network.multiplier   // first chunk
total             = network.subtotal + aggregatedNode + aggregatedService
```

Do not sum per-chunk `total`. Do not estimate only chunk 0.

### Language-only (not in the table)

- **Java.** `executeAsync` must schedule retry delay (`Delayer` / `client.executor`), not `Thread.sleep` on the `HttpClient` callback. Drop the `EntityIdHelper` "API 33+" javadoc when the transport moves.
- **JS.** Shared bigint-safe parse (`response.text()` + lossless parser → `Long.fromString`) for every mirror JSON path ([#4336](https://github.com/hiero-ledger/hiero-sdk-js/issues/4336)).
- **Go.** Generic "required chunks" path; reuse one `http.Client`; pin TLS / redirects there (drops per-call `#nosec` `http.Post`). Route `AccountID._MirrorNodeRequest` / `ContractID.PopulateContract` through the helper too — they use raw `http.Get` with no retry or timeout today.
- **Python.** Prefer per-chunk bytes without mutating the caller's transaction (or test the save/restore `finally` as a contract).
- **C++.** `internal::HttpClient` finds the path with `url.find('/', 8)` so the `//` in the scheme is skipped. That works for `http://host/path` and `https://host/path`. It is still a brittle parser (no-path URLs, `.data()` on a `string_view`). Parse scheme / host / path properly; keep the helper; give it `Client` lifetime. Also route `populateAccountEvmAddress` through the retrying `invokeRESTWithStatus` (it uses `invokeREST` today) and stop hardcoding `https://`, which breaks the local-mirror path.

---

## Response Codes

No new consensus-node codes.

- `400` / `404` / other 4xx — SDK error, no retry.
- `200` + `/api/v1/balances` → `balances: []` — `INVALID_ACCOUNT_ID`, no retry. Not 0 HBAR. (`[{ balance: 0 }]` is a real account; see [account-balance migration](./account-balance-query-mirror-node-migration.md).)
- `408` / `429` / `5xx` (table above) — retry, then surface.
- Transport failures — retry, then surface.

Keep each query's existing error type. Prefer the mirror `_status` envelope when present.

---

## Test Plan

1. Two query types on one `Client` share one transport (one session/pool).
2. After `close()`, `execute()` fails with client-closed and does not open a new client.
3. `close(timeout)` longer than an in-flight call waits for it, then releases the transport.
4. `close(timeout)` shorter than an in-flight call aborts and returns by the timeout.
5. `addMirrorHttpHeader("Authorization", "Bearer test")` appears on the wire **with** the SDK `User-Agent`.
6. Caller `User-Agent` is appended after the SDK token.
7. `setRequestTimeout(1s)` against a hung mirror fails near 1 s (not a hardcoded 10 s / 30 s).
8. 503 then 200 — helper retries and succeeds.
9. 400 — no retry; error includes body/detail.
10. Injected mock transport is used; default network client is not.
11. **Java / Android.** Load + execute a mirror query on `example-android` (or the Android artifact) — no `NoClassDefFoundError` for `java.net.http.HttpClient`.
12. **Java.** Async completion runs on `client.executor`.
13. Chunked `FileAppend` (3 chunks) — one POST per chunk; `total` / `network.subtotal` match the HIP-1261 formula.
14. Unfrozen transaction — `FeeEstimateQuery.execute()` freezes before POST (Java fails this today).
15. `/balances` → `{"balances":[]}` — `INVALID_ACCOUNT_ID`, not 0 HBAR. (`[{ balance: 0 }]` remains a successful zero; covered in the account-balance proposal.)
16. Balance or fee `total` `4611686018427387905` parses exactly (JS today fails; Swift: confirm).
17. `AccountId.populateAccountNum` (or equivalent) uses the shared transport and does not sleep 3 s.
18. 400 body with `_status.messages[].detail` — surfaced error contains that detail.
19. **Rust.** `setHighVolumeThrottle(5000)` → `high_volume_throttle=5000` and `Content-Type: application/protobuf`.
20. **Go.** `setRequestTimeout(1s)` + hung mirror — fee estimate fails near 1 s, not hang with timeout `0`.
21. **C++.** `http://127.0.0.1:38081/api/v1/...` — host/path split is correct.
22. **Java.** `executeAsync` retry after 503 is not `Thread.sleep` on the HTTP callback thread.
23. Per-attempt timeout: `setRequestTimeout(1s)` + a mirror that stalls each attempt, with `maxAttempts > 1`, retries — each attempt aborts near 1 s and total wall-clock ≈ `maxAttempts × (1 s + backoff)`, i.e. the timeout is per-attempt, not a single overall 1 s budget.
24. `429` with `Retry-After: 2` then `200` — the retry waits ~2 s (header), not the computed jittered backoff.
25. Concurrent `execute()`: N queries on one `Client` run in parallel through the shared transport with no shared-state corruption (one session/pool, correct per-response bodies).
26. Cross-origin redirect drops `Authorization` — a `3xx` to a different origin does not forward the caller `Authorization` header.
27. `4xx`/`5xx` surfaces as a `MirrorNodeHttpResponse` at the SPI (transport does not throw on status code); the helper, not the transport, decides retry.
28. **Python.** `setRequestTimeout(1s)` + hung mirror — fee estimate fails near 1 s, not the hardcoded 10 s.

### TCK

Cross-SDK issues in `hiero-ledger/hiero-sdk-tck`: 1, 2, 5–9, 13–16, 18, 23–27. Per-SDK unit tests where the TCK cannot see the socket: 3, 4, 10, 25. Java-only: 11, 12, 22. Language-specific: 17, 19–21, 28.

---

## SDK Example

Existing callers do not change:

```javascript
const client = Client.forMainnet();
const balance = await new MirrorNodeAccountBalanceQuery()
    .setAccountId("0.0.12345")
    .execute(client);
await client.close();
```

Headers and injection:

```javascript
client.addMirrorHttpHeader("Authorization", "Bearer <token>");
```

```java
client.setMirrorHttpTransport(new OkHttpMirrorTransport(
    new OkHttpClient.Builder()
        .proxy(proxy)
        .sslSocketFactory(sslSocketFactory, trustManager)
        .addInterceptor(tracingInterceptor)
        .build()));
```

Implementers replace per-class clients with `MirrorNodeHttp.get/post(client, ...)` (or `client.getMirrorHttpTransport().execute(...)`).

---

## Rollout

| Phase | Scope | Blocks feature PRs? |
|---|---|---|
| **0 — Contract** | This proposal. | No |
| **1 — Java Android-safe default** | Remove `java.net.http` from the Android class-load path on current call sites. Smoke-test `example-android`. Other SDKs skip. | No |
| **2 — Client-owned transport** | Create/close with `Client`. Migrate every REST call site. Retry helper, `User-Agent`, headers, transport policy (TLS/redirect/scheme). Tests 1–10, 17, 23–27. New queries must not add a static client. | No |
| **3 — Executor + close** | Completion on `client.executor`. Close waits/aborts in-flight HTTP. Java: no callback `Thread.sleep`. Tests 3, 4, 12, 22. | No |
| **4 — Query behavior** | Freeze + all-chunks. Missing-entity error. Lossless integers (JS, Swift). `requestTimeout` (Go, Python). Wire format. Error envelope. Tests 13–16, 18–21, 28. Parallel with Phase 2 per SDK. | No |
| **5 — Native power features** | Documented proxy / mTLS / pool / interceptors via `setMirrorHttpTransport`. | No |

Phase 1 is the only shipping break on a maintained target. Phases 2–3 are Hendrik's architecture. Phase 4 is product alignment. Local URL rewriting stays on the ingress timeline.

---

## Open Questions

1. **Java default stack.** OkHttp (matches `grpc-okhttp`, interceptors/proxy/TLS, new JVM dependency) vs `HttpURLConnection` (no new dep) vs optional `java.net.http` in a JVM-only module. Maintainers pick in review.
2. **`setMirrorHttpTransport` public in every language in Phase 2?** Recommendation: yes wherever a mock is useful; JS takes a `fetch` function.
3. **User-Agent version.** Same channel each SDK already uses for its published version.
4. **`maxAttempts` defaults.** Recommendation: standardize classification + backoff only; leave attempt counts.
5. **Python chunk mutation.** Rebuild bytes without mutating the caller, or keep save/restore and test it?

Settled elsewhere (not open here): missing account on `/balances` — empty array → `INVALID_ACCOUNT_ID`; `[{ balance: 0 }]` → success. Specified in [account-balance-query-mirror-node-migration.md](./account-balance-query-mirror-node-migration.md). JS still returns 0 HBAR today; that is a follow-through bug, not a design choice.

---

## Decision asked of reviewers

Does this split — Android-safe Java transport now, `Client`-owned HTTP next, query-behavior alignment in parallel, native proxy/TLS/interceptors via injection, feature PRs not blocked — address #2883, or should any of Phases 2–5 be one blocking unit before further mirror-node query work lands?
