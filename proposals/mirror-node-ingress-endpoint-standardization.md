# Mirror Node Ingress Endpoint Standardization for Local & CI Environments

**Date Submitted:** 2026-05-08

## Summary

This design proposal eliminates **in-SDK rewrites of the Mirror Node URL port** for queries that target services
hosted on different Mirror Node ports in local development and Continuous Integration (CI) environments. It does not
introduce a new endpoint, a new transport, or any new public configuration: the SDK already accepts a single
Mirror Node URL from the user, and that single URL is what should be used everywhere — without internal port
rewriting.

The Mirror Node team is migrating endpoints from the legacy JS REST API service to the new Java API service. As a
result, individual Mirror Node operations live behind different internal ports, and the SDKs have grown ad-hoc
port-rewrite logic to reach the right service. The JavaScript SDK is the most visible offender. For example, when
the configured `mirrorNodeUrl` points at `localhost`, the SDK currently does things like:

```js
// Current behavior in some SDK paths:
if (mirrorNodeUrl.includes("localhost") || mirrorNodeUrl.includes("127.0.0.1")) {
    mirrorNodeUrl = mirrorNodeUrl.replace("5551", "8384");
}
```

This pattern is fragile. It hardcodes Mirror Node's internal service topology into the SDKs; every time Mirror Node
consolidates, renames, or moves a service port, every SDK has to chase the change. The rewrite only fires for
`localhost` / `127.0.0.1`, which means production-like setups using a real ingress already work without rewriting —
confirming that rewriting is purely a workaround for the local/CI case.

[Solo](https://github.com/hiero-ledger/hiero-solo-action) exposes Mirror Node behind a **single ingress endpoint**
that fronts every Mirror Node service on a single port. The SDKs will treat the user-supplied Mirror Node URL as
authoritative, remove all in-SDK port-rewrite logic, and update the `MirrorNetwork.LOCAL_NODE` constants (and
language equivalents) to point at the Solo ingress port. CI does not need separate work: each SDK's CI pipeline
spins up the same local network the developer runs locally, so fixing the SDK source code fixes both.

**Once this proposal is implemented, every Solo network used for SDK development must be started with the ingress
controller enabled.** This applies in both directions:

- **Local development** — when running Solo on a developer machine, the network must be started with the ingress
  controller enabled.
- **CI** — when invoking [`hiero-solo-action`](https://github.com/hiero-ledger/hiero-solo-action), the workflow must
  enable the ingress controller.

Without the ingress controller enabled, the Mirror Node services that the SDK now reaches via the ingress port will
not be exposed and integration tests will fail.

Related references:

- Issue: [hiero-sdk-js#3590](https://github.com/hiero-ledger/hiero-sdk-js/issues/3590)
- Solo ingress: [hiero-solo-action#120](https://github.com/hiero-ledger/hiero-solo-action/issues/120)
- Mirror Node migration: [hiero-mirror-node#1699](https://github.com/hiero-ledger/hiero-mirror-node/issues/1699)

---

## New APIs

This proposal does not introduce any new public APIs.

---

## Updated APIs

This proposal does not modify any existing public APIs. Public configuration entry points such as
`Client.setMirrorNetwork(...)`, `Client.setMirrorNodeUrl(...)`, and language equivalents keep their current
signatures and semantics.

---

## Internal Changes

The change is entirely internal to each SDK.

Every code path in every SDK that conditionally rewrites the Mirror Node URL port — typically guarded by a
`localhost` / `127.0.0.1` check — must be deleted. The SDK must use the configured Mirror Node URL as-is for the
operation it is performing. Example of code to remove (JavaScript SDK, illustrative):

```js
// REMOVE: ad-hoc port rewriting that hardcodes Mirror Node service topology.
if (mirrorNodeUrl.includes("localhost") || mirrorNodeUrl.includes("127.0.0.1")) {
    mirrorNodeUrl = mirrorNodeUrl.replace("5551", "8384");
}
```

Each SDK owner is responsible for auditing their codebase for equivalent rewrites — including any path that swaps
between gRPC and REST ports for the same Mirror Node URL — and removing them. After this change, the configured URL
is the single source of truth for every Mirror Node service the SDK calls.

In parallel, each SDK's local-environment preset — for example `MirrorNetwork.LOCAL_NODE` and
`WebMirrorNetwork.LOCAL_NODE` in the JavaScript SDK, and the equivalent constants in the Java, Go, Rust, Python, C++,
and Swift SDKs — must be updated to point at the Solo ingress endpoint and port. The previous, service-specific port
values are removed from the SDK source: no fallback is needed because the ingress fronts every Mirror Node service.

After this work lands, both local development and CI must run Solo with the ingress controller enabled (see
[Summary](#summary)). This is a hard prerequisite, not a recommendation: SDK integration tests will fail against any
local network that does not expose the Mirror Node services through the ingress.

### Response Codes

Not applicable. This proposal does not interact with consensus node response codes.

#### Transaction Retry

Not applicable. The change does not alter retry semantics; it only changes the URL the SDK already uses for Mirror
Node operations.

---

## Test Plan

No new tests are required for this proposal. Each SDK's existing unit and integration test suites already cover the
Mirror Node-backed operations affected by this change. The acceptance criterion is that, after the in-SDK
port-rewrite logic is removed and `MirrorNetwork.LOCAL_NODE` is updated, every SDK's existing test suite continues to
pass with **no regressions** when run against a Solo local network started with the ingress controller enabled —
both locally and in CI.

### TCK

The TCK [hiero-sdk-tck](https://github.com/hiero-ledger/hiero-sdk-tck) does not exercise the in-SDK port-rewrite paths
directly, but its SDK JSON-RPC server reference implementations are themselves SDK consumers and will inherit the
fix. The same acceptance criterion applies: after each SDK's reference implementation is updated, the TCK conformance
suite must continue to pass with no regressions when run against a Solo local network started with the ingress
controller enabled. Issues should be opened against the TCK repository to update `.env.custom_node` examples to use
the Solo ingress endpoint.

## SDK Example

This proposal introduces no new user-facing flows. All existing SDK examples should continue to work seamlessly after
this change, provided the local Solo network is started with the ingress controller enabled (see
[Summary](#summary)).
