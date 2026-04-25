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

The labels defined in this document **complement** GitHub's issue types—they do not replace them. Use GitHub's built-in types for primary classification (bug vs. feature vs. task), and use these labels for additional context like priority, status, skill level, and scope.

---

## Label Categories

### 1. Workflow Status Labels

Status labels track the lifecycle of an issue or pull request. They are split into two groups: labels that apply to **issues** and labels that apply to **pull requests**. A given issue or PR should have exactly one status label at any time.

#### Issues

| Label | Description | Color |
| :---- | :---------- | :---- |
| `status: needs triage` | New issue that needs to be reviewed and categorized by maintainers | `#fbca04` |
| `status: needs info` | A maintainer has reviewed the issue but requires more information from the submitter | `#d876e3` |
| `status: blocked` | Cannot proceed due to an external dependency, upstream issue, or required decision | `#000000` |
| `status: ready for dev` | Fully defined and ready for a contributor to pick up | `#c2e0c6` |
| `status: in progress` | A contributor is actively working on this issue | `#fef2c0` |

#### Pull Requests

| Label | Description | Color |
| :---- | :---------- | :---- |
| `status: needs review` | The pull request is ready for maintainer review | `#1d76db` |
| `status: needs revision` | The pull request requires changes from the author before it can be reviewed or merged | `#e99695` |
| `status: awaiting merge` | The pull request is approved and ready to merge once CI passes | `#6f42c1` |

---

### 2. Priority Labels

Priority labels help align development efforts with the project roadmap. Use these to signal urgency and importance.

| Label | Description | Color |
| :---- | :---------- | :---- |
| `priority: critical` | Must be fixed immediately; affects core functionality, security, or causes data loss | `#b60205` |
| `priority: high` | Important issue that should be prioritized in the current sprint/release | `#d93f0b` |
| `priority: medium` | Normal priority; to be addressed in the standard development cycle | `#fbca04` |
| `priority: low` | Non-urgent tasks, nice-to-have improvements, or minor issues | `#0e8a16` |

---

### 3. Skill Level Labels

These labels categorize issues by the experience level required to complete them. They help new contributors find appropriate entry points and help maintainers create a progression path for community members.

| Label | Description | Color |
| :---- | :---------- | :---- |
| `skill: good first issue` | Simple, well-scoped tasks ideal for someone new to the repository or open source | `#7057ff` |
| `skill: beginner` | Suitable for contributors who have completed a good first issue and want to build skills | `#9f7be1` |
| `skill: intermediate` | Requires familiarity with the codebase structure and SDK concepts | `#6f42c1` |
| `skill: advanced` | Requires deep understanding of the SDK architecture and may span multiple modules | `#4a2c7a` |

> **Note:** The `skill: good first issue` label corresponds to GitHub's built-in "good first issue" feature, which surfaces these issues in GitHub's contributor-friendly views.

---

### 4. Scope Labels

These labels identify which area of the repository is affected. They answer the question "what does this touch?" and are the primary way to categorize the nature of work beyond the GitHub issue type. Multiple scope labels can be applied if an issue or PR spans several areas.

| Label | Description | Color |
| :---- | :---------- | :---- |
| `scope: api` | Related to the public SDK API surface or interfaces | `#bfdadc` |
| `scope: build` | Related to the build system (CMake, Maven, Gradle, Cargo, npm, etc.) | `#cfd3d7` |
| `scope: ci` | Related to GitHub Actions, automated testing workflows, or CI/CD pipelines | `#62b0e2` |
| `scope: crypto` | Related to cryptographic operations, key management, or signing | `#b4a7d6` |
| `scope: dependencies` | Related to dependency updates, version bumps, or dependency management | `#0366d6` |
| `scope: docs` | Related to READMEs, guides, API documentation, or code comments | `#0075ca` |
| `scope: examples` | Related to the example projects or code snippets provided for users | `#bfd4f2` |
| `scope: grpc` | Related to gRPC communication, protobuf definitions, or the network layer | `#5319e7` |
| `scope: security` | Related to security vulnerabilities or security-sensitive areas of the codebase | `#e11d21` |
| `scope: tests` | Related to unit tests, integration tests, or test infrastructure | `#c5def5` |

---

## Quick Reference Table

Below is the complete list of all standardized labels for easy reference:

