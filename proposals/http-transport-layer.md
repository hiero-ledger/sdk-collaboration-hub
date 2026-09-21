# HTTP Transport Layer for the Hiero SDKs

**Date Submitted:** 2026-09-18

## Summary

This proposal introduces a **shared HTTP transport layer** for mirror node REST access in all Hiero SDKs. It defines
a payload-agnostic transport interface that an application or platform can replace, and a mirror REST adapter above it
that owns base-URL resolution, path typing, retry, backoff and timeouts.

Today every SDK opens HTTP to the mirror node from inside the class that needs it. There is no transport object, no
shared connection pool, no owner, and no single retry policy. The same logical read therefore behaves differently in
each SDK, and differently between two call sites of the *same* SDK: attempt counts, timeouts, retryable status codes
and identity-header handling are all decided per call site.

The design is two namespaces with one seam between them:

| Namespace | Responsibility | Replaceable by |
|-----------|----------------|----------------|
| `http` | One request in, one response out. No retry, no status interpretation, no mirror-node knowledge. | an application (corporate proxy, mTLS, tracing) or a platform (Android) |
| `mirrorNode.http` | Base-URL resolution, path typing, retry, backoff, `Retry-After`, timeout bounds. | not intended to be replaced |

The seam is what makes "inject your own HTTP stack" a two-method interface rather than a fork of the retry loop, and
it is what lets seven SDKs implement the same SPI instead of each re-deriving one.

