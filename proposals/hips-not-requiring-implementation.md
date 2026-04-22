# HIPS --NOT-- REQUIRING IMPLEMENTATION

> [!IMPORTANT]
> This document is for tracking and coordination only.
> Any HIP requiring SDK code changes must be tracked in separate implementation issues (e.g., #216). Do not use this document for tracking implementation progress or PRs that modify SDK logic.

---

## Scope and Purpose

This document serves as the central tracking artifact for Hedera Improvement Proposals (HIPs) that **do not require direct SDK code implementation**, yet necessitate cross-SDK visibility, coordination, and ecosystem awareness.

### What Qualifies as a "Non-Implementation HIP"?
- **Documentation Updates:** Changes that strictly affect API reference docs, tutorials, or conceptual guides without altering underlying code.
- **Behavioral Clarifications:** Standardizations of how SDKs should behave in edge cases (e.g., error handling conventions) where the implementation might already exist but needs to be uniformly understood.
- **Ecosystem / Awareness:** Network-wide standards, tool integrations, or architectural patterns that SDK developers and users must be aware of, even if the SDKs themselves don't require code changes.

### What Does NOT Qualify?
- Any HIP that requires writing new features, modifying existing logic, adding new API endpoints, or updating dependencies within the SDK codebase. These must be tracked in their own dedicated implementation epics.

### Why Tracking is Important
Even if no code is written, SDK maintainers must remain aligned. This tracker ensures that:
- Documentation accurately reflects the latest network standards.
- Maintainers enforce behavioral conventions uniformly across all languages (JS, Go, Java, Swift, etc.).
- Critical ecosystem changes do not catch SDK maintainers or end-users by surprise.

---

## Structured Tracking Table

| HIP ID | Title | Summary | Affected SDKs | Impact Type | Status | References |
|--------|-------|---------|---------------|-------------|--------|------------|
| HIP-15 | Hedera Account Identifiers | Clarifies string formats for account IDs to maintain consistency across ecosystem tooling. | All | Awareness | Final | [HIP-15](https://hips.hedera.com/hip/hip-15) |
| HIP-794 | Standardize Network Node Identifiers | Defines naming conventions for network nodes in documentation and reference materials. | All | Docs | Adopted | [HIP-794](https://hips.hedera.com/hip/hip-794) |
| HIP-820 | SDK Error Handling Terminology | Establishes a uniform vocabulary for describing network errors in conceptual documentation. | All | Behavior | Review | [HIP-820](https://hips.hedera.com/hip/hip-820) |

*(Note: Impact Type = Docs / Behavior / Ecosystem / Awareness \| Status = Draft / Review / Final / Adopted)*

---

## How to Contribute

When a new HIP is introduced that fits the non-implementation criteria, please add it to the table above following these guidelines:

### How to Determine if a HIP Belongs Here
Review the HIP's "Specification" and "Implementation" sections. If the HIP only mandates documentation updates, standardizes existing behavior, or provides informational context without necessitating code changes in the SDKs, it belongs here.

### Formatting Rules
Add a new row to the table using the following required fields:
- **HIP ID:** The official HIP number (e.g., HIP-123).
- **Title:** The exact title from the official HIP document.
- **Summary:** A 1-2 sentence concise explanation of the HIP's purpose.
- **Affected SDKs:** Comma-separated list (e.g., JS, Go, Java) or "All".
- **Impact Type:** Must be exactly one of: `Docs`, `Behavior`, `Ecosystem`, or `Awareness`.
- **Status:** Must be exactly one of: `Draft`, `Review`, `Final`, or `Adopted`.
- **References:** A markdown link to the official HIP URL.

---

## Maintenance Guidelines

- **Who updates status:** SDK maintainers, technical writers, or community contributors who monitor HIP progression.
- **When to update entries:** Statuses should be updated whenever the corresponding HIP changes state on the official Hedera HIPs repository.
- **Cross-SDK Tracking:** This issue acts as the source of truth for visibility. If a non-implementation HIP requires specific documentation tasks per SDK, individual issues may be created and linked in the "Summary" column, but the overarching status is tracked here. Entries are generally not removed, serving as a historical ledger of adopted standards.
