# Standardized GitHub Labels for Hiero SDKs

This document defines a standardized set of GitHub labels to be implemented across all Hiero SDK repositories. Establishing this baseline ensures that contributors and maintainers experience a consistent workflow, regardless of the specific programming language or repository they are working in.

---

## GitHub Issue Types vs. Labels

GitHub provides built-in **issue types** that should be used as the primary classification:

| GitHub Issue Type | Description |
| :---------------- | :---------- |
| **Bug** | An unexpected problem or behavior |
| **Feature** | A request, idea, or new functionality |
| **Task** | A specific piece of work |
| **Epic** | A body of work that can be broken down into specific tasks |
| **Initiative** | A large group of work focused on high-level goals and strategy |

The labels defined in this document **complement** GitHub's issue types—they do not replace them. Use GitHub's built-in types for primary classification (bug vs. feature vs. task), and use these labels for additional context like priority, status, skill level, and work category.

---

## Label Categories

### 1. Work Kind Labels

These labels provide additional specificity about the **kind of work** involved in a task, feature, or bug. They complement GitHub's issue types by answering "what area does this touch?" rather than "what type of issue is this?"

| Label | Description | Color |
| :---- | :---------- | :---- |
| `kind: enhancement` | Improvement to existing code or performance without adding new features | `#3e82f7` |
| `kind: documentation` | Improvements or additions to READMEs, guides, API docs, or code comments | `#0075ca` |
| `kind: refactor` | Code changes that neither fix a bug nor add a feature (cleanup, restructuring) | `#d4c5f9` |
| `kind: security` | Security vulnerabilities or security-related improvements | `#e11d21` |
| `kind: testing` | Adding or improving unit tests, integration tests, or test infrastructure | `#c5def5` |
| `kind: maintenance` | Updates to CI/CD pipelines, build systems, dependencies, or tooling | `#6a737d` |

> **Example Usage:**
> - A GitHub **Bug** issue about a security vulnerability → add `kind: security`
> - A GitHub **Task** issue to update CI workflows → add `kind: maintenance`
> - A GitHub **Feature** issue that includes documentation → add `kind: documentation`

---

### 2. Workflow Status Labels

These labels track the lifecycle of an issue or pull request, helping maintainers and developers understand what action is needed next.

| Label | Description | Color |
| :---- | :---------- | :---- |
| `status: awaiting triage` | New issue that needs to be reviewed and categorized by maintainers | `#fbca04` |
| `status: needs info` | More information is required from the reporter before progress can be made | `#d876e3` |
| `status: needs repro` | A bug report that cannot be acted upon without a reproduction case | `#d93f0b` |
| `status: blocked` | Work is stalled due to an external dependency, upstream issue, or required decision | `#000000` |
| `status: in progress` | Someone is actively working on this issue | `#fef2c0` |
| `status: ready for dev` | The issue is fully defined and ready for a contributor to pick up | `#c2e0c6` |
| `status: needs review` | The pull request is ready for a maintainer to review | `#1d76db` |
| `status: needs revision` | A pull request that requires changes before it can be merged | `#e99695` |

---

### 3. Priority Labels

Priority labels help align development efforts with the project roadmap. Use these to signal urgency and importance.

| Label | Description | Color |
| :---- | :---------- | :---- |
| `priority: critical` | Must be fixed immediately; affects core functionality, security, or causes data loss | `#b60205` |
| `priority: high` | Important issue that should be prioritized in the current sprint/release | `#d93f0b` |
| `priority: medium` | Normal priority; to be addressed in the standard development cycle | `#fbca04` |
| `priority: low` | Non-urgent tasks, nice-to-have improvements, or minor issues | `#0e8a16` |

---

### 4. Skill Level Labels

These labels categorize issues by the experience level required to complete them. They help new contributors find appropriate entry points and help maintainers create a progression path for community members.

