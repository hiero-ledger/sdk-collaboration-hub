# Hiero Improvement Proposals (HIPs)

## What is a HIP?

A HIP (Hiero Improvement Proposal) is a formal design document that proposes improvements to the Hiero ecosystem. HIPs
serve as the primary mechanism for proposing new features, collecting community input, and documenting design decisions.
They are maintained in the [hiero-improvement-proposals](https://github.com/hiero-ledger/hiero-improvement-proposals)
repository under the Linux Foundation Decentralized Trust.

The HIP process is defined in [HIP-1](https://hips.hedera.com/hip/hip-1). Published HIPs can be browsed at
[hips.hedera.com](https://hips.hedera.com).

## HIP Types

- **Standards Track** — Proposes new features requiring a specification and reference implementation. Subcategories:
  - **Core** — Low-level protocol changes (consensus, networking).
  - **Service** — Changes to the service layer (HAPI, smart contracts, tokens, etc.).
  - **Mirror/Block Node** — Changes to data retrieval systems (mirror node APIs, block node).
  - **Application** — Standards for external software and applications built on Hiero.
- **Informational** — Provides guidelines or information without proposing a new standard. Does not necessarily
  represent community consensus.
- **Process** — Describes governance procedures or changes to existing processes. Applies outside the codebase itself.

## HIP Statuses and Workflow

Standards Track HIPs progress through these stages:

    Idea -> Draft -> Review -> Last Call -> Approved -> Final

Alternative terminal statuses: Stagnant, Deferred, Withdrawn, Rejected, Replaced.

Informational and Process HIPs follow a simpler path:

    Idea -> Draft -> Review -> Last Call -> Active

## HIP Document Structure

A HIP document includes a YAML preamble followed by these sections:

- **Abstract** — Short summary of the proposal.
- **Motivation** — Why the change is needed.
- **Rationale** — Design reasoning and trade-offs.
- **User Stories** — How users interact with the proposed feature.
- **Specification** — Technical details of the proposed change.
- **Backwards Compatibility** — Impact on existing functionality.
- **Security Implications** — Security considerations.
- **How to Teach This** — Guidance for documentation and education.
- **Reference Implementation** — Working code or prototype.
- **Rejected Ideas** — Alternatives considered and why they were rejected.
- **Open Issues** — Unresolved questions.
- **References** — Links to related resources.

HIPs use GitHub-flavored Markdown and are licensed under Apache 2.0.

## HIPs in This Repository

The `proposals/hips/` folder contains SDK design documents for specific HIPs. Each file is named `hip-NNNN.md` where
NNNN is the HIP number. These proposals define how the SDK should implement the features described in the corresponding
HIP, using the meta-language syntax from `guides/api-guideline.md`.

## Governance

The Hiero Technical Steering Committee (TSC) makes final decisions on Standards Track HIPs. HIP editors handle
administrative tasks like number assignment and pull request management. Discussion happens through GitHub issues and the
LFDT Discord server.
