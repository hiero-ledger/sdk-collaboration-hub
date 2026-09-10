# Client gRPC-Web Transport and Custom Request Headers

**Date Submitted:** 2026-07-07

## Summary

This document proposes two public, client-level capabilities to be implemented
consistently across **all Hiero SDKs** (Java, JavaScript, Go, Rust, Python,
C++, Swift):

- **`requestHeaders`** — a string→string map of custom headers attached to every
consensus-node request (e.g. an authentication token).
- **`transport`** — selects native gRPC (default) or gRPC-Web for consensus-node
connections, allowing a server-side client to reach a gRPC-Web proxy.

The motivating use case is a managed Hedera-compatible network whose
consensus-node gRPC endpoints are private and fronted by a **gRPC-Web proxy**
that requires a custom **authentication header** (e.g.
`authorization: Bearer <token>`). Two gaps in the current SDKs block this:

1. The default server-side client transport speaks native gRPC (HTTP/2) and
cannot talk to a gRPC-Web proxy.
2. Clients hardcode their request headers and expose no way to inject custom
ones (auth tokens, gateway/middleware headers).

The public API surface (`Transport`, `setTransport`/`getTransport`,
`setRequestHeaders`/`getRequestHeaders`, `ClientConfiguration`) must be
semantically identical across every SDK, mapped to each language's conventions.
The JavaScript SDK already supports gRPC-Web in the browser; this design extends
gRPC-Web support to server-side clients (and to the other SDKs) and adds the
header injection point. These capabilities aim to reduce the need for per-SDK
workarounds (custom channel/transport subclasses or interception of the underlying
HTTP client).

**Defaults are unchanged:** native gRPC, no extra headers. A client that never
touches the new settings behaves identically to today, and existing networks are
unaffected.