| Label | Description | Color |
| :---- | :---------- | :---- |
| `skill: good first issue candidate` | Proposed as a potential good first issue; needs maintainer review before assignment | `#e6ccff` |
| `skill: good first issue` | Simple, well-scoped tasks ideal for someone new to the repository or open source | `#7057ff` |
| `skill: beginner` | Suitable for contributors who have completed a good first issue and want to build skills | `#9f7be1` |
| `skill: intermediate` | Requires familiarity with the codebase structure and SDK concepts | `#6f42c1` |
| `skill: advanced` | Requires deep understanding of the SDK architecture and may span multiple modules | `#4a2c7a` |

> **Note:** The `skill: good first issue` label corresponds to GitHub's built-in "good first issue" feature, which surfaces these issues in GitHub's contributor-friendly views.

---

### 5. Scope Labels

These labels identify which subsystem or area of the repository is affected. They are generic enough to apply across all language SDKs.

| Label | Description | Color |
| :---- | :---------- | :---- |
| `scope: api` | Related to the public SDK API surface or interfaces | `#bfdadc` |
| `scope: build` | Related to the build system (CMake, Maven, Gradle, Cargo, npm, etc.) | `#cfd3d7` |
| `scope: ci` | Related to GitHub Actions, automated testing workflows, or CI/CD pipelines | `#62b0e2` |
| `scope: dependencies` | Related to dependency updates, version bumps, or dependency management | `#0366d6` |
| `scope: examples` | Related to the example projects or code snippets provided for users | `#bfd4f2` |
| `scope: grpc` | Related to gRPC communication, protobuf definitions, or network layer | `#5319e7` |
| `scope: crypto` | Related to cryptographic operations, key management, or signing | `#b4a7d6` |

---

### 6. Resolution Labels

Used when closing issues or PRs without a merge to explain the reason.

| Label | Description | Color |
| :---- | :---------- | :---- |
| `resolution: duplicate` | This issue or pull request already exists elsewhere | `#cfd3d7` |
| `resolution: invalid` | This doesn't seem right, is spam, or is based on a misunderstanding | `#e4e669` |
| `resolution: wontfix` | This will not be worked on; out of scope or intentional behavior | `#ffffff` |
| `resolution: stale` | Closed due to inactivity without resolution | `#ededed` |

---

## Quick Reference Table

Below is the complete list of all standardized labels for easy reference:

| Label | Description | Color |
| :---- | :---------- | :---- |
| **Work Kind** | | |
| `kind: enhancement` | Improvement to existing code or performance without adding new features | `#3e82f7` |
| `kind: documentation` | Improvements or additions to READMEs, guides, API docs, or code comments | `#0075ca` |
| `kind: refactor` | Code changes that neither fix a bug nor add a feature | `#d4c5f9` |
| `kind: security` | Security vulnerabilities or security-related improvements | `#e11d21` |
| `kind: testing` | Adding or improving unit tests, integration tests, or test infrastructure | `#c5def5` |
| `kind: maintenance` | Updates to CI/CD pipelines, build systems, dependencies, or tooling | `#6a737d` |
| **Status** | | |
| `status: awaiting triage` | New issue that needs to be reviewed and categorized by maintainers | `#fbca04` |
| `status: needs info` | More information is required from the reporter | `#d876e3` |
| `status: needs repro` | A bug report that cannot be acted upon without a reproduction case | `#d93f0b` |
| `status: blocked` | Work is stalled due to external dependency or required decision | `#000000` |
| `status: in progress` | Someone is actively working on this issue | `#fef2c0` |
| `status: ready for dev` | The issue is fully defined and ready for a contributor | `#c2e0c6` |
| `status: needs review` | The pull request is ready for maintainer review | `#1d76db` |
| `status: needs revision` | A pull request that requires changes before merge | `#e99695` |
| **Priority** | | |
| `priority: critical` | Must be fixed immediately | `#b60205` |
| `priority: high` | Important; should be prioritized in current sprint | `#d93f0b` |
| `priority: medium` | Normal priority; standard development cycle | `#fbca04` |
| `priority: low` | Non-urgent; nice-to-have improvements | `#0e8a16` |
| **Skill Level** | | |
| `skill: good first issue candidate` | Proposed as potential good first issue; needs maintainer review | `#e6ccff` |
| `skill: good first issue` | Simple tasks ideal for newcomers | `#7057ff` |
| `skill: beginner` | For contributors who have completed a good first issue | `#9f7be1` |
| `skill: intermediate` | Requires familiarity with codebase structure | `#6f42c1` |
| `skill: advanced` | Requires deep understanding; may span modules | `#4a2c7a` |
| **Scope** | | |
| `scope: api` | Related to the public SDK API surface | `#bfdadc` |
| `scope: build` | Related to the build system | `#cfd3d7` |
| `scope: ci` | Related to GitHub Actions or CI/CD | `#62b0e2` |
| `scope: dependencies` | Related to dependency updates | `#0366d6` |
| `scope: examples` | Related to example projects or snippets | `#bfd4f2` |
| `scope: grpc` | Related to gRPC/protobuf/network layer | `#5319e7` |
| `scope: crypto` | Related to cryptographic operations | `#b4a7d6` |
| **Resolution** | | |
| `resolution: duplicate` | Issue/PR already exists | `#cfd3d7` |
| `resolution: invalid` | Not valid, spam, or misunderstanding | `#e4e669` |
| `resolution: wontfix` | Out of scope or intentional behavior | `#ffffff` |
| `resolution: stale` | Closed due to inactivity | `#ededed` |

