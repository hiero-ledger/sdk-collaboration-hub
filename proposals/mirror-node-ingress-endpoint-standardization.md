# Mirror Node Ingress and Windows-Compatible Local Defaults for Local & CI Environments

**Date Submitted:** 2026-05-08

## Summary

This design proposal has two coupled goals for local development and Continuous Integration (CI) environments:

1. Eliminate **in-SDK rewrites of the Mirror Node URL port** for queries that target services hosted on different
   Mirror Node ports. The SDK already accepts a single Mirror Node URL from the user, and that single URL must be
   used everywhere — without internal port rewriting.
2. Align each SDK's **local-environment port defaults** with the Windows-compatible local ports that the Solo team
   adopted in [hiero-ledger/solo#3618](https://github.com/hiero-ledger/solo/pull/3618), so SDK integration tests can
   run on Windows machines without per-port overrides.

The change introduces no new endpoint, no new transport, and no new public configuration.

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
the Solo ingress endpoint on **`127.0.0.1:38081`** — the local port that Solo now forwards the ingress to. CI does
not need separate work: each SDK's CI pipeline spins up the same local network the developer runs locally, so
fixing the SDK source code fixes both.

In parallel, solo#3618 moved several other default local port-forwards out of the Windows ephemeral port range
(`49152–65535`) and the default Kubernetes NodePort range (`30000–32767`) so that Solo can run on Windows runners.
The SDKs hold their own copies of these defaults in their local-environment presets and must be updated to match.
The full alignment table:

| Service                | Pod Port | Previous Local Port | New Local Port |
|------------------------|----------|---------------------|----------------|
| Consensus Node gRPC    | `50211`  | `50211`             | `35211`        |
| JSON-RPC Relay         | `7546`   | `7546`              | `37546`        |
| Mirror Node ingress    | `80`     | `8081`              | `38081`        |

SDKs only need to update the local-default constants for the services they actually expose to users. In practice
this means: every SDK updates its consensus-node default (`35211`) and its Mirror Node ingress default (`38081`);
SDKs that ship a JSON-RPC Relay local default additionally update it to `37546`.

The set above does **not** yet cover the per-node **gRPC Web proxy ports** that the SDKs use to reach the consensus
nodes from browser runtimes. `hiero-solo-action` currently exposes these as `grpcProxyPort` `8080` for node 1 and
`grpcProxyPort` `8081` for node 2. Both values collide with the previous local ports that solo#3618 moved away from
(Hiero Explorer was on `8080` and the Mirror Node ingress was on `8081`) and are inconsistent with the new
high-port (`3xxxx`) convention adopted for the rest of the alignment. They do not fall inside the Windows
ephemeral port range (`49152–65535`), so they are not strictly *broken* on Windows today, but the inconsistency
means a developer running Solo CLI and `hiero-solo-action` defaults side-by-side will see a confusing mix of port
conventions. **This proposal therefore leaves two questions open for the team to decide before the SDKs land their
cleanup PRs:** (1) do we move the per-node gRPC Web proxy ports into the `3xxxx` range to match the rest of the
Windows-compatible alignment (for example node 1 → `38211`, node 2 → `38212`, scaling for additional nodes), or
keep them at `8080` / `8081` since they technically still work on Windows; and (2) whichever values we choose, the
SDKs' JS-Web local defaults must be updated in lockstep with `hiero-solo-action`, so this proposal must record the
decision before any SDK PR is merged. This sub-decision is **not gating** on the rest of the proposal — the Mirror
Node ingress cleanup and the consensus-node / JSON-RPC Relay alignment above can land independently of the gRPC Web
proxy choice.

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
- Solo Windows-compatible local ports: [hiero-ledger/solo#3618](https://github.com/hiero-ledger/solo/pull/3618)
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

In parallel, each SDK's local-environment presets — for example `MirrorNetwork.LOCAL_NODE` and
`WebMirrorNetwork.LOCAL_NODE` in the JavaScript SDK, and the equivalent constants in the Java, Go, Rust, Python,
C++, and Swift SDKs — must be updated to match the Windows-compatible local ports adopted by Solo (see
[Summary](#summary) for the full table). Concretely, each SDK updates:

- its consensus-node default to `127.0.0.1:35211`;
- its Mirror Node ingress default to `127.0.0.1:38081`;
- its JSON-RPC Relay default (where applicable) to `127.0.0.1:37546`.

The previous, service-specific Mirror Node port values are removed from the SDK source: no fallback is needed
because the ingress fronts every Mirror Node service. The other previous local ports are simply replaced.

After this work lands, both local development and CI must run Solo with the ingress controller enabled (see
[Summary](#summary)). This is a hard prerequisite, not a recommendation: SDK integration tests will fail against any
local network that does not expose the Mirror Node services through the ingress. The cleanup itself is **gated** on
[`hiero-solo-action#120`](https://github.com/hiero-ledger/hiero-solo-action/issues/120) — the action must support an
opt-in flag for the ingress controller and pick up the new Solo defaults — and on a corresponding
`hiero-solo-action` release shipping. **No SDK can complete this cleanup until those changes ship.**

Once the gate lifts, each SDK can land the cleanup as a single atomic PR in its own repository, with three
coordinated changes: (1) bump and configure `hiero-solo-action` to enable the ingress controller in CI; (2) remove
the in-SDK port-rewrite logic that violates the end-state rule above; and (3) update the SDK's local-environment
presets to point at the new Windows-compatible local ports. Because each SDK's CI uses the same Solo network
developers run locally, this keeps source and CI in sync without cross-repo coordination.

### Response Codes

Not applicable. This proposal does not interact with consensus node response codes.

#### Transaction Retry

Not applicable. The change does not alter retry semantics; it only changes the URL the SDK already uses for Mirror
Node operations.

---

## Test Plan

No new tests are required for this proposal. Each SDK's existing unit and integration test suites already cover the
Mirror Node-backed operations and consensus-node operations affected by this change. The acceptance criterion is
that, after the in-SDK port-rewrite logic is removed and the local-environment presets are updated to the new
Windows-compatible ports, every SDK's existing test suite continues to pass with **no regressions** when run
against a Solo local network started with the ingress controller enabled — both locally (including on Windows) and
in CI.

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