**On Android the SPI is not an enhancement, it is the only way mirror REST ships at all.** `java.net.http` has never
been part of the Android API — it is absent from the platform's public API surface at every level, and **no
core-library desugaring variant supplies it**, so it is not something an application can configure its way to.
`hiero-sdk-java` routes every mirror REST call through it with no fallback path, so all of them fail at runtime on
Android, on a project that advertises Android 26+ support. With an injectable
transport an Android application supplies an OkHttp-backed one and the feature works. The same seam answers the
browser and React Native targets of the JavaScript SDK, whose HTTP stacks likewise belong to the platform. See
[Conformance profiles](#conformance-profiles).

All changes are additive: no existing SDK signature changes. Two timing defaults move and two bounds are new, all
four listed with their per-SDK before and after in [Compatibility](#compatibility), alongside the behavior changes
that land.

> **Out of scope.** Base URL, port and localhost rewriting belong to the
> [ingress proposal](https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/proposals/mirror-node-ingress-endpoint-standardization.md).
> Streaming is excluded deliberately: `roundTrip` buffers the whole response, which is what makes a retry replayable.
> Topic subscriptions and gRPC-Web server streaming need a second, composed interface. Query-level product behavior —
> auto-freeze, chunk aggregation, missing-entity semantics, integer precision — is a query-product question rather
> than a transport one. It is specified separately, in a companion proposal opened alongside this one; this document
> neither restates nor contradicts it.
>
> **Application identification is also out of scope.** The `x-user-agent` header is owned entirely by the SDK and both
> spellings are reserved on the header setters, which is what every SDK does today. Letting an application contribute
> its own token is a telemetry question rather than a transport one, and it reaches the consensus leg as much as the
> mirror one. Reserving now costs nothing and forecloses nothing: the setters are new, so no caller breaks, and either
> way out — adding a setter later, or relaxing the reservation later — is additive.

**Dependencies on merged policy:**

- [client-grpc-deadline-configuration](https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/proposals/client-grpc-deadline-configuration.md)
  — defines `requestTimeout`, which bounds the attempt loop here.
- [mirror-node-ingress-endpoint-standardization](https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/proposals/mirror-node-ingress-endpoint-standardization.md)
  — owns base-URL rewriting, which this document does not touch.
- [account-balance-query-mirror-node-migration](https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/proposals/account-balance-query-mirror-node-migration.md)
  — owns the one case where a `2xx` is an error.

---

## New APIs

Durations use the `duration` basic type, which this PR adds to
[`guides/api-guideline.md`](../guides/api-guideline.md) — nanosecond precision, matching the `time`, `dateTime` and
`zonedDateTime` entries it joins. It maps to `time.Duration` in Go and `java.time.Duration` in Java, which is what both
SDKs already use on `Client` today, and to a millisecond number in TypeScript and Python.

A duration literal in `@@default` and `@@min` is an integer followed by one of `ns`, `ms`, `s`, `m` or `h` — `30s`,
`250ms` — and a bare `0` is zero duration in any unit. The guideline row this PR adds carries the same rule, so every
proposal after it spells a timeout the same way.

### namespace `http`

This namespace carries no mirror-node vocabulary, because the contract it describes is plain HTTP: an opaque body with
its content type, one request in, one response out. Naming a generic pipe after its first consumer would make any
second consumer a rename of public API.

> gRPC-Web is not that second consumer, and the reason is streaming rather than trailers. A unary gRPC-Web call
> carries its status and trailers in the final frame of the response *body*, which a buffered `roundTrip` conveys
> perfectly well. What this pipe cannot carry is a server stream, by design. Reuse would be its own proposal, keeping
> gRPC framing, status handling and retry out of the mirror REST adapter.

#### HttpMethod

```
namespace http

enum HttpMethod {
    GET
    HEAD
    POST
    PUT
    PATCH
    DELETE
    OPTIONS
    TRACE
    CONNECT
}
```

> Only `GET` and `POST` are reachable from mirror REST today. The enumeration is complete so it never has to be
> reopened. A `string` field would admit `"get"`, `"Get"` and `"PROPFIND"`.

#### HttpRequest

```
@@finalType
HttpRequest {
    @@immutable method: HttpMethod
    @@immutable @@pattern("^https?://[^\s]+$") url: string
    @@nullable @@immutable body: bytes
    @@nullable @@immutable contentType: string
    @@immutable headers: map<string, string>
    @@nullable @@immutable @@min(0) deadline: duration
}
```

> `url` is absolute and already resolved — the transport never joins anything. `body` and `contentType` are null on a
> bodyless method. `headers` is never null; an empty map is the absence of headers, and header names are lowercase
> (see [Header representation](#header-representation)).
>
> `deadline` bounds **the whole exchange, body included** — not time-to-first-byte. A transport must abandon the
> exchange and raise `timeout-error` when it elapses. It is null when the caller imposes no bound of its own. The
> field exists because a deadline the layer above merely *waits out* is not a deadline: see
> [Cancellation and deadlines](#cancellation-and-deadlines).

#### HttpResponse

```
@@finalType
HttpResponse {
    @@immutable statusCode: uint16
    @@immutable body: bytes
    @@immutable headers: map<string, list<string>>
}
```

> Response header names are **ASCII-lowercased**, and a header may repeat. Both are normative; see
> [Header representation](#header-representation).

#### HttpTransportConfiguration

```
@@finalType
HttpTransportConfiguration {
    @@immutable @@min(0) @@default(0) connectTimeout: duration
    @@immutable @@min(0) @@default(5) maxRedirects: uint16
    @@immutable @@min(1) @@default(33554432) maxResponseBytes: int64
    @@immutable defaultHeaders: map<string, string>
}
```

> `connectTimeout` of `0` means "keep the platform stack's own bound", not "no bound"; it covers TCP connect and the
> TLS handshake together. `maxResponseBytes` (32 MiB) caps what a mirror node can make the SDK allocate, since the
> body is buffered.

#### Cancellation

A caller's request to end an exchange early. The adapter creates one per attempt and the transport observes it; it is
never created by the transport.

```
@@finalType
Cancellation {
    @@threadSafe(cancellation)
    bool isCancelled()

    @@threadSafe(cancellation)
    void onCancel(callback: function<void run()>)

    @@static
    Cancellation none()
}
```

> This type exists because the languages disagree about *where* cancellation lives — ambient in Go, a parameter in
> TypeScript, absent from standard Java — and a parameter every binding can name is the only way the SPI has one
> signature. `none()` is the value for a call with no caller-supplied cancellation; it is never null.
>
> **The SDK constructs these; a transport only observes them.** How one is built from a caller's native cancellation
> is language-local and deliberately unspecified. `none()` is the only factory declared here because it is the only
> one a transport implementer ever needs; the adapter, which every SDK must build, constructs the rest from whatever
> its language offers.
>
> **Lifetime and callbacks.** One `Cancellation` serves one call — every attempt and every page included — and the
> adapter passes the caller's instance through to each attempt unchanged. `onCancel` registered on an instance that
> has already been cancelled runs its callback immediately rather than never. There is no unregister, and none is
> needed: the instance does not outlive the call, so a callback cannot either.
>
> Spelled `Cancellation` rather than `CancellationSignal` to avoid colliding with `android.os.CancellationSignal` on
> the platform this document names as its reason to exist.

#### HttpTransport

The SPI. This is the type an application, a platform or a test implements.

```
abstraction HttpTransport {
    @@async
    @@threadSafe(transport)
    @@throws(connection-error, timeout-error, unknown-host-error, tls-error, client-closed-error, cancelled-error, response-too-large-error)
    HttpResponse roundTrip(request: HttpRequest, cancellation: Cancellation)

    @@threadSafe(transport)
    void close(@@min(0) closeTimeout: duration)
}
```

An implementer must satisfy the following; everything not listed is free.

- **A non-2xx status is a successful exchange.** `404` and `503` are returned as an `HttpResponse` carrying their
  `statusCode`. The transport must not fail on a status code. The layer above reads the code and decides.
- **Only a failure to obtain a response fails the call**, and it fails with one of the seven identifiers below.
- **No policy at this layer.** No retry, no backoff, no rate limiting, no circuit breaking, no status interpretation.
  Redirect bounding and the identity-header rule are the two exceptions; both are properties of the HTTP stack rather
  than of mirror semantics (see [Default transport policy](#default-transport-policy)).
- **`roundTrip` is safe to call concurrently**, including concurrently with `close`.
- **`close` aborts; it drains first where the runtime permits.** It stops accepting new work, waits up to
  `closeTimeout` for exchanges already in flight, then cancels whatever remains. **Abort is the floor and drain is
  best effort**: a runtime that cannot wait without blocking its only thread may abort immediately and still conform.
  It is idempotent and never reports failure — a bounded shutdown is the guarantee, not an outcome.
- **A runtime that cannot wait without blocking its event loop must expose an asynchronous form of `close`, and that
  form is the documented primary one in that language.** The synchronous declaration is the contract floor, not a
  recommendation to prefer it: in JavaScript the drain is only observable through a promise.
- **`close` releases only what the transport itself owns.** A transport wrapping a client the application built must
  not shut that client down; see [Ownership and lifetime](#ownership-and-lifetime). A third-party `close` that does
  nothing is a conforming `close`.
- **After `close`, `roundTrip` fails fast** with `client-closed-error` and must not open a new connection or construct
  a replacement client.
- **The body is fully buffered** before `roundTrip` returns, bounded by `maxResponseBytes`. This is what lets the
  layer above replay a request without body-lifecycle discipline at every call site.

> **Nothing is at stake in the gap between abort and drain.** Every mirror REST endpoint in scope is read-only and
> idempotent — which is why this document retries `POST` — so an aborted exchange loses no work and leaves no partial
> state. Drain is a tidiness property for long-lived servers, not a correctness one.

> `close` is not annotated `@@async`, because every SDK's `Client.close()` is synchronous today and making this one
> asynchronous would make that one asynchronous — a breaking change on a widely used method. Per-runtime mapping:
>
> | Runtime | Bounded drain | Mapping |
> |---------|---------------|---------|
> | JVM (Java 21+) | yes | `shutdown()` → `awaitTermination(closeTimeout)` → `shutdownNow()`. Plain `close()` is unbounded and must not be used on its own. |
> | Go | yes | stop accepting work, wait on the in-flight group up to `closeTimeout`, then cancel the transport's abort context. |
> | Node | drain is asynchronous | `Promise.race([agent.close(), delay(closeTimeout)])` then `agent.destroy()`, not awaited by `Client.close()`. |
> | Browser / React Native | no | abort outstanding requests immediately. There is no pool handle to drain. |

#### Cancellation and deadlines

`roundTrip` is declared with one parameter, but a deadline the layer above merely *waits out* is not a deadline. A
helper holding only an opaque asynchronous result can stop waiting; it cannot end the exchange. Racing a hung request
against a timer in JavaScript leaves the socket open and the request in flight; `CompletableFuture.orTimeout` in Java
leaves the connection established. With a five-attempt budget that is five leaked sockets per logical call, out of the
private pool this document specifies, and `cancelled-error` is unreachable because the transport is never told.

Two mechanisms therefore carry the bound, and both are normative:

1. **`HttpRequest.deadline`** — portable, expressible in every language, and honoured by the transport itself.
2. **`Cancellation`**, a declared parameter rather than a per-language signature difference. Each language adapts its
   native mechanism to it; the SPI signature itself is the same everywhere.

| Language | How `Cancellation` is supplied | `roundTrip` |
|----------|--------------------------------|-------------|
| Go | wraps a `context.Context`; `IsCancelled()` is `ctx.Err() != nil` | `RoundTrip(req HttpRequest, c Cancellation) (HttpResponse, error)` |
| TypeScript / JavaScript | wraps an `AbortSignal`; `onCancel` registers an `abort` listener | `roundTrip(request: HttpRequest, cancellation: Cancellation): Promise<HttpResponse>` |
| Java | a plain object the adapter creates and completes | `CompletionStage<HttpResponse> roundTrip(HttpRequest request, Cancellation cancellation)` |
| Rust | wraps whatever the runtime supplies (`tokio_util::sync::CancellationToken` where present) | `async fn round_trip(&self, req: HttpRequest, cancel: Cancellation) -> Result<HttpResponse, TransportError>` |

> **Java returns `CompletionStage`, not `CompletableFuture`**, because
> [`guides/api-best-practices-java.md`](../guides/api-best-practices-java.md) requires every `@@async` method to
> return `java.util.concurrent.CompletionStage<T>`. An implementation may still use a `CompletableFuture` internally.
>
> A language whose idiom is an ambient context may **additionally** accept it — a Go binding may take
> `ctx context.Context` as a first parameter — provided it derives the `Cancellation` from it, so a transport written
> against the declared signature still works.

> **Java implementers: do not rely on cancelling the returned future.** `HttpClient.sendAsync(…).cancel(true)` does
> abort the exchange, but only because `java.net.http` returns an internal future that carries a `Cancelable` through
> its derived stages. That is a privilege of that one stack, not `CompletableFuture` semantics: a generic
> `CompletableFuture` completed from an OkHttp, Ktor or Retrofit callback ignores `cancel` entirely — which is exactly
> the adapter this document asks applications to write. Note also that on the JDK's own future `cancel(true)` returns
> `true` while `isCancelled()` returns `false`, so a retry helper branching on `isCancelled()` will misclassify its
> own cancellation as a transport failure and retry it.

> **Do not map `deadline` onto `java.net.http.HttpRequest.timeout()` alone.** That bounds time to *response received*,
> not body completion: a server that sends headers immediately and then drips a 100-byte body for 50 s runs to
> completion under a 2 s request timeout. A slow mirror node is unbounded under that mapping, and
> `response-too-large-error` does not help — a drip feed never trips a size cap.

#### DefaultHttpTransport

The SDK-supplied implementation, used when no transport is injected.

```
@@finalType
DefaultHttpTransport extends HttpTransport {
    @@static
    DefaultHttpTransport create(configuration: HttpTransportConfiguration)
}
```

> Construction lives on the concrete type rather than on `HttpTransport`, because a static factory on the abstraction
> is not implementable by a third party in most target languages.

#### Transport error identifiers

`@@throws` identifiers are stable across languages and assertable in all seven SDKs. The second column is **whether
repeating the exchange can help.** Without it, a mistyped hostname costs the full attempt budget before an obvious
error surfaces, and certificate validation is re-run purely to burn attempts.

| Identifier | Raised when | Retryable |
|------------|-------------|-----------|
| `connection-error` | Connection refused, reset, or unreachable | Yes |
| `timeout-error` | The per-attempt deadline elapsed | Yes |
| `unknown-host-error` | The host name does not resolve | No |
| `tls-error` | Certificate verification or handshake failure | No |
| `client-closed-error` | The transport was closed | No |
| `cancelled-error` | The caller cancelled the call | No |
| `response-too-large-error` | The body exceeded `maxResponseBytes` | No |

> **Why `response-too-large-error` is an error and not a truncation.** The obvious implementation of a body cap — read
> at most N bytes — truncates silently, and the truncated bytes then reach a JSON or protobuf parser as malformed
> data, producing an error that names the wrong cause. Read one byte past the cap and fail.
>
> **Why `timeout-error` is separate from `connection-error`.** Nothing consumes the distinction yet, but "the server
> took too long" and "the server was unreachable" justify different backoff, and a taxonomy that merges them cannot be
> un-merged later without a breaking change.

**Who must produce these identifiers.** The SDK's own `DefaultHttpTransport` **must**, in every SDK — that is where
the conformance tests live and where the native-error mapping is knowable. A third-party transport **should**, but may
instead signal only *retryable* or *terminal*; demanding a seven-way classification of an application team wrapping
OkHttp or `fetch` would turn a two-method interface back into homework, and the extension point is the thing this
document exists to make cheap.

**A transport failure the adapter does not recognise is treated as non-retryable.** This is the case that will
actually occur — a hand-written adapter throwing a bare `IOException` or `Error` — and leaving it unstated decides, by
accident, whether a badly wrapped corporate proxy burns the full attempt budget on every call. Non-retryable is the
safe direction: a wrongly-terminal error surfaces a visible failure after one attempt, a wrongly-retryable one costs
the whole budget and hides the cause.

> **`unknown-host-error` does not distinguish permanent from temporary DNS failure, because one SDK cannot.**
> `java.net.UnknownHostException` carries no code and no fields — OpenJDK's resolver collapses every `getaddrinfo`
> failure into it without branching on `EAI_NONAME` / `EAI_AGAIN`, and `InetAddressResolver.lookupByName` declares
> only that type, so even a custom resolver cannot surface more. The only discriminator is a locale- and
> platform-dependent `gai_strerror` string that the JDK omits unless enhanced exceptions are enabled. Go
> (`net.DNSError.IsNotFound`) and Node (`ENOTFOUND` vs `EAI_AGAIN`) *can* split it; specifying a split that one of the
> three largest SDKs must fake is worse than not splitting it. **All name-resolution failures map to
> `unknown-host-error` and are not retried.** A transient SERVFAIL therefore fails fast — a deliberate trade, recorded
> here so it is not rediscovered as a bug.

### namespace `mirrorNode.http`

```
namespace mirrorNode.http
requires http
```

This namespace also contains `MirrorNodeHttpClient` and `MirrorNodeRestPath`, which are normative but **not public**
and are therefore specified under [The mirror REST adapter](#the-mirror-rest-adapter) rather than here.

#### MirrorNodeHttpRetryPolicy

```
@@finalType
MirrorNodeHttpRetryPolicy {
    @@immutable @@min(1) @@default(5) maxAttempts: uint16
    @@immutable @@min(0) @@default(30s) perAttemptTimeout: duration
    @@immutable @@min(0) @@default(0) totalDeadline: duration
    @@immutable @@min(0) @@default(250ms) initialBackoff: duration
    @@immutable @@min(0) @@default(8s) maxBackoff: duration
    @@immutable @@default([408, 429, 500, 502, 503, 504]) retryableStatusCodes: list<uint16>

    @@static
    MirrorNodeHttpRetryPolicy defaults()

    MirrorNodeHttpRetryPolicy withMaxAttempts(@@min(1) maxAttempts: uint16)
    MirrorNodeHttpRetryPolicy withPerAttemptTimeout(@@min(0) perAttemptTimeout: duration)
    MirrorNodeHttpRetryPolicy withTotalDeadline(@@min(0) totalDeadline: duration)
    MirrorNodeHttpRetryPolicy withInitialBackoff(@@min(0) initialBackoff: duration)
    MirrorNodeHttpRetryPolicy withMaxBackoff(@@min(0) maxBackoff: duration)
    MirrorNodeHttpRetryPolicy withRetryableStatusCodes(retryableStatusCodes: list<uint16>)
}
```

> **`defaults()` and `withX` are the only sanctioned way to build one, and that is deliberate.** (`default` is a
> reserved word in Java and in C++, so the obvious spelling does not compile in two of the seven target languages.) `@@default` on an
> `@@immutable` field is well defined in a language with builders or object spread and undefined in a language whose
> value types have zero values. In Go, `MirrorNodeHttpRetryPolicy{MaxAttempts: 5}` yields `perAttemptTimeout = 0`,
> `initialBackoff = maxBackoff = 0` and `retryableStatusCodes = nil` — a caller who asked for five attempts gets five
> attempts that never retry, silently. Derivation from `defaults()` makes that state unreachable, and it needs no
> builder type: neither the Java nor the JavaScript SDK contains a single builder for its own types.

Three properties of this type are decisions rather than defaults.

1. **It is a value, not client state.** It is resolved per call as *package default → client → query setter*, carried
   into the call by value, which lets concurrent calls hold different budgets while sharing one connection pool and
   keeps the hot path lock-free.

   The third tier is not new API. Several SDKs already expose per-query retry setters on exactly these classes — Go
   has `SetMaxAttempts` on `FeeEstimateQuery`, `MirrorNodeAccountBalanceQuery` and `RegisteredNodeAddressBookQuery`,
   and Java has the same three plus `setMaxBackoff` — and this proposal changes no existing signature, so those
   setters survive and need defined semantics. **A query setter overrides only the field it names**, leaving the
   others at the client value: `setMaxAttempts(5)` does not reset `perAttemptTimeout`.

2. **`perAttemptTimeout` is its own field**, not a reinterpretation of the existing `requestTimeout`. `requestTimeout`
   is documented in every SDK as the budget for a whole operation; redefining it as a per-attempt bound silently
   multiplies the worst case by `maxAttempts` for every existing caller. It would also contradict merged policy:
   [client-grpc-deadline-configuration](https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/proposals/client-grpc-deadline-configuration.md)
   defines `requestTimeout` as governing "the **overall execution duration** of a `Transaction` or `Query` (including
   retries, backoff, and node rotation)".

3. **The retryable set is data.** A rule in prose ("5xx") is read differently by each implementer: `501`, `505`,
   `506`, `507`, `508`, `510` and `511` all describe a server that will answer identically next time. An explicit list
   has one reading.

> **Why `5` and `30 s`.** The pair governs two regimes. Against a **slow** mirror node, with a 30 s per-attempt bound
> and a `requestTimeout`-inherited total of two minutes, the fourth attempt hits the total deadline — so an attempt
> count above four is unreachable there, and the ten that Java and JavaScript carry today are already dead letters.
> Against a **fast** failure — a refused connection, or the `web3` throttle answering `429` in milliseconds — wall
> clock is pure backoff: with `initialBackoff: 250 ms` the four retry caps are 0.25 / 0.5 / 1 / 2 s, so a dead
> endpoint is reported after **1.875 s on average and 3.75 s at worst**. Three attempts would report it in 0.375 s,
> ten would mean 19.875 s of backoff on average and 39.75 s at worst. Five is the judgement this document makes, and
> it buys the most on the fast retryable statuses, which is where the budget actually does work: no mirror node
> module sends `Retry-After`, so computed backoff governs every endpoint family rather than only some. No SDK holds
> this pair today, so every one of them moves — that is the cost, and it is itemised in
> [Compatibility](#compatibility).

> There is no connect timeout on this type. It is fixed when the transport is built, not per call, so a per-call field
> for it would silently do nothing. It lives on `HttpTransportConfiguration` and on `Client`.

> **`maxAttempts` is counted per request, not per call.** It is one initial request plus up to `maxAttempts - 1`
> retries *of that request*. A paginated walk gets the budget afresh for each page: a six-page read in which every
> page succeeded first time has spent no retries, and must not be treated as having spent six attempts. What spans
> the whole walk is `totalDeadline`, which every page and every backoff draws down.

> **`perAttemptTimeout` of `0` means "no per-attempt cap", not "expire immediately".** The total bound still applies,
> and each attempt is bounded by whichever of the two is tighter — at `0` that is always the remaining total.
> `@@min(0)` admits the value, so it needs a stated meaning.
> **`totalDeadline` of `0` means "inherit `Client.requestTimeout`", not "no bound".** It bounds one *call* as defined
> in [What a call is](#what-a-call-is) — a paginated walk gets one budget, not one per page. Merged policy already defines
> `requestTimeout` as the overall execution duration of a `Transaction` or `Query` including retries, backoff and node
> rotation, and a mirror REST query is a `Query` — so the answer to "what bounds the whole attempt loop?" is settled
> policy rather than a new number this document gets to pick. An explicit positive value overrides it, and this type
> offers deliberately **no** way to switch the total bound off, because nothing in the merged definition permits an
> unbounded `Query`. One route out survives and is not closed here: an SDK whose `Client.requestTimeout` accepts `0`
> passes that through, and the call is then unbounded. Clamping it would change the behaviour of an existing setter,
> which this proposal does not do.
>
> This is also what keeps `maxAttempts × perAttemptTimeout` from being the thing that determines worst case. Whatever
> that pair settles at, the total is bounded.

> **`initialBackoff`, not `minBackoff`.** With full jitter the draw is from
> `[0, min(maxBackoff, initialBackoff × 2^n))`, so there is no floor — the value is the base of the exponential.
> `Client.minBackoff` in Java, JavaScript and Swift today *is* a real floor, and reusing that name on the same object
> for a different meaning is how a knob gets set wrong.

---

## Updated APIs

### Client

All additive. No existing signature changes.

```
Client {
    Client setMirrorNodeHttpTransport(@@nullable transport: http.HttpTransport)
    @@nullable http.HttpTransport getMirrorNodeHttpTransport()

    Client setMirrorNodeHttpRetryPolicy(policy: mirrorNode.http.MirrorNodeHttpRetryPolicy)
    mirrorNode.http.MirrorNodeHttpRetryPolicy getMirrorNodeHttpRetryPolicy()

    Client setMirrorNodeHttpConnectTimeout(@@min(0) connectTimeout: duration)
    duration getMirrorNodeHttpConnectTimeout()

    Client setMirrorNodeRequestHeaders(headers: map<string, string>)
    Client addMirrorNodeRequestHeader(name: string, value: string)
    map<string, string> getMirrorNodeRequestHeaders()
}
```

- `setMirrorNodeHttpTransport` is the single extension point for Android, corporate proxies, mTLS, tracing wrappers
  and test fakes. Passing null restores the SDK default.
- **Ownership follows construction.** The `Client` closes only a transport it built itself. An **injected transport is
  never closed by the SDK** — it is almost always a wrapper over a process-wide client the application also uses, and
  shutting that down from `Client.close()` would take the application's HTTP stack with it. OkHttp makes this
  concrete: it has no `close()`, and its documented shutdown recipe rejects all future calls on that client and
  crashes any call against its cache.
- `setMirrorNodeHttpConnectTimeout` must be called before the first mirror REST call, because the transport is built
  once per `Client`.
- `setMirrorNodeRequestHeaders` and `addMirrorNodeRequestHeader` **reject `user-agent` and `x-user-agent`**, matched
  case-insensitively, because the SDK owns that header.
- **`requestTimeout` and the gRPC `maxAttempts` keep their current meanings.** `requestTimeout` remains the total
  budget for an operation and is **not** reused as an HTTP per-attempt bound. It does, per merged policy, bound the
  mirror REST attempt loop as a whole: `totalDeadline` inherits it by default. The `maxAttempts` on
  `MirrorNodeHttpRetryPolicy` is a separate value governing HTTP only; the two must not be aliased. **This severs an
  existing link:** three Go mirror REST call sites read gRPC retry state today — two consult `Client.getMaxAttempts()`
  and one initialises from the gRPC `maxAttempts` constant. Separating them is the point of this document, and it is
  listed as an intended behavior change under [Compatibility](#compatibility) rather than left to be discovered.
- **Existing per-query retry setters keep working and gain a defined meaning.** `FeeEstimateQuery.setMaxAttempts`,
  `MirrorNodeAccountBalanceQuery.setMaxAttempts`, `RegisteredNodeAddressBookQuery.setMaxAttempts` and their
  `setMaxBackoff` siblings override the named field of the resolved policy for that query's calls, and nothing else.
- **An explicit per-call timeout argument maps to `totalDeadline`.** Several mirror REST queries already take one —
  `execute(client, timeout)` — and no two agree on what it means, within an SDK as much as between two. Measured:
  every Java query that has the argument applies it per HTTP request; in JavaScript the balance queries document and
  implement a total budget, the address-book query applies it per request *per page* and documents nothing, and the
  fee-estimate and contract-call queries take no timeout at all. There is no coherent existing meaning to preserve,
  so the argument becomes a per-call override of `totalDeadline` in every SDK, and the call sites that move are named
  in the release note.
- **`close()` also releases the mirror HTTP transport it built**, with a grace period of **5 seconds** by default. The
  value is deliberately far below the per-attempt read timeout, because waiting out a stalled read on a shutdown path
  is worse than aborting it. An SDK that already has a client-wide shutdown budget should use it instead. Where the
  runtime cannot wait without blocking, the grace period collapses to an immediate abort — see the mapping table under
  [HttpTransport](#httptransport).

> A language whose native idiom is a function rather than an object (a JS `fetch`) may accept that form and document
> it as this method.

### Existing mirror REST query classes

No public signature changes. `FeeEstimateQuery`, `RegisteredNodeAddressBookQuery`, `MirrorNodeAccountBalanceQuery`,
`MirrorNodeContractCallQuery`, `MirrorNodeContractEstimateGasQuery`, `MirrorNodeTokenBalanceQuery`, entity-id
resolution and the `AccountId` / `ContractId` populate helpers become callers of the `Client`-owned transport. They
stop constructing HTTP clients, stop implementing retry, and stop setting identity headers of their own.

---

## Internal Changes

### The mirror REST adapter

`MirrorNodeHttpClient` and `MirrorNodeRestPath` are specified here rather than under **New APIs** because they are
**not public API** — the design-doc template reserves that section for public surface. They are normative all the
same: every SDK must have this layer, with these responsibilities, or the drift this document exists to remove comes
straight back. What is deliberately not required is that anyone outside the SDK can reach them.

The transport SPI above must be public, because an injectable transport nobody can reach is pointless. The adapter
need not be, and exporting it would make "call any mirror REST endpoint" a supported feature in seven SDKs forever.
The decision is one-way by design: a later release can export it, and no release can unexport it.

#### MirrorNodeHttpClient

The shared helper. It is the layer that drifted in every SDK, and an unnamed layer is re-invented per language and per
call site.

```
@@finalType
MirrorNodeHttpClient {
    @@immutable baseUrl: string
    @@immutable retryPolicy: MirrorNodeHttpRetryPolicy

    @@static
    MirrorNodeHttpClient create(baseUrl: string, transport: http.HttpTransport, retryPolicy: MirrorNodeHttpRetryPolicy)

    @@async
    @@threadSafe(client)
    @@throws(connection-error, timeout-error, unknown-host-error, tls-error, client-closed-error, cancelled-error, response-too-large-error, retries-exhausted-error, deadline-exceeded-error)
    http.HttpResponse get(path: MirrorNodeRestPath, cancellation: http.Cancellation)

    @@async
    @@threadSafe(client)
    @@throws(connection-error, timeout-error, unknown-host-error, tls-error, client-closed-error, cancelled-error, response-too-large-error, retries-exhausted-error, deadline-exceeded-error)
    http.HttpResponse post(path: MirrorNodeRestPath, contentType: string, body: bytes, cancellation: http.Cancellation)
}
```

- **It passes the caller's `Cancellation` straight through**, unwrapped, to every attempt. It derives each attempt's
  `HttpRequest.deadline` from `perAttemptTimeout` and the call's remaining `totalDeadline`, but it never invents a
  cancellation and never swallows one — a caller cancelling a mirror read must reach the socket.
- **It exposes no lifecycle method.** It does not own the transport it was handed and must not close it. One transport
  can back several `MirrorNodeHttpClient` instances, which is what lets a single connection pool serve several mirror
  nodes.
- **It returns the response as received** on any status the policy does not retry, including 4xx.
- **It adds exactly two errors of its own.** `retries-exhausted-error` is raised when a retryable status survived
  every attempt. `deadline-exceeded-error` is raised when the call's `totalDeadline` elapsed, or when an honoured
  `Retry-After` exceeded the time left in it. The clock running out is not the same as a node failing repeatedly, and
  a caller needs the difference to know whether to raise `requestTimeout` or go and look at the node. Neither is a
  transport failure, so neither appears on `HttpTransport`. Because the adapter is internal, so are both identifiers:
  what reaches an application is each query's existing error type, carrying a message built from the last response as
  described under [Response Codes](#response-codes).
- **After `Client.close()` the adapter starts no new attempt** and interrupts any backoff sleep in progress, failing
  with `client-closed-error`. A transport the `Client` built refuses new work itself once closed; an injected
  transport is never closed and would otherwise go on serving retries for a `Client` that has already shut down, for
  as long as the remaining `totalDeadline` allowed.
- **A `POST` is retried like a `GET`.** Every mirror REST endpoint in scope is read-only, so the method is not a
  reason to suppress a retry. The body must be replayable byte-for-byte on each attempt.

#### MirrorNodeRestPath

```
@@finalType
MirrorNodeRestPath {
    @@immutable @@pattern("^/[^\s]*$") value: string

    @@static
    @@throws(invalid-path-error)
    MirrorNodeRestPath of(path: string)

    @@static
    @@throws(invalid-path-error)
    MirrorNodeRestPath fromNextLink(link: string)
}
```

The type is the mechanism: **no mirror REST call site can name a foreign host**, because the only way to reach
`MirrorNodeHttpClient` is through this type and this type cannot express one. Nothing downstream has to check. `of`
rejects, in addition to the pattern:

- anything without a leading `/`, which is how an absolute `https://evil.example/…` is caught;
- a `//host` prefix, which is protocol-relative and never what a caller meant;
- any `..` path segment.

`fromNextLink` converts a mirror node `links.next` value into a path. It rejects any value containing `://`, and
strips the leading `/api/v1` that the mirror node includes and the base URL already carries. It raises
`invalid-path-error` on rejection.

**Resolution is plain concatenation** — `trimTrailingSlash(baseUrl) + path` — and this is normative. Every language
offers a URL-reference resolver (`URI.resolve`, `url.URL.ResolveReference`, `new URL(ref, base)`), and every one of
them lets an absolute or protocol-relative reference replace the host outright, which is the failure this type exists
to prevent. Concatenation also makes both spellings of a trailing slash on the base URL behave identically.

> The guarantee belongs to `mirrorNode.http`, not to `http`: `HttpRequest.url` is a plain absolute URL, so anything
> holding the transport directly can still address anywhere. That is the correct division — the transport is not
> mirror-aware — but it means the safety property is a property of this type. Together with a paginated result that
> never hands the caller a raw cursor, it closes the confused-deputy path on `links.next` structurally rather than
> with a runtime check.

### Layering

```
  query                       knows what the response means
    |  MirrorNodeRestPath
    v
  MirrorNodeHttpClient        POLICY
    |                         base URL + retry + backoff + jitter + Retry-After
    |                         + per-attempt / total deadlines + status classification
    |  absolute URL, one exchange
    v
  HttpTransport   <-- injectable
    |                         PIPE: one request, one response. No policy.
    v
  native HTTP stack           connect timeout, TLS, redirect cap, pooling
```

An implementer may collapse these into fewer files, but not into fewer responsibilities: the seam between
`MirrorNodeHttpClient` and `HttpTransport` is what an injected transport replaces, and anything policy-shaped that
leaks below it has to be reimplemented by every injector.

### Conformance profiles

An SDK is not one runtime. `hiero-sdk-js` ships **three** build targets that all make mirror REST calls — Node,
browser (`WebClient`) and React Native (`NativeClient`) — and `hiero-sdk-java` ships two, JVM and Android. Several
rules below are unimplementable on the non-server targets, and a specification that does not say so is one that
quietly cannot be met.

| Profile | Runtimes |
|---------|----------|
| **Full** | Node, JVM, Go, Rust, C++, Python, Swift — anything with a controllable HTTP stack |
| **Constrained** | Browser, React Native, Android — the HTTP stack belongs to the platform |

Every rule and every test below is unmarked (both profiles) or marked **Full only**, with the constrained substitute
stated. Six rules differ, and two of them are capability-shaped rather than profile-shaped — Android's OkHttp exposes
a connect timeout, so it is only the browser and React Native that cannot honour one, and only those two are subject
to a same-origin policy:

| Rule | Full | Constrained | Why |
|------|------|-------------|-----|
| Redirect bound | at most 5 hops | the platform's own bound | A browser `fetch` has exactly two redirect policies: follow, with a fixed and unobservable 20-hop cap, or reject outright. `redirect: "manual"` returns an opaque response whose status is `0` and whose header list is empty, so the `Location` cannot be read and the hop cannot be re-issued. Even in Node, a real 5-cap requires `undici.request` with the redirect interceptor; handing that dispatcher to `undici.fetch` silently bypasses it. |
| Body cap | fail on the first byte past `maxResponseBytes`, streaming | buffer, then reject | React Native's `fetch` cannot stream response bodies, which is why that SDK's own gRPC-Web path uses `XMLHttpRequest`. The failure is identical; the allocation is not avoided. |
| Connect timeout *(browser and React Native only)* | `connectTimeout` is honoured | ignored; the platform's own bound applies | `RequestInit` has no connect-timeout member and `AbortSignal.timeout` bounds the whole exchange, not the connect phase. Android is **not** affected: OkHttp exposes `connectTimeout` directly. Test 7 is bound to runtimes that can express one. |
| Error granularity | `unknown-host-error` and `tls-error` are distinguished from `connection-error` | they collapse into `connection-error` | A browser `fetch` surfaces every network failure as one opaque `TypeError`, with no cause to inspect. See the note below for what that means for retry. |
| Private connection pool | required | not expressible | Browser `fetch` and React Native expose no pool handle at all: connection reuse, limits and TLS settings belong to the platform. Nothing is left for the SDK to own, which is also why there is nothing to drain on close. |
| `Retry-After` is readable *(browser and React Native only)* | yes | **no, on the browser** | A cross-origin response header is invisible to script unless the server names it in `Access-Control-Expose-Headers`, and no mirror node module does. Computed backoff is therefore the normal path in a browser whatever an edge proxy sends. React Native is unaffected — its `fetch` enforces no same-origin policy. |

> **The collapse is recognised, not unrecognised.** The "unrecognised failure is non-retryable" rule covers an
> adapter that cannot classify what it caught. A browser that reports every network failure identically has
> classified it — as a network failure — and that maps to `connection-error`, which is retryable. Tests 4 and 5 are
> therefore **Full only**; on the constrained profile a bad hostname or an untrusted certificate costs the attempt
> budget before failing, which is the price of a platform that will not say which happened.

**Android is the reason this proposal exists, not a footnote to it.** `java.net.http` is not part of the Android API
at any level, and **no core-library desugaring variant supplies it** — this is not an omission an application can
configure away. `hiero-sdk-java` routes every mirror REST call through `HttpClient.newHttpClient()` with no fallback,
so *all* of them fail at runtime there, together with the EVM-address resolution behind `AccountId` and `ContractId`
population, while the project advertises Android 26+. An injectable transport is the only way that ships:
an Android application supplies an OkHttp-backed transport. Its corollary is normative — **the SDK-supplied
`DefaultHttpTransport` must be separable or lazily linked**, or the artifact still fails to load on a platform where
the class it references does not exist. "Lazily linked" means no `java.net.http` type is resolved unless the default
transport is actually constructed — which must hold under minification as well as at class load, since a shrinker
that inlines the reference defeats it.

### Ownership and lifetime

- One transport per `Client`, **built lazily on first mirror REST use**. Most clients never make one, and a
  short-lived client should not open a pool it will not use. Lazy construction also means a transport injected any
  time before the first call still wins.
- **A transport the `Client` built is owned by the `Client`** and released by `Client.close()`. **A transport the
  application injected is owned by the application** and is never closed by the SDK. `MirrorNodeHttpClient` instances
  are cheap, per-call, and own nothing.
- **Full profile only:** the default transport must use a **private** connection pool rather than the language's
  process-global default client, so pool limits and TLS settings belong to the SDK and are not affected by anything
  else in the process. On the constrained profile there is no pool to own — see
  [Conformance profiles](#conformance-profiles).

> **Private does not mean isolated from the environment.** The default transport must not lose whatever ambient
> proxy configuration the language's own default client honours. What that is differs sharply, and not intuitively.
> Go's `http.DefaultTransport` reads `HTTP_PROXY` / `HTTPS_PROXY` / `NO_PROXY`, and a hand-built `&http.Transport{}`
> silently does not — a real break for every Go user behind a corporate proxy, and the reason this rule exists.
> Java's `HttpClient` reads the `http.proxyHost` family of **system properties** and never the environment variables.
> Node honours them only once something opts in. So this is a no-regression rule, not a requirement to add
> environment-variable proxying to an SDK that never had it. An application needing more injects a transport, which
> is what the extension point is for.

### What a call is

**A *call* is one query execution, including every page it walks.** An *attempt* is one exchange through the
transport. A paginated read is therefore one call and many attempts, spread over several requests.

The distinction is load-bearing in three places, and every one of them breaks under the other reading:

- **The base URL is fixed for the whole call**, so every page of one walk comes from the same mirror node. Two nodes
  at different ingest heights would otherwise return a silently truncated `RegisteredNodeAddressBook` at HTTP 200 —
  which is then fed to `Client.setNetwork`.
- **`totalDeadline` bounds the whole call**, not one exchange. This is what makes the inherited `requestTimeout`
  meaningful: merged policy scopes it to a `Query`, and a `k`-page walk under a per-exchange reading would get
  `k × requestTimeout` while claiming to be bounded.
- **The retry policy applies per attempt**, so a `503` on page 2 is retried exactly as on page 1, out of the call's
  remaining deadline.

### Base URL selection

A `Client` may hold several mirror node URLs. **The base URL is chosen per call by round-robin over the configured
mirror nodes, and every attempt in that call — on every page — targets the same base.** A transport failure does
**not** mark a mirror node unhealthy.

Both halves are normative because both are currently unspecified and already divergent: **all three SDKs measured
for this document draw uniformly at random** per call, from a list nothing ever demotes — Go's `rand.Intn`, Java's
`healthyNodes.get(random.nextInt(...))`, JavaScript's `Math.floor(Math.random() * ...)`. That makes a two-node network
with one node down a coin flip `retries-exhausted-error` cannot escape, re-flipped on the next call. Two of the three
name the method `getNextMirrorNode`, and one documents it as returning the most healthy node by sort order, over an
implementation that neither sorts nor advances. Round-robin costs one counter and is assertable in a unit test in
every language.

**The counter advances atomically.** It is the only shared mutable state this design puts on the hot path, and a
`Client` is explicitly concurrent, so an unsynchronised read-modify-write would hand all seven SDKs the same race —
one of them has it already.

Demotion on failure is the right end state and the wrong proposal: it is a mirror-node-health design, and no SDK has
the machinery to hang it on today. It is deliberately excluded rather than forgotten.

### Default transport policy

Overridable in full by an injected transport.

- Verify TLS certificates against the platform trust store.
- **Full profile only:** follow at most **5** redirects, and drop `Authorization` and other caller headers on a
  cross-origin hop. On the constrained profile the platform's own bound applies, and only `Authorization` is stripped
  — the fetch specification removes it on the first cross-origin hop, but a custom credential header such as an API
  key survives and a browser implementer cannot strip it. Do not carry a mirror credential in a custom header on the
  constrained profile.
- Never force-upgrade the scheme. Plaintext `http://` is allowed when the mirror base URL is explicitly `http://`,
  which the local-mirror path requires.
- Keep-alive on.
- **`x-user-agent: hiero-sdk-<language>/<semver>` on every request, on every profile, and no `User-Agent`.** The
  header is owned entirely by the SDK: `user-agent` and `x-user-agent` are **reserved keys**, rejected
  case-insensitively by `setMirrorNodeRequestHeaders` and `addMirrorNodeRequestHeader`. There is no caller
  contribution.

  > `x-user-agent` rather than `User-Agent` because a browser cannot set the latter: it is not a forbidden header, but
  > Chrome overrides it silently and `Headers.set` reports success, so an implementer gets no signal and a test
  > asserting it passes locally and fails in a browser. **Every SDK's gRPC path already sends `x-user-agent`**, in Go,
  > Java and JavaScript alike, including from Node where `User-Agent` *is* settable, and the JavaScript SDK's own unit
  > tests assert the custom header. Sending both would invent a difference between runtimes that no SDK has today.
  >
  > **On the browser this header has a cost, and it is stated rather than avoided.** `x-user-agent` is not
  > CORS-safelisted, so every cross-origin mirror request becomes a preflighted one — an `OPTIONS` ahead of each
  > `GET`, cached only for as long as the server's `Access-Control-Max-Age` allows. The Spring-served modules supply
  > that header from the framework's own defaults; the module serving most of the REST surface supplies none, so
  > there the preflight is effectively per request. Dropping the header on the constrained profile would buy the
  > round trip back at the cost of reintroducing exactly the per-runtime divergence it exists to remove, so it stays
  > — and `Access-Control-Max-Age`, alongside `Access-Control-Expose-Headers: retry-after`, is an ask on whoever
  > operates the ingress.

- Header precedence, later winning: `HttpTransportConfiguration.defaultHeaders` < caller headers from `Client` <
  per-endpoint headers set by the query (for example `Content-Type: application/protobuf` on the fee-estimate POST) <
  the identity-header rule above. `defaultHeaders` sits at the bottom because it belongs to the SDK-built transport
  alone: an injected transport is never handed an `HttpTransportConfiguration` and carries whatever defaults its
  implementer gave it.

> Redirect bounding and the identity-header rule are the only two policies below the seam. Both are properties of the
> HTTP stack rather than of mirror semantics, and in most languages the redirect hook is only reachable from the stack
> itself.

### Header representation

**Header names are ASCII-lowercased on both request and response, and response headers are list-valued**
(`map<string, list<string>>`). Comparison is byte equality after lowercasing; no implementation may rely on a
case-insensitive map surviving a copy.

This is a correctness rule, not tidiness. The same wire header produces three different keys today: Go's `http.Header`
canonicalises to `Retry-After`, Node and every browser lowercase to `retry-after`, and Java's `HttpHeaders.map()`
preserves whatever the server sent behind a case-insensitive comparator — a comparator that is **lost the moment the
map is copied into an ordinary `Map`**, which is the obvious way to satisfy a `map<string, string>`:
`new HashMap<>(httpHeaders.map()).get("retry-after")` returns `null`. And because **HTTP/2 lowercases every header
name on the wire while HTTP/1.1 does not**, a shared helper reading `Retry-After` finds it against an HTTP/1.1 mirror
node and misses it against an HTTP/2 one — a backoff failure that appears only under throttling, in production.

List-valued because a single-valued map silently discards all but one value of a repeated header. Neither the Java nor
the JavaScript SDK reads *any* response header today, so `Retry-After` is net-new work everywhere and the model can be
fixed at zero migration cost. That window closes the moment someone implements it.

Request headers stay `map<string, string>`, lowercased. Repeated request headers and trailers are gRPC-Web concerns
and are out of scope here for the same reason gRPC-Web itself is.

### Timeouts

| Bound | Where it lives | Applies to | Default |
|-------|----------------|------------|---------|
| `connectTimeout` | `HttpTransportConfiguration` / `Client` | TCP connect and TLS handshake | platform default |
| `perAttemptTimeout` | `MirrorNodeHttpRetryPolicy` | one attempt, end to end | 30 s |
| `totalDeadline` | `MirrorNodeHttpRetryPolicy` | the whole **call**, every page and every backoff included | the client's `requestTimeout` |
| `deadline` | `HttpRequest` | one attempt, as seen **by the transport** | set per attempt by the adapter |

A refused or blackholed connection should give up in seconds; a large paginated read can legitimately take much
longer. One knob forces a bad compromise in both directions, and every native stack in every target language already
exposes both.

`deadline` is not a fourth knob: it is how `perAttemptTimeout` crosses the seam. The adapter derives it per attempt
and the transport enforces it, because a bound the adapter merely waits out leaves the exchange running — see
[Cancellation and deadlines](#cancellation-and-deadlines).

Where the language has an ambient cancellation or deadline mechanism (a Go `context`, an `AbortSignal`, a cancellation
token), it is passed through the SPI as that language's binding prescribes and wins whenever it is the tighter bound.
Cancelling one mirror read must not require cancelling anything else on the client.

### Retry

Applies to every mirror REST call.

| Condition | Retry |
|-----------|-------|
| `408`, `429`, `500`, `502`, `503`, `504` | Yes |
| `2xx` | No |
| `400`, `404`, any other 4xx | No |
| Any other 5xx (`501`, `505`, `506`, `507`, `508`, `510`, `511`) | No |
| `connection-error`, `timeout-error` | Yes |
| `unknown-host-error`, `tls-error`, `client-closed-error`, `cancelled-error`, `response-too-large-error` | No |
| Any transport failure the adapter does not recognise | No |

- **Backoff is exponential with full jitter:** a uniform draw from `[0, min(maxBackoff, initialBackoff × 2^n))`, where
  `n` is the zero-based retry index. Full jitter rather than a fixed curve, because every SDK retrying a throttled
  mirror node on the same curve re-converges on it. Which RNG supplies the draw is a language-local choice.
- **`Retry-After` always wins, bounded by the remaining total deadline — never by `maxBackoff`.** On a `429`, and on
  any **retryable** `5xx` that carries the header, parse it in both permitted forms — delta-seconds and HTTP-date —
  and wait that long instead of the computed backoff. A date already in the past means "now". If the value exceeds
  the time left in `totalDeadline`, **stop and fail immediately** with `deadline-exceeded-error`; do not retry sooner
  than the node asked. A non-retryable status is never waited on, whatever header it carries.

  > **Today this rule is for edge proxies, not for the mirror node.** No mirror node module sets `Retry-After` on any
  > response. The only `429` the node itself produces comes from the `web3` module's in-process throttle, serving
  > `/api/v1/contracts/call` and the opcodes endpoint, and it is returned with a null header map. The REST modules
  > rate-limit nothing. So computed backoff with full jitter is the normal path on a `429`, the attempt budget is
  > load-bearing on every endpoint family rather than only some, and the `Retry-After` rule exists for whatever an
  > operator puts in front. `x-retry-in` is deliberately **not** specified: it is read by the mirror node's own
  > monitoring client and produced by nothing in that codebase.

  > `maxBackoff` is the wrong bound. It caps a *guess* about how long to wait; `Retry-After` is the node stating the
  > fact the guess approximates. With the shipped defaults the four retry caps are 0.25 / 0.5 / 1 / 2 s, so bounding
  > by `maxBackoff` would answer a throttled node's explicit instruction with four more requests inside 3.75 s —
  > reintroducing exactly the stampede that full jitter exists to prevent, in the one case where guessing is
  > unnecessary. The loop's real bound is `totalDeadline`, and a `Retry-After: 30` sits comfortably inside it.
- **A terminal failure consumes exactly one attempt.** It must not be retried and must not consume the remaining
  budget.
- **Sleeping is cancellable.** A caller cancellation or the total deadline must interrupt a backoff sleep, not be
  discovered after it. A caller cancellation surfaces as `cancelled-error`, the total deadline as
  `deadline-exceeded-error`, and a `Client.close()` during a sleep as `client-closed-error`.
- **Attempts within one call do not rotate mirror nodes.** The base URL is chosen once when the call starts, by the
  rule in [Base URL selection](#base-url-selection), and every attempt in that call targets the same base. This is the
  opposite of gRPC retry, which rotates nodes between attempts, so an implementer who assumes symmetry will get it
  wrong. It is, however, not a change: Go, Java and JavaScript all resolve the base URL once before entering the
  retry loop today, so the rule writes down what they already do. Rotating on HTTP retry is defensible and can be
  specified later.

### Mapping native failures to identifiers

The identifiers are only a conformance lever if seven teams derive them the same way, and this is the derivation. It
is normative for each SDK's `DefaultHttpTransport` and advisory for a third-party transport.

| Identifier | Go | Java (`java.net.http`) | JS (`fetch` / undici) |
|------------|----|------------------------|-----------------------|
| `connection-error` | `*net.OpError`, `syscall.ECONNREFUSED`, `ECONNRESET`, `EHOSTUNREACH`, unexpected EOF | `ConnectException`, `SocketException`, `EOFException`, other `IOException` | `TypeError: fetch failed` with `cause.code` in `ECONNREFUSED`, `ECONNRESET`, `EHOSTUNREACH`, `UND_ERR_SOCKET` |
| `timeout-error` | `ctx.Err() == context.DeadlineExceeded`, `*net.Error` with `Timeout()` | `HttpTimeoutException`, `HttpConnectTimeoutException` | `TimeoutError`, `AbortError` raised by the deadline rather than by the caller, `UND_ERR_HEADERS_TIMEOUT`, `UND_ERR_BODY_TIMEOUT` |
| `unknown-host-error` | `*net.DNSError` | `UnknownHostException` | `cause.code` in `ENOTFOUND`, `EAI_AGAIN` |
| `tls-error` | `*tls.CertificateVerificationError`, `x509.UnknownAuthorityError`, `x509.HostnameError`, `x509.CertificateInvalidError` | `SSLHandshakeException`, `CertificateException`, other `SSLException` | `cause.code` beginning `ERR_TLS_`, `CERT_HAS_EXPIRED`, `UNABLE_TO_VERIFY_LEAF_SIGNATURE` |
| `client-closed-error` | the transport's own closed sentinel | `IllegalStateException` after `shutdown()`, or the transport's own sentinel | the transport's own sentinel |
| `cancelled-error` | `ctx.Err() == context.Canceled` | cancellation signalled by the caller | `AbortError` whose signal is the caller's |
| `response-too-large-error` | the transport's own sentinel, raised one byte past the cap | the transport's own sentinel | the transport's own sentinel |
| *(unrecognised)* | — | — | treated as non-retryable, per the table above |

> `timeout-error` and `cancelled-error` are the same native value in several stacks — a Go `context` cancellation, a
> JS `AbortError` — and are distinguished by *whose* deadline fired, not by the error. The transport knows: it owns
> the deadline it derived from `HttpRequest.deadline`, and anything else came from the caller.

### Compatibility

No existing signature changes, so no SDK breaks at compile time. The connect bound ships unchanged and the total bound
defers to a value each `Client` already has:

| Bound | Before | Default after | Why |
|-------|--------|---------------|-----|
| connect | the platform stack's own | **unchanged** (`0`) | a tighter default would break a slow proxy that works today |
| total deadline | none, in most SDKs | the client's **`requestTimeout`** | not a number this document picks: merged policy already defines `requestTimeout` as the overall execution duration of a `Query` including retries and backoff, and a mirror REST query is a `Query` |

The other two — `maxAttempts` and `perAttemptTimeout` — **do** move, and they move differently at every call site,
because today every call site decides for itself. Current state, read from source in the three SDKs measured for
this document:

| Call site | Go | Java | JS |
|-----------|----|------|----|
| `FeeEstimateQuery` | **10** attempts (the *gRPC* constant `maxAttempts`), **no** per-attempt bound | 10, `requestTimeout` (2 min) **per request**; the argument's meaning is undocumented | 5 hardcoded, **no bound and no abort signal** — can hang on the OS TCP timeout |
| `MirrorNodeAccountBalanceQuery` | `Client.maxAttempts` if set, else 3; 30 s | 10, 2 min **per request**, documented as such | `client.maxAttempts` (10; **1000** on local networks), `requestTimeout` as a *total*, documented as such |
| `RegisteredNodeAddressBookQuery` | `Client.maxAttempts` if set, else **1 — no retry at all**, since `GetMaxAttempts()` returns `-1` when unset; fixed retry delay, no per-request bound; paginated | 10, 2 min per request **per page** | `maxAttempts + 1`, `requestTimeout` per request **per page**, undocumented |
| `MirrorNodeContractCallQuery` / `…EstimateGasQuery` | 3 hardcoded, 30 s | none (no retry), 30 s hardcoded, **a new HTTP client per call** | **none** — one POST, no retry, no bound |
| `AccountId` / `ContractId` populate helpers | **none** — bare `http.Get`, no retry, no bound | none, 30 s hardcoded | none, no bound |

So "a tightening for Java, a loosening for JS, a no-op for Go" is true only of one Go row. Four of Go's six call
sites move, two of them from *unbounded*. The pair belongs in a release note with this table.

Two further bounds are **new** rather than moved — nothing in any SDK bounds them today:

| Bound | Before | After | Why |
|-------|--------|-------|-----|
| redirect hops *(Full profile, SDK-built transport only)* | Java follows **none** — `HttpClient` defaults to a redirect policy of never; Go follows **10**; the browser's own cap applies and is not observable | **5**, with `Authorization` and other caller headers dropped on a cross-origin hop | No SDK agrees today. Java starts following redirects it ignores now, which is a new class of request leaving the process; Go tightens. On the constrained profile the platform's own bound still applies, and an injected transport sets its own. |
| response body | unbounded in every SDK | **32 MiB**, both profiles | A mirror node can otherwise make the SDK allocate without limit. This is the one new bound that introduces an error a caller has never seen — `response-too-large-error`, non-retryable. The profiles differ in *how* the cap is enforced, not in whether it holds. |

Five further behavior changes are intended and belong in the same note:

1. **A total bound now applies to mirror REST.** Where an SDK previously let a retry run go unbounded — Go's fee
   estimate and populate helpers pass no timeout at all, and four of the JavaScript call sites set none — the run is
   now bounded by `requestTimeout`. This is the change most likely to turn a slow success into a visible failure, and
   the one worth calling out first.
2. **HTTP retry stops borrowing the gRPC retry budget.** Three Go call sites read gRPC state today:
   `MirrorNodeAccountBalanceQuery` and `RegisteredNodeAddressBookQuery` consult `Client.getMaxAttempts()`, and
   `FeeEstimateQuery` initialises from the gRPC `maxAttempts` constant. After this proposal all three read
   `MirrorNodeHttpRetryPolicy` instead, so **`Client.setMaxAttempts` no longer changes mirror REST behaviour**. That
   is the intended separation rather than a regression — a knob documented for consensus-node retry silently
   governing HTTP reads is the drift this document exists to remove — but it is a visible change for anyone who set
   it expecting both.
3. **Call sites that never retried now do.** JavaScript's two contract queries and three populate helpers, Go's two
   populate helpers, and Go's `RegisteredNodeAddressBookQuery` — which resolves to a single attempt unless a caller
   set one of the two `maxAttempts` — gain the full retry policy, including retried `POST`s to
   `/api/v1/contracts/call`.
4. **`408` becomes retryable and `5xx` narrows to the pinned list.** Some SDKs treat every 4xx as terminal; others
   already retry `408`. `501`, `505`, `506`, `507`, `508`, `510` and `511` stop being retried.
5. **`Client.close()` can now block** for up to the grace period while in-flight mirror reads drain, in runtimes that
   can block. It could not before, because nothing owned the HTTP client.

Two further changes are observable only in timing: terminal transport failures now fail after one attempt instead of
consuming the budget, and a `Retry-After` shorter than the computed backoff shortens the wait while a longer one
lengthens it — the header is obeyed either way, up to the remaining total deadline.

> **Two consequences to check against your own code before adopting.** Any call site that retried on a fixed delay now
> backs off exponentially with jitter, so its worst-case wall clock grows even though its attempt count is unchanged.
> And a hand-built transport loses the ambient proxy configuration its language's default client had — see
> [Ownership and lifetime](#ownership-and-lifetime).

### Response Codes

No new consensus node response codes. Mirror REST is HTTP, and the mapping is the retry table above. Two SDK-level
rules apply:

- The error surfaced to the caller keeps each query's existing error type. The transport error, the exhausted
  retryable status or the expired total deadline is wrapped into it at the `MirrorNodeHttpClient` boundary.
- The message should prefer the mirror node's own envelope — `_status.messages[].detail`, then
  `_status.messages[].message`, then a truncated raw body — rather than dumping the body.

> `GET /api/v1/balances` returning an empty `balances` array is `INVALID_ACCOUNT_ID` at HTTP 200, not a zero balance.
> It is noted here only because it is the one case where a `2xx` is an error. The merged
> [account-balance migration proposal](https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/proposals/account-balance-query-mirror-node-migration.md)
> still describes the opposite, while every SDK implements `INVALID_ACCOUNT_ID`; reconciling that document and its
> test expectation is a separate change and is not in scope here. This proposal describes the implemented behaviour.

#### Transaction Retry

No consensus node transaction is involved. Nothing in this proposal changes gRPC retry, node rotation or
precheck-code handling. The HTTP retry policy above is entirely separate from, and must not reuse the knobs of, the
gRPC retry policy.

## Test Plan

**Transport contract**

1. Given a mirror endpoint returning `404`, when a request is made, then the transport returns an `HttpResponse` with
   `statusCode` `404` and does not raise.
2. Given a request is sent, when it reaches the wire, then it carries `x-user-agent: hiero-sdk-<language>/<version>`
   and the endpoint's `Content-Type`, and carries no `User-Agent` set by the SDK.
3. Given a mirror REST call and a consensus gRPC call from the same `Client`, when both are made, then both carry the
   same `x-user-agent` value.
4. Given a host name that does not resolve, when a request is made, then it fails with `unknown-host-error` and is not
   retried. *(Full profile only; on the constrained profile it fails with `connection-error` and is retried.)*
5. Given a server presenting an untrusted certificate, when a request is made, then it fails with `tls-error` and is
   not retried. *(Full profile only; on the constrained profile it fails with `connection-error` and is retried.)*
6. Given a closed port, when a request is made, then it fails with `connection-error` and is retryable.
7. Given a `connectTimeout` of 100 ms and an endpoint that never completes the handshake, when a request is made,
   then it fails near 100 ms rather than at the read deadline. *(Not applicable to the browser and React Native
   targets, which cannot express a connect bound; see [Conformance profiles](#conformance-profiles).)*
8. Given an endpoint that redirects indefinitely, when a request is made, then it stops after 5 hops. *(Full profile
   only; on the constrained profile it stops at the platform bound.)*
9. Given a request carrying `Authorization` and a redirect to a different origin, when the redirect is followed, then
   the second request carries no `Authorization`.
10. Given a closed transport, when a request is made, then it fails with `client-closed-error` and no connection is
    opened.
11. Given a transport already closed, when `close` is called again, then it is a no-op and does not raise.
12. Given an in-flight request and a close timeout longer than it, when `close` is called, then the request completes
    and the transport is released afterwards. *(Full profile only; the constrained profile has no pool to drain and
    aborts immediately.)*
13. Given an in-flight request and a close timeout shorter than it, when `close` is called, then it returns by the
    timeout and the request is aborted.
14. Given a caller that cancels mid-request, when the cancellation fires, then the call fails with `cancelled-error`
    immediately and is not retried.
15. Given N concurrent requests on one transport, when they run, then each receives its own response body and no
    shared state is corrupted.
16. Given a `deadline` of 1 s and an endpoint that sends headers immediately then drips the body for 30 s, when the
    request is made, then it fails with `timeout-error` near 1 s and the underlying connection is released — not
    merely abandoned by the caller.
17. Given a response whose header was written `Retry-After: 7`, when the headers are read, then the value is found
    under the key `retry-after`, over both HTTP/1.1 and HTTP/2.
18. Given a response carrying a header twice, when the headers are read, then both values are present as separate
    entries. *(Full profile only; a browser `Headers` object joins repeated values into one comma-separated string,
    so the constrained profile asserts only that no value is lost.)*

**Retry and timeout policy**

19. Given an endpoint returning `503` then `200`, when the call is made, then it retries once and succeeds.
20. Given an endpoint returning `400`, when the call is made, then it is not retried and the body reaches the caller.
21. Given a transport raising a failure outside the seven identifiers, when the call is made, then it is not retried.
22. Given an endpoint returning `408`, when the call is made, then it is retried.
23. Given an endpoint returning `501`, when the call is made, then it is not retried.
24. Given an endpoint returning `503` on every attempt, when the budget is exhausted, then the caller receives
    `retries-exhausted-error` **and** the last response.
25. Given a `429` carrying `Retry-After: 2`, when the call is retried, then it waits about two seconds rather than the
    computed backoff. *(Full profile only; on the constrained profile the header is unreadable and computed backoff
    applies.)*
26. Given a `429` carrying `Retry-After: 3600` and a remaining total deadline below it, when the response is handled,
    then the call fails immediately with `deadline-exceeded-error` and no further request is sent. *(Full profile
    only; a browser cannot read `Retry-After` cross-origin — see [Conformance profiles](#conformance-profiles).)*
27. Given a fixed retry index, when backoff is computed repeatedly, then the values vary and never exceed
    `min(maxBackoff, initialBackoff × 2^n)`.
28. Given `perAttemptTimeout` of 1 s, a stalling mirror node and `maxAttempts` above 1, when the call runs, then each
    attempt aborts near 1 s and total wall clock is about `maxAttempts * (1 s + backoff)`.
29. Given `totalDeadline` set below what the attempt budget would take, when the call runs, then it stops at the
    deadline with attempts remaining and fails with `deadline-exceeded-error`, distinguishably from the
    `retries-exhausted-error` of test 24.
30. Given a caller cancellation during a backoff sleep, when it fires, then the sleep is interrupted and no further
    attempt is made.
31. Given a `POST` with a body and an endpoint returning `503` then `200`, when it is retried, then the body is
    replayed byte-for-byte.
32. Given `maxAttempts` of 0, when a call is made, then it is rejected as invalid rather than silently doing nothing.

**Paths and pagination**

33. Given the string `https://evil.example/api/v1/accounts`, when a `MirrorNodeRestPath` is constructed, then
    construction fails with `invalid-path-error`.
34. Given the string `//evil.example/steal`, when a `MirrorNodeRestPath` is constructed, then construction fails with
    `invalid-path-error`.
35. Given base URLs `https://mirror/api/v1` and `https://mirror/api/v1/`, when the same path is resolved against each,
    then both produce the same absolute URL, and neither reference resolution nor host substitution can occur.
36. Given a `links.next` of `/api/v1/network/nodes?limit=100&order=asc`, when it is converted, then the `/api/v1`
    prefix is stripped exactly once.
37. Given a `links.next` naming another host, when it is converted, then conversion fails and the page is not fetched.
38. Given a paginated endpoint whose second page returns `503` then `200`, when the walk runs, then pagination
    inherits the same retry policy as the first page, every page is fetched from the same base URL, and the retries
    are drawn from the call's single remaining `totalDeadline`.
38a. Given a walk of more pages than `maxAttempts` in which every page succeeds first time, when it runs, then it
    completes — the attempt budget is per request, not per call.

**Client integration**

39. Given a `Client` that has made no mirror REST call, when it is inspected, then no transport has been built.
40. Given two different query types on one `Client`, when both execute, then they share one transport and one
    connection pool.
41. Given an injected transport, when a query executes, then the injected transport is used and the default is never
    constructed.
42. Given a policy set on the `Client` and a query on which `setMaxAttempts` was called, when that query executes,
    then the query's attempt count is used and every other field comes from the client's policy — query setter beats
    client beats package default, field by field.
43. Given an injected transport, when `Client.close()` is called, then the injected transport is **not** closed and
    remains usable by the application.
44. Given a `Client` configured with three mirror node base URLs, when three calls are made, then each targets a
    different base URL in round-robin order, and every attempt within one call targets the same base.
45. Given a `Client` with an open transport, when `close()` is called, then the transport is released and later calls
    fail with `client-closed-error`.
46. Given a `Client` that never used mirror REST, when `close()` is called, then it is a no-op and does not raise.
47. Given a `Client` on which no connect timeout was set, when a request is made, then the platform's own connect
    bound applies unchanged.
48. Given a `Client` on which no total deadline was set, when a long retry run happens, then the run is bounded by the
    client's `requestTimeout`.
49. Given `addMirrorNodeRequestHeader("Authorization", "Bearer test")`, when any mirror request is made, then the
    header is on the wire together with the SDK `x-user-agent`.
50. Given `addMirrorNodeRequestHeader("User-Agent", …)` or `addMirrorNodeRequestHeader("X-User-Agent", …)`, when the
    call is made, then it is rejected case-insensitively as a reserved key.
51. Given a caller header `Content-Type` and an endpoint that sets its own, when the request is made, then the
    endpoint's value wins.
52. Given a mirror error body carrying `_status.messages[].detail`, when the error surfaces, then the message contains
    that detail rather than the raw body.
53. Given `java.net.http` being absent, when a mirror query class is loaded on Android **from a minified release
    build**, then no `NoClassDefFoundError` is raised and no `java.net.http` type is resolved unless the default
    transport is constructed. *(Java only.)*

**Body limits and environment**

54. Given a response body larger than `maxResponseBytes`, when it is read, then the call fails with
    `response-too-large-error` and no truncated body reaches the caller. *(On the full profile the failure occurs one
    byte past the cap without buffering the remainder; on the constrained profile the body may be buffered first.)*
55. Given the ambient proxy configuration the language's own default client honours — `HTTP_PROXY` in Go, the
    `http.proxyHost` system properties in Java, whatever has been opted into in Node — when a mirror request is made
    through the default transport, then it is routed exactly as that default client would have routed it. The test is
    per language by necessity: no two of the three read the same source.

### TCK

The tests defined above should also be defined in the [TCK repository](https://github.com/hiero-ledger/hiero-sdk-tck).
The TCK drives SDKs through their public API against a network; it cannot install a fake transport, close a socket
mid-response, or count connection-pool instances. That splits this plan in two:

- **TCK-suitable** — observable through the public API against a controllable HTTP endpoint: 2, 3, 8, 9, 17, 19,
  20, 22, 23, 24, 25, 26, 28, 29, 31, 36, 37, 38, 38a, 44, 49, 51, 52, 54.
- **SDK-local unit tests** — require transport injection, socket control, raw header access, or pool inspection: 1,
  4, 5, 6, 7, 10, 11, 12, 13, 14, 15, 16, 18, 21, 27, 30, 32, 33, 34, 35, 39, 40, 41, 42, 43, 45, 46, 47, 48, 50, 55.
- **Language-specific:** 53 (Java/Android).
- **Profile-bound**, each with its constrained-profile form stated inline above: 8 and 9 (redirect bound), 54
  (streaming body cap), 4 and 5 (error granularity), 7 (connect bound — browser and React Native only), 12 (drain),
  18 (repeated headers), and 25 and 26 (`Retry-After` readability — browser only).

> Two of these moved out of the TCK bucket on inspection. Test 21 injects a transport that raises an unrecognised
> failure, which no public API can do, and test 18 needs the raw repeated header values, which a browser `fetch` does
> not preserve and a query's public surface never exposes.

**A prerequisite the TCK does not have today.** Every item in the TCK-suitable bucket needs an HTTP endpoint whose
status codes, headers, redirects and timing the test controls. The TCK has no controllable mock mirror REST endpoint.
Building one is a piece of TCK work in its own right, and it gates every other item here — so it is the first thing to
agree, before any per-area issues are worth filing.

Issues should be created in the TCK repository per group of related items rather than per item, and linked as
reference in this design doc.

Because most of the contract lands in the second bucket, the practical cross-SDK conformance levers are the shared
**error identifiers** and the pinned **retryable status list** — both assertable from a unit test in any language, and
neither of which exists today.

## SDK Example

### Example 1: existing callers are unaffected

Nothing changes for code that already uses a mirror REST query. This is the compatibility constraint in practice.

```javascript
const client = Client.forMainnet();

const balance = await new MirrorNodeAccountBalanceQuery()
    .setAccountId("0.0.12345")
    .execute(client);

await client.close(); // now also closes the HTTP transport it built
```

### Example 2: route mirror traffic through a corporate proxy

1. Build the language-native HTTP client with the proxy, trust store and interceptors the environment requires.
2. Wrap it in an `HttpTransport` implementation.
3. Install it on the `Client` before the first mirror REST call.
4. Execute any mirror query — it uses the injected transport, its retry policy and timeouts unchanged.

```java
client.setMirrorNodeHttpTransport(new OkHttpMirrorTransport(
    new OkHttpClient.Builder()
        .proxy(corporateProxy)
        .sslSocketFactory(sslSocketFactory, trustManager)
        .addInterceptor(tracingInterceptor)
        .build()));
```

> The `OkHttpClient` here is the application's, and stays the application's: `client.close()` will not shut it down.
> Closing it is the application's call, after every Hiero `Client` using it is done.

### Example 3: tighten the bounds for a latency-sensitive service

```java
client.setMirrorNodeHttpConnectTimeout(Duration.ofSeconds(2));
client.setMirrorNodeHttpRetryPolicy(MirrorNodeHttpRetryPolicy.defaults()
    .withMaxAttempts(5)
    .withPerAttemptTimeout(Duration.ofSeconds(3))
    .withTotalDeadline(Duration.ofSeconds(15)));

client.addMirrorNodeRequestHeader("Authorization", "Bearer " + token);
```

The same lines in Go and TypeScript, to show the derivation carries:

```go
client.SetMirrorNodeHttpConnectTimeout(2 * time.Second)
client.SetMirrorNodeHttpRetryPolicy(hiero.DefaultMirrorNodeHttpRetryPolicy().
    WithMaxAttempts(5).
    WithPerAttemptTimeout(3 * time.Second).
    WithTotalDeadline(15 * time.Second))
```

```typescript
client.setMirrorNodeHttpConnectTimeout(2_000); // milliseconds, per the TS binding of `duration`
client.setMirrorNodeHttpRetryPolicy(
    MirrorNodeHttpRetryPolicy.defaults()
        .withMaxAttempts(5)
        .withPerAttemptTimeout(3_000)
        .withTotalDeadline(15_000),
);
```

> Starting from `defaults()` rather than a literal is what keeps the five fields the caller did not name at their
> declared defaults. A struct or object literal naming only `maxAttempts` would leave the rest at their zero values in
> at least one language, producing a policy that retries nothing.

> Method and accessor naming should follow each language's best-practice guideline. Go exposes
> `SetMirrorNodeHttpConnectTimeout`; Rust uses snake_case; TypeScript/JavaScript use
> `setMirrorNodeHttpConnectTimeout`.
