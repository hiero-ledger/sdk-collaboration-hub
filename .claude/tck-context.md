# Hiero SDK Technology Compatibility Kit (TCK)

## What is the TCK?

The [TCK](https://github.com/hiero-ledger/hiero-sdk-tck) is a set of tools and test suites used to verify whether an
SDK implementation conforms to the Hiero SDK specification. It validates consensus node operations, query functionality,
and correct behavior across all supported transaction types.

## Architecture

The TCK uses a **JSON-RPC server model**:

1. Each SDK implements a JSON-RPC server that exposes SDK operations as RPC methods.
2. The TCK test driver (written in TypeScript) sends JSON-RPC requests to the server at `http://localhost:8544/` by
   default.
3. The server translates each RPC call into the corresponding SDK operation (e.g., create an account, submit a
   transaction) and returns the result.

This architecture decouples the test suite from any specific SDK language — the same tests run against all SDK
implementations as long as they provide a conforming JSON-RPC server.

## Technology Stack

- **TypeScript** — Test driver and infrastructure.
- **Mocha** — Test execution framework, generating HTML and JSON reports in `mochawesome-report/`.
- **Docker** — Containerized test execution for isolated environments.
- **OpenAPI code generation** — TypeScript interfaces generated from `mirror-node.yaml` for type-safe Mirror Node API
  interaction.
- **Taskfile.yaml** — Task automation for common operations.

## Test Coverage

The TCK validates 40+ transaction and operation types including:

- **Account management** — Create, update, delete, allowance operations.
- **File operations** — Create, update, append, delete.
- **Token operations** — Create, mint, burn, transfer, freeze, pause, associate, dissociate, fee schedules, KYC.
- **Smart contracts** — Create, call, delete.
- **Topics / HCS** — Create, update, delete, submit messages.
- **Schedule transactions** — Create, sign, delete scheduled operations.
- **Node operations** — Node create, update, delete.

## Environment Configuration

Tests can target different networks via environment files:

- `.env.testnet` — Hedera testnet.
- `.env.custom_node` — Local or custom network (e.g., local node for development).

## How SDKs Integrate

To add TCK support for an SDK:

1. Implement a JSON-RPC server in the SDK's language that handles the TCK's defined RPC methods.
2. Each method maps to an SDK operation (e.g., `createAccount` maps to the SDK's account creation flow).
3. Run the TCK test suite against the server to verify conformance.

The TCK is the authoritative source for verifying that an SDK implementation behaves correctly and consistently with
other SDK implementations.
