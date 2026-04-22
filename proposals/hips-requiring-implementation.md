# HIPS REQUIRING IMPLEMENTATION

> [!IMPORTANT]
> This document tracks the implementation progress of Hedera Improvement Proposals (HIPs) across the Hiero SDKs. 
> For HIPs that **do not** require code changes (documentation only, ecosystem awareness, etc.), please see [HIPS --NOT-- REQUIRING IMPLEMENTATION](./hips-not-requiring-implementation.md).

---

## Scope and Purpose

This document serves as the primary coordination artifact for HIPs that necessitate direct code changes, new API surfaces, or logic modifications within the Hiero SDKs (Java, Go, JS, etc.).

### What Qualifies as an "Implementation HIP"?
- **New Features:** Addition of new network services or transaction types.
- **API Modifications:** Breaking changes or enhancements to existing SDK interfaces.
- **Logic Updates:** Changes to internal SDK behavior (e.g., transaction fee calculation, retry logic).
- **Protocol Integration:** Implementing new network standards that require SDK-side logic.

### Why Tracking is Important
Unlike informational HIPs, implementation-heavy HIPs require significant effort across multiple repositories. This tracker ensures:
- **Cross-Language Parity:** Verifying that all SDKs implement the HIP consistently.
- **Version Coordination:** Aligning release schedules for feature parity.
- **Resource Allocation:** Identifying which SDKs are lagging or need maintainer attention.

---

## Implementation Tracking Table

| HIP ID | Title | Summary | Impacted SDKs | Status | PR / Issues |
|--------|-------|---------|---------------|--------|-------------|
| HIP-719 | Associate Tokens with Accounts | Implement `TokenAssociateTransaction` and related logic. | All | Final | [Java #123](https://github.com/hiero-ledger/hiero-sdk-java/pull/123), [JS #456](https://github.com/hiero-ledger/hiero-sdk-js/pull/456) |
| HIP-1137 | Account Restriction Support | Add support for `AccountRestrictionStatus` in account queries and updates. | Java, Go | In Progress | [Go #789](https://github.com/hiero-ledger/hiero-sdk-go/pull/789) |
| HIP-1195 | Network Node Identifiers | Update SDK logic to support the new node identifier format in gRPC calls. | All | Review | [Issue #221](https://github.com/hiero-ledger/sdk-collaboration-hub/issues/221) |

*(Note: Status = Not Started / In Progress / Review / Final / Adopted)*

---

## How to Add a New HIP to This Tracker

When a new HIP is accepted that requires SDK implementation, please add it here:

### Required Fields
- **HIP ID:** The official HIP number.
- **Title:** The HIP title.
- **Summary:** Concise description of the implementation requirements.
- **Impacted SDKs:** List of SDKs that must implement this (usually "All").
- **Status:** Current implementation state.
- **PR / Issues:** Links to active development PRs or language-specific issues.

---

## Maintenance Guidelines

- **Who updates status:** SDK maintainers or lead developers for each language.
- **When to update:** At the start of implementation, during review, and upon final merge into the `main` branch of the respective SDK.
- **Integration:** Statuses here should be synced with the main Hiero roadmap.
