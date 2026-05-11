# Mirror Node Ingress Endpoint Standardization for Local & CI Environments

**Date Submitted:** 2026-05-08

## Summary

This design proposal eliminates **in-SDK rewrites of the Mirror Node URL port** for queries that target services
hosted on different Mirror Node ports in local development and Continuous Integration (CI) environments. It does not
introduce a new endpoint, a new transport, or any new public configuration: the SDK already accepts a single
Mirror Node URL from the user, and that single URL is what should be used everywhere — without internal port
rewriting.

The Mirror Node team is migrating endpoints from the legacy JS REST API service to the new Java API service. As a
result, individual Mirror Node operations live behind different internal ports, and some SDKs have grown ad-hoc
port-rewrite logic to reach the right service. These rewrites sit at multiple distinct call sites — not in a single
block — and each call site targets a different Mirror Node service. For example, requests served by the Java API
(such as fee estimation and registered-node address book queries) rewrite the configured port `5551` to `8084`,
while Mirror Node Web3 contract queries rewrite `5551` to `8545`. The set of Java-API-bound rewrites is actively
growing as the Mirror Node team migrates more endpoints away from the legacy JS REST API service.

This pattern is fragile. It hardcodes Mirror Node's internal service topology into the SDKs; every time Mirror Node
consolidates, renames, or moves a service port, every SDK has to chase the change. The rewrites only fire for
`localhost` / `127.0.0.1`, which means production-like setups using a real ingress already work without rewriting —
confirming that rewriting is purely a workaround for the local/CI case.

[Solo](https://github.com/hiero-ledger/hiero-solo-action) exposes Mirror Node behind a **single ingress endpoint**
that fronts every Mirror Node service on a single port. The SDKs will treat the user-supplied Mirror Node URL as
authoritative, remove all in-SDK port-rewrite logic, and update each SDK's local mirror network preset to point at
the Solo ingress endpoint. CI does not need separate work: each SDK's CI pipeline spins up the same local network
the developer runs locally, so fixing the SDK source code fixes both.

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

The change is entirely internal to each SDK and is captured by a single end-state rule:

> The user-supplied Mirror Node URL must be used as-is for every Mirror Node operation. No SDK code path may mutate
> the URL (host, port, scheme, or path prefix) based on whether the host is `localhost` / `127.0.0.1` or any other
> characteristic of the configured value.

Each SDK owner is responsible for auditing their codebase and removing anything that violates this rule. Common
violating shapes include `localhost`-guarded port replacement, per-network port-translation maps, and
transport-specific overrides; some SDKs may already comply and need no change. In the SDKs that do violate the rule
today, the violations span multiple distinct call sites — for example, fee estimation and registered-node address
book queries rewrite to the Java REST API port `8084`, while Mirror Node contract queries rewrite to the Web3 port
`8545` — and the set of `8084`-bound rewrites continues to grow as the Mirror Node team migrates more endpoints from
the legacy JS REST API service.

In parallel, each SDK's local-environment preset — for example `MirrorNetwork.LOCAL_NODE` and
`WebMirrorNetwork.LOCAL_NODE` in the JavaScript SDK, and the equivalent constants in the Java, Go, Rust, Python, C++,
and Swift SDKs — must be updated to point at the Solo ingress endpoint and port. The previous, service-specific port
values are removed from the SDK source: no fallback is needed because the ingress fronts every Mirror Node service.

After this work lands, both local development and CI must run Solo with the ingress controller enabled (see
[Summary](#summary)). This is a hard prerequisite, not a recommendation: SDK integration tests will fail against any
local network that does not expose the Mirror Node services through the ingress. The cleanup itself is **gated** on
[`hiero-solo-action#120`](https://github.com/hiero-ledger/hiero-solo-action/issues/120), which still has open design
questions (which local port the ingress is forwarded to, and whether the ingress controller is enabled by default or
behind a flag). As of the latest action release `v0.19.0`, the ingress controller is enabled only as a side effect
of `installRelay: true`; there is no first-class input for it. **No SDK can complete this cleanup until that PR
lands and a corresponding `hiero-solo-action` release ships.**

Once the gate lifts, each SDK can land the cleanup as a single atomic PR in its own repository, with three
coordinated changes: (1) bump or reconfigure `hiero-solo-action` to enable the ingress controller in CI; (2) remove
the in-SDK port-rewrite logic that violates the end-state rule above; and (3) update the SDK's local mirror network
preset to point at the Solo ingress endpoint. Because each SDK's CI uses the same Solo network developers run
locally, this keeps source and CI in sync without cross-repo coordination.

### Response Codes

Not applicable. This proposal does not interact with consensus node response codes.

#### Transaction Retry

Not applicable. The change does not alter retry semantics; it only changes the URL the SDK already uses for Mirror
Node operations.

---

## Test Plan

No new tests are required for this proposal. Each SDK's existing unit and integration test suites already cover the
Mirror Node-backed operations affected by this change. The acceptance criterion is that, after the in-SDK
port-rewrite logic is removed and the local mirror network preset is updated, every SDK's existing test suite
continues to pass with **no regressions** when run against a Solo local network started with the ingress controller
enabled — both locally and in CI.

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
