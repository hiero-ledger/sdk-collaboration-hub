# HIPs - Not Requiring Implementation

> [!IMPORTANT]
> This document is for tracking and coordination only.
> Any HIP requiring SDK code changes must be tracked in separate implementation issues (e.g., #216). Do not use this document for tracking implementation progress or PRs that modify SDK logic.

---

## Purpose and Scope

The purpose of this document is to track Hedera Improvement Proposals (HIPs) that do not require direct code changes or API modifications within the Hiero SDKs, but still impact the ecosystem, documentation, or how developers interact with the protocol.

### Included in Scope
- Documentation-impacting HIPs: Changes that require updates to guides, tutorials, or READMEs.
- Conceptual / Protocol-level changes: Changes to how the network operates that impact mental models but not the SDK code itself.
- Behavioral clarifications: Updates to specifications that clarify expected behavior without changing implementation logic.
- Ecosystem / Awareness: HIPs that define standards (like metadata formats) which SDK users and maintainers should be aware of.

### Excluded from Scope
- Any HIP requiring new methods, classes, or logic in SDKs.
- Performance improvements that require implementation work in the codebase.
- Feature additions needing SDK updates.

---

## HIP Tracking Table

| HIP ID | Title | Summary | Affected SDKs | Impact Type | Status | References |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| [HIP-1](https://hips.hedera.com/hip/hip-1) | Hedera Improvement Proposal | The standard process for proposing improvements to the Hedera network. | All | Documentation | Active | [Link](https://hips.hedera.com/hip/hip-1) |
| [HIP-15](https://hips.hedera.com/hip/hip-15) | HIP Governance | Defines the governance model for the HIP process. | All | Ecosystem | Active | [Link](https://hips.hedera.com/hip/hip-15) |
| [HIP-XXX] | [Example Placeholder] | [Example of a conceptual protocol update or behavioral clarification] | All | Behavioral | Draft | [Link] |

---

## Impact Type Definitions

- Documentation: Requires updates to technical documentation, guides, or examples.
- Behavioral: Impacts how developers use the SDK (e.g., timing, retries, or expected errors) without changing the API.
- Ecosystem: Relates to broader standards or protocol conventions (e.g., metadata schemas).
- Awareness: Primarily for informing SDK maintainers and users about protocol evolution.

---

## How to Contribute

We welcome updates to this tracking list. To add or update an entry:

1. Verify Scope: Ensure the HIP does not require SDK code changes.
2. Impact Analysis: Identify which SDKs are affected and the type of impact.
3. Submit a PR: Add the entry to the table above and link the relevant HIP.

---

## Related Tracking
- SDK Implementation Epic: #216 (For HIPs requiring code changes)
- Hiero Ledger HIPs Repo: [github.com/hashgraph/hedera-improvement-proposals](https://github.com/hashgraph/hedera-improvement-proposals)