---

## Implementation Guidelines

### Using GitHub Issue Types + Labels Together

1. **Always set a GitHub Issue Type** (Bug, Feature, Task, Epic, or Initiative) when creating an issue
2. **Add labels for additional context** — `kind:`, `priority:`, `status:`, `skill:`, and `scope:` labels provide detail that issue types alone cannot convey
3. **Labels complement, not replace** — A GitHub "Bug" issue can also have `kind: security` to indicate it's a security-related bug

### Label Prefixing

Use the `category: name` format (e.g., `kind:`, `status:`, `priority:`) to ensure labels are grouped together alphabetically in the GitHub UI. This makes it easier for contributors and maintainers to find relevant labels.

### Label Exclusivity

- An issue should have exactly **one** GitHub issue type (Bug, Feature, Task, etc.)
- An issue may have **zero or one** `kind:` label (optional, for additional context)
- An issue should have exactly **one** `status:` label at any given time
- An issue should have at most **one** `priority:` label
- An issue should have at most **one** `skill:` label
- Multiple `scope:` labels can be applied if the issue spans multiple areas

### Applying Labels

1. **New Issues:** Set GitHub issue type + apply `status: awaiting triage` automatically or manually
2. **After Triage:** Add appropriate `kind:` (if applicable), `priority:`, and `skill:` labels
3. **When Ready:** Change status to `status: ready for dev` once the issue is well-defined
4. **On Assignment:** Change status to `status: in progress`
5. **PR Submitted:** Change to `status: needs review`
6. **Closing Without Merge:** Apply appropriate `resolution:` label

### Repo-Specific Labels

Repositories may add their own labels for ecosystem-specific needs. Examples include:

- **hiero-sdk-java:** `gradle`, `maven`, `android`
- **hiero-sdk-js:** `npm`, `browser`, `node`, `react-native`
- **hiero-sdk-swift:** `swift-6`, `swiftpm`, `ios`, `macos`
- **hiero-sdk-cpp:** `cmake`, `vcpkg`, `conan`
- **hiero-sdk-rust:** `cargo`, `crates.io`, `wasm`
- **hiero-sdk-python:** `pip`, `pypi`, `wheels`
- **hiero-sdk-go:** `go-modules`

These repo-specific labels should **not** replace the standardized labels but rather complement them.

---

## References

- [GitHub Documentation: Managing Labels](https://docs.github.com/en/issues/using-labels-and-milestones-to-track-work/managing-labels)
- [Python Developer's Guide: GitHub Labels](https://devguide.python.org/triage/labels/)
- [CNCF: Issue Labels for New Contributors](https://contribute.cncf.io/maintainers/templates/issue-labels)
- [StandardIssueLabels Repository](https://github.com/wagenet/StandardIssueLabels)
- [Creative Commons: Repository Labels](https://opensource.creativecommons.org/contributing-code/repo-labels)

---

## Changelog

| Date | Version | Description |
| :--- | :------ | :---------- |
| 2026-01-20 | 1.0.0 | Initial version |