| Label | Description | Color |
| :---- | :---------- | :---- |
| **Status — Issues** | | |
| `status: needs triage` | New issue that needs to be reviewed and categorized | `#fbca04` |
| `status: needs info` | Reviewed; waiting on more information from the submitter | `#d876e3` |
| `status: blocked` | Cannot proceed due to external dependency or required decision | `#000000` |
| `status: ready for dev` | Fully defined and ready for a contributor to pick up | `#c2e0c6` |
| `status: in progress` | A contributor is actively working on this issue | `#fef2c0` |
| **Status — Pull Requests** | | |
| `status: needs review` | Ready for maintainer review | `#1d76db` |
| `status: needs revision` | Requires changes from the author before it can proceed | `#e99695` |
| `status: awaiting merge` | Approved and ready to merge once CI passes | `#6f42c1` |
| **Priority** | | |
| `priority: critical` | Must be fixed immediately | `#b60205` |
| `priority: high` | Important; should be prioritized in current sprint | `#d93f0b` |
| `priority: medium` | Normal priority; standard development cycle | `#fbca04` |
| `priority: low` | Non-urgent; nice-to-have improvements | `#0e8a16` |
| **Skill Level** | | |
| `skill: good first issue` | Simple tasks ideal for newcomers | `#7057ff` |
| `skill: beginner` | For contributors who have completed a good first issue | `#9f7be1` |
| `skill: intermediate` | Requires familiarity with codebase structure | `#6f42c1` |
| `skill: advanced` | Requires deep understanding; may span modules | `#4a2c7a` |
| **Scope** | | |
| `scope: api` | Related to the public SDK API surface | `#bfdadc` |
| `scope: build` | Related to the build system | `#cfd3d7` |
| `scope: ci` | Related to GitHub Actions or CI/CD | `#62b0e2` |
| `scope: crypto` | Related to cryptographic operations | `#b4a7d6` |
| `scope: dependencies` | Related to dependency updates | `#0366d6` |
| `scope: docs` | Related to documentation | `#0075ca` |
| `scope: examples` | Related to example projects or snippets | `#bfd4f2` |
| `scope: grpc` | Related to gRPC/protobuf/network layer | `#5319e7` |
| `scope: security` | Related to security vulnerabilities or sensitive areas | `#e11d21` |
| `scope: tests` | Related to tests or test infrastructure | `#c5def5` |

---

## Implementation Guidelines

### Using GitHub Issue Types + Labels Together

1. **Always set a GitHub Issue Type** (Bug, Feature, Task, Epic, or Initiative) when creating an issue
2. **Add labels for additional context** — `priority:`, `status:`, `skill:`, and `scope:` labels provide detail that issue types alone cannot convey
3. **Labels complement, not replace** — a GitHub "Bug" issue about a security vulnerability should also have `scope: security`

### Label Prefixing

Use the `category: name` format (e.g., `status:`, `priority:`, `scope:`) to ensure labels are grouped together alphabetically in the GitHub UI. This makes it easier for contributors and maintainers to find relevant labels.

### Label Exclusivity

- An issue should have exactly **one** GitHub issue type (Bug, Feature, Task, etc.)
- An issue should have exactly **one** `status:` label at any given time
- A pull request should have exactly **one** `status:` label at any given time
- An issue should have at most **one** `priority:` label
- An issue should have at most **one** `skill:` label
- An issue should have **at least one** `scope:` label by the time it reaches `status: ready for dev`
- Multiple `scope:` labels can be applied if the issue or PR spans multiple areas

### Issue Lifecycle

1. **New issue:** apply `status: needs triage` (can be automated via issue templates)
2. **After triage:** add `priority:`, `skill:`, and at least one `scope:` label; change status to `status: ready for dev` once the issue is well-defined, or `status: needs info` if more information is required from the submitter
3. **On assignment:** change status to `status: in progress`

### PR Lifecycle

1. **PR opened:** apply `status: needs review`
2. **Changes requested:** change to `status: needs revision`
3. **Author pushes fixes:** change back to `status: needs review`
4. **PR approved, CI passing:** change to `status: awaiting merge`

> GitHub's native review state (approved, changes requested) and CI status are visible directly in the PR interface. The `status:` labels on PRs serve as a high-level signal in the PR list view and for automation, not a replacement for GitHub's built-in review tooling.

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
- [StandardIssueLabels Repository](https://github.com/wagenet/StandardIssueLabels)
- [Creative Commons: Repository Labels](https://opensource.creativecommons.org/contributing-code/repo-labels)
