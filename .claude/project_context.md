# Hiero SDK Collaboration Hub - Project Context

## Project Overview

This is the **Hiero SDK Collaboration Hub** - a central repository for collaboration on the SDKs of the [Hiero Organization](https://github.com/hiero-ledger).
This repository does not serve as an SDK implementation, but as a collaboration and design workspace for:

- Design proposals for SDK features
- Guidelines and best practices
- Onboarding resources for contributors
- V3 SDK prototypes and experiments

## Repository Structure

```
sdk-collaboration-hub/
├── guides/              # Contribution guidelines and best practices
├── proposals/           # SDK design proposals (active and archived)
├── templates/           # Reusable templates for proposals, issues, PRs
├── v3-sandbox/          # Experimental workspace for SDK V3
│   ├── prototype/       # Java PoC (historical, not actively maintained)
│   ├── prototype-api/   # Language-agnostic API definitions for V3
│   └── prototype-keys-java/  # Current Java prototype for key handling
└── .claude/             # Claude Code context and configuration
```

## Key Guidelines

### API Design (language-agnostic)

- **Guidelines**: See `guides/api-guideline.md`
- **Syntax**: We use our own meta-language for language-agnostic API definitions
- All proposals (see `proposals` and `v3-sandbox/prototype-api` folders) should document APIs in this format

### Java Implementation Guidelines

- **Guidelines**: See `guides/api-best-practices-java.md`

## SDK V3 Sandbox

The `v3-sandbox/` folder is the experimental workspace for a possible V3 of the Hiero SDKs:

### prototype-api/
- Language-agnostic API definitions (in `.md` files)
- Documents ideal API without backward compatibility concerns
- Modules: client, keys, transactions, grpc, config, etc.

### prototype-keys-java/
- **Current Java prototype** for cryptographic key handling
- Package: `org.hiero.keys`
- Main classes:
  - `Key` - Base interface for all keys
  - `PublicKey` - Public key with `verify()` method
  - `PrivateKey` - Private key with `sign()` method
  - `KeyAlgorithm` - Enum for supported algorithms (ED25519, ECDSA_SECP256K1, RSA)
  - `KeyFactory` - Factory for key instances
- Dependencies: BouncyCastle (`bcpkix-jdk18on:1.76`)
- Testing: JUnit 5.12.2

## Git Workflow

- **Main Branch**: `main`
- PRs against `main`

## Community

- **SDK Community Call**: Every Monday (see [LFX Calendar](https://www.lfdecentralizedtrust.org/meeting-calendar))
- **Meeting Minutes**: [Hiero Governance Wiki](https://github.com/hiero-ledger/governance/wiki)
- **Contribution Guide**: https://github.com/hiero-ledger/.github/blob/main/CONTRIBUTING.md

## Important Links

- Hiero Organization: https://github.com/hiero-ledger
- Code of Conduct: https://www.lfdecentralizedtrust.org/code-of-conduct
- License: Apache 2.0

## Maintainers

See `MAINTAINERS.md` in the repository root.

---

**Note for Claude Code**: This repository is primarily for design and collaboration.
Code changes should be experimental.
Always follow the guidelines in `guides/` for API changes.