- Reference issue: [hiero-sdk-js#4105](https://github.com/hiero-ledger/hiero-sdk-js/issues/4105)

## New APIs

### Transport

Enum selecting the consensus-node transport for a client.

```
enum Transport {
    GRPC      // native gRPC over HTTP/2 (default)
    GRPC_WEB  // gRPC-Web framing over HTTP(S); reaches a gRPC-Web proxy
}
```

### Client Configuration

The configuration object accepted by the client constructor / `fromConfig`,
letting users set the same two fields declaratively (e.g. from a config file).

```
ClientConfiguration {
    @@nullable transport: Transport
    @@default({}) requestHeaders: map<string, string>
}
```

When `transport` is null or omitted, it resolves to the client default, `GRPC`,
keeping the `@@nullable` config shape consistent with `Client`'s `@@default(GRPC)`
transport; `requestHeaders` is `@@default({})` rather than nullable.

## Updated APIs

### Client

```
Client {
    @@default(GRPC) transport: Transport
    @@default({}) requestHeaders: map<string, string>

    Client setTransport(transport: Transport)
    Transport getTransport()

    // Replaces the full user-supplied header set (no add/remove helper); keys are
    // normalized (lowercased) on store. Reserved keys are rejected (see Internal
    // Changes), so the stored set never contains one.
    // May be called while requests are in flight (see "Mutability and concurrency").
    // Throws when headers contains a reserved key, a -bin key, or a non-ASCII value.
    @@throws(invalid-header-error)
    Client setRequestHeaders(headers: map<string, string>)

    // Returns exactly the user-supplied set as last set (keys lowercased to match
    // transmission), not the effective merged set: SDK-managed headers are never
    // included. Because reserved keys are rejected at set time, the result can never
    // contain one, so it always matches what is sent and is independent of transport.
    map<string, string> getRequestHeaders()
}
```

## Internal Changes

### Header injection

Custom `requestHeaders` are attached to every consensus-node request and to any
pre-connection probe an SDK may perform. Headers flow from the client down to
each per-node connection and are applied per request: a change to the header set
must affect subsequent requests, including on connections that already exist.

Header injection is independent of `transport`: the same `requestHeaders` are
carried as gRPC metadata in both modes. Under `GRPC` this metadata is encoded as
HTTP/2 header frames; under `GRPC_WEB` it is sent as request HTTP headers over
HTTP/1.1 or HTTP/2. The injection point and semantics are identical.

### Mutability and concurrency

`setRequestHeaders` replaces the full set. Any request whose headers are assembled
after the call returns uses the complete new set, and a request being dispatched
concurrently with the call reads either the complete old set or the complete new set.
Requests already in flight are unaffected; their headers are fixed at dispatch and
reused across retries.

### Reserved headers

Reserved keys are SDK-managed and protocol-critical. Rather than silently ignoring
them at send time (which would leave `getRequestHeaders` reporting headers that
never go on the wire), `setRequestHeaders` **rejects** any reserved key, matched
case-insensitively — consistent with the `-bin`/non-ASCII rejection in "Key/value
constraints". Validation uses the **union** across both transports, so it is
deterministic and independent of the selected `transport`. Reserved keys are:

- The `grpc-*` prefix and HTTP/2 pseudo-headers (`:*`).
- Native gRPC mode: `content-type`, `te`, `user-agent`, `grpc-timeout`.
- gRPC-Web mode: `content-type`, `x-grpc-web`, `x-user-agent`.

Because the union reserves both `user-agent` and `x-user-agent`, the SDK's client
identity cannot be overridden in either mode — even though native gRPC only sends
`user-agent` and gRPC-Web only sends `x-user-agent` as the actual header. This is
stricter than either single transport requires, but it keeps rejection deterministic
across transports.

All accepted headers are sent unchanged, apart from the normalization in "Key/value
constraints".

### Key/value constraints

The `map<string, string>` surface carries ASCII string headers. To satisfy gRPC
metadata rules:

- Keys are **lowercased** on store (and thus before transmission), so protected-key
matching is case-insensitive and `getRequestHeaders` returns lowercased keys.
- Non-ASCII values are **rejected** rather than silently mangled.
- `bin` (binary metadata) keys are **rejected**, since a string→string map can't
express them. Binary headers are out of scope for this revision.

### Transport selection

When `transport = GRPC_WEB` is selected, consensus-node requests use standard
gRPC-Web framing — the same wire format browser clients already implement — with
`grpc-status` / `grpc-message` honored for error mapping (see "Error classification
and status mapping").

Only the JS SDK ships a gRPC-Web client today, so for JS this mostly extends an
existing browser path to server-side use. The other six SDKs speak native gRPC
only; for them `GRPC_WEB` is new framing logic — encoding/decoding it over an HTTP
client, or adopting a maintained gRPC-Web library where one exists.

The consensus-node HAPI RPCs used by the SDK are unary — one request, one response
— for both transactions and queries. This **includes receipt and record confirmation**
(`getTransactionReceipts`, `getTxRecordByTxID`), which the SDK polls client-side.
A transaction is therefore expected to be **submittable and confirmable end to end**
over `GRPC_WEB` (verified by the Test Plan).

Server-streaming RPCs are out of scope for this revision. On the Hiero SDKs the
streaming paths are mirror-node subscriptions (e.g. `TopicMessageQuery`).

### Network topology and node routing

The SDK network is a map of `host:port → nodeAccountId`, and the retry path
rotates across nodes, tracking health/backoff per node. gRPC-Web fits this
model directly: consistent with the established Hedera gRPC-Web proxy design,
**each consensus node is fronted by its own gRPC-Web proxy endpoint** —
there is one proxy per node.

This is the contract assumed by the existing `hedera-grpcWeb-proxy`, which states
it "is not possible to run a single proxy for all Hedera nodes since transactions
sent to a particular node need to identify the AccountId of the node," and by
[HIP-1046](https://hips.hedera.com/hip/hip-1046), which adds a per-node
`grpc_proxy_endpoint` field to the address book. A network is therefore configured
with a distinct proxy address per node:

```json
{
    "https://node3.proxy.example:443": "0.0.3",
    "https://node4.proxy.example:443": "0.0.4",
    "https://node5.proxy.example:443": "0.0.5"
}
```

gRPC-Web addresses carry an explicit `http://`/`https://` scheme, which is
authoritative and identical across SDKs; schemeless resolution is SDK-specific
(JS keeps its current local-host inference), so specify the scheme for portable
behavior. Auto-discovering each node's `grpc_proxy_endpoint` from the address book, as
described in HIP-1046, is a future extension.

**Node routing.** Because each node has a distinct proxy address, the existing
address→accountId map already pins each request to the correct node.

**Rotation and backoff.** Per-node health and backoff are unchanged, so a single
proxy outage marks only its one node unhealthy. Authentication, by contrast, is
not a per-node condition and is handled as fatal rather than rotated (see
"Transaction Retry").

### Response Codes

No new consensus-node response codes are introduced. Authentication is enforced
at the gRPC-Web proxy / transport layer and surfaces as a gRPC status, not a
`ResponseCodeEnum` value:

- `UNAUTHENTICATED` (gRPC status 16) — missing/invalid auth header (HTTP `401`).
- `PERMISSION_DENIED` (gRPC status 7) — authenticated but not authorized (HTTP
`403`).

#### Error classification and status mapping

A gRPC-Web proxy can reject a request *before* it reaches gRPC semantics — e.g. a
bad token returned as HTTP `401` with no gRPC trailers. If such responses fall
through to the generic retryable fallback, a bad/expired token would be rotated
across nodes instead of failing fast. To prevent this, every gRPC-Web transport
(and any gRPC-Web pre-connection probe an SDK may perform) must apply the following
mapping **at the transport boundary, before any retry decision**, in order:

1. **`grpc-status` trailer present** → map that status authoritatively,
regardless of whether `grpc-message` is present. A non-zero status with no
message is still an error (so `grpc-status: 16` → `UNAUTHENTICATED`). A
present `grpc-status` always wins over the HTTP status code.
2. **No `grpc-status`, HTTP `401`** → `UNAUTHENTICATED` (16).
3. **No `grpc-status`, HTTP `403`** → `PERMISSION_DENIED` (7).
4. **Otherwise** (no `grpc-status` and not an auth HTTP code) → fall back to a
transport-level `UNAVAILABLE`, as today. Note this also collapses other
non-auth HTTP codes (e.g. `404`, `500`) into a retryable `UNAVAILABLE`, which
is intentional for node rotation and deviates from the canonical gRPC HTTP →
status mapping.

#### Transaction Retry

- `UNAUTHENTICATED` (16) and `PERMISSION_DENIED` (7) from the proxy are configuration
errors, not transient node faults. SDKs must treat them as **fatal / non-retryable**
(do not rotate to other nodes), so a bad/expired token (`401`) or an unauthorized
credential (`403`) surfaces immediately. Rotating would only re-present the same
credential and re-hit the identical failure, so it cannot help.
- gRPC-Web transport-level `UNAVAILABLE` (proxy unreachable, or a probe failure
where one runs) is treated like any other unhealthy-node condition: mark the node
unhealthy and retry on the next node, consistent with existing behavior.

## Test Plan

1. Given `requestHeaders` set, when any request is sent, then every outgoing
request (including any pre-connection probe) carries the configured headers.
2. Given `setRequestHeaders` is called with a reserved key (`content-type`, `te`,
`user-agent`, `grpc-timeout`, `x-grpc-web`, `x-user-agent`, the `grpc-*` prefix,
or `:*`), including in mixed case (e.g. `Content-Type`, `X-Grpc-Web`) and under
either `transport`, when the call is made, then it is rejected case-insensitively
and the stored set is unchanged. A set of only non-reserved headers is accepted,
and `getRequestHeaders` returns it exactly as last set (keys lowercased), matching
what is sent.
3. Given `setRequestHeaders` is called while the client is in use, when the next
request is sent, then it carries the new set even on pre-existing connections.
4. Given a request already in flight when `setRequestHeaders` is called, when that
request completes (including any retries), then its headers remain the set
fixed at dispatch and are unaffected by the new set.
5. Given `requestHeaders` with a non-ASCII value or a `bin` key, when set, then
the SDK rejects them rather than sending mangled metadata.
6. Given default `transport = GRPC` and no `requestHeaders`, when requests are
sent, then behavior is identical to the current SDK (regression guard).
7. Given `transport = GRPC_WEB`, when a query (e.g. account balance) is executed,
then a gRPC-Web framed request is constructed and a successful response returned.
8. Given `transport = GRPC_WEB`, when a transaction is submitted and its receipt
awaited, then both submit and receipt confirmation complete over gRPC-Web and
the transaction reaches consensus (submit-and-confirm, not just read-only).
9. Given a `GRPC_WEB` network whose node addresses carry an explicit
`http://`/`https://` scheme, when a request is sent, then the scheme is honored
and the request is routed to the correct per-node proxy (and its `nodeAccountId`).
10. Given `transport = GRPC_WEB`, when a mirror-node subscription (e.g.
`TopicMessageQuery`) is started, then it runs over the mirror network unchanged.
11. Given a `ClientConfiguration` that declaratively sets `transport` and
`requestHeaders`, when requests are sent, then behavior matches the equivalent
`setTransport`/`setRequestHeaders` calls.
12. Given an auth-requiring proxy and `transport = GRPC_WEB`, when the
`authorization` header is valid then the call succeeds; when omitted/invalid
(HTTP `401`) it fails with `UNAUTHENTICATED` (16) and is not retried; when
authenticated-but-unauthorized (HTTP `403`) it fails with `PERMISSION_DENIED`
(7) and is not retried. A non-zero `grpc-status` trailer (e.g. `16`) with no
`grpc-message` and/or a differing HTTP code is mapped authoritatively and wins
over the HTTP code.
13. Given a non-auth HTTP error without `grpc-status` trailers (e.g. `404`/`500`)
or an unreachable proxy, when classified, then it falls back to retryable
`UNAVAILABLE`, the node is marked unhealthy, and the request rotates to the
next node.

### TCK

The end-to-end tests above should be mirrored in the TCK repository.
Create TCK issues for the header-injection and transport-selection cases and link them
here as reference.

## SDK Example

The following shows a server-side client reaching a gRPC-Web proxy that requires
a bearer token. The assertions for each step are specified in the Test Plan
above.

1. Build a client pointed at the gRPC-Web proxy network.
2. Select the gRPC-Web transport: `client.setTransport(Transport.GRPC_WEB)`.
3. Supply the authentication header:
`client.setRequestHeaders({ "authorization": "Bearer <token>" })`.
4. Execute a simple query (e.g. account balance).
5. Submit a transaction (e.g. a transfer) and await its receipt.
6. Replace the token mid-session with `setRequestHeaders` and execute another
query.
7. Set an invalid/expired token and execute a query; the call fails with
`UNAUTHENTICATED` immediately and is **not** retried across other nodes.

```go
client := hiero.ClientForNetwork(proxyNetwork)
client.SetTransport(hiero.TransportGrpcWeb)
client.SetRequestHeaders(map[string]string{"authorization": "Bearer " + token})

// Read-only query over gRPC-Web through the proxy
balance, err := hiero.NewAccountBalanceQuery().
    SetAccountID(accountID).
    Execute(client)

// Submit-and-confirm over gRPC-Web
response, err := hiero.NewTransferTransaction().
    AddHbarTransfer(sender, amount.Negated()).
    AddHbarTransfer(recipient, amount).
    Execute(client)

receipt, err := response.GetReceipt(client)
// expect receipt.Status == hiero.StatusSuccess, confirmed over gRPC-Web through the proxy
```
