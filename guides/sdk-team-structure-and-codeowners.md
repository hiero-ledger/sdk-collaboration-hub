# SDK Team Structure & Code Ownership

> **Status: Planned / upcoming change.** This document describes a change to the
> GitHub team and permission structure for the Hiero SDKs that has **not been
> rolled out yet**. It is shared in advance so that everyone working on the SDKs
> understands what is changing, why, and what (if anything) they need to do.
> Nothing in your day-to-day workflow changes until the migration is announced
> as completed.

> **Governance basis.** This guide applies the Hiero governance model defined in
> [
`governance/roles/roles-and-groups.md`](https://github.com/hiero-ledger/governance/blob/main/roles/roles-and-groups.md)
> to the SDKs. That document is authoritative for roles, teams, permission
> levels, the `CODEOWNERS` rules, and the voting process — this guide only
> describes how the SDKs apply it, and the governance document wins wherever the
> two differ. Importantly, the model explicitly allows a "project" to be **a
> group of related repositories**, not just a single repo. That is exactly what
> lets us treat *the SDKs as one project* with a single shared set of role teams.

---

## Overview

- Today every SDK has its **own** committer and maintainer team
  (`hiero-sdk-java-committers`, `hiero-sdk-python-maintainers`, …).
- We are merging these per-language teams into **two shared teams** for the whole
  SDK community: `hiero-sdk-committers` and `hiero-sdk-maintainers`.
- After the merge, a member of `hiero-sdk-maintainers` has maintainer rights on
  **all** SDK repositories — the SDK people become one community rather than
  several isolated silos.
- Because that is a broad grant, each SDK can still keep tight control over
  sensitive parts of its repository by defining a dedicated code-owner team
  (e.g. `hiero-sdk-java-crypto-codeowners`) and referencing it in that repo's
  `CODEOWNERS` file. This is **opt-in** and decided by each SDK's current
  maintainers.
- For triage, `hiero-sdk-python-triage` is folded into the single shared
  `hiero-sdk-triage` team. The newcomer-support team
  `hiero-sdk-good-first-issue-support` is **renamed to
  `hiero-good-first-issue-support`** (it spans the website too, so it is an
  org-wide onboarding function, not an SDK-specific one).

---

## 1. Background & motivation

The Hiero SDKs (Java, JavaScript/TypeScript, Go, Rust, Python, C++, Swift, plus
the cross-SDK TCK) are maintained today by separate GitHub teams — one
committer team and one maintainer team per language. The current SDK teams in
[`hiero-ledger/governance/config.yaml`](https://github.com/hiero-ledger/governance/blob/main/config.yaml)
are:

| Repository         | Committer team                | Maintainer team                |
|--------------------|-------------------------------|--------------------------------|
| `hiero-sdk-cpp`    | `hiero-sdk-cpp-committers`    | `hiero-sdk-cpp-maintainers`    |
| `hiero-sdk-go`     | `hiero-sdk-go-committers`     | `hiero-sdk-go-maintainers`     |
| `hiero-sdk-java`   | `hiero-sdk-java-committers`   | `hiero-sdk-java-maintainers`   |
| `hiero-sdk-js`     | `hiero-sdk-js-committers`     | `hiero-sdk-js-maintainers`     |
| `hiero-sdk-python` | `hiero-sdk-python-committers` | `hiero-sdk-python-maintainers` |
| `hiero-sdk-rust`   | `hiero-sdk-rust-committers`   | `hiero-sdk-rust-maintainers`   |
| `hiero-sdk-swift`  | `hiero-sdk-swift-committers`  | `hiero-sdk-swift-maintainers`  |
| `hiero-sdk-tck`    | `hiero-sdk-tck-committers`    | `hiero-sdk-tck-maintainers`    |

This per-language split has a cost: it makes the SDKs feel like separate
projects instead of one effort. People who are perfectly capable of reviewing
and helping across languages cannot do so without being added to yet another
team, cross-SDK alignment is harder, and contributors have a fragmented picture
of "who owns the SDKs."

The people working on the SDKs are **one community**. The team structure should
reflect that.

## 2. The new team model

Hiero governance defines four roles — Contributor, Junior Committer, Committer,
and Maintainer — and, per project, a team for each role mapped to a fixed GitHub
permission level (see
[Definitions of GitHub roles](https://github.com/hiero-ledger/governance/blob/main/roles/roles-and-groups.md#definitions-of-github-roles)).
Today each SDK is treated as its own project, so each has its own role teams. We
are instead treating **the SDKs as a single project** (a group of related
repositories) with **one shared set of role teams** spanning every SDK repo:

| Role             | Shared team             | GitHub permission | What members can do                                                                                                      |
|------------------|-------------------------|-------------------|--------------------------------------------------------------------------------------------------------------------------|
| Maintainer       | `hiero-sdk-maintainers` | `MAINTAIN`        | Review, approve and merge PRs, manage the repo (within branch-protection limits), drive the roadmap, report to the TSC   |
| Committer        | `hiero-sdk-committers`  | `WRITE`           | Push branches, open/update PRs, merge where allowed — standard committer rights                                          |
| Junior Committer | `hiero-sdk-triage`      | `TRIAGE`          | Triage issues (assign, label, close/reopen); **no** write access, and reviews do **not** count toward required approvals |

Contributors have no team — anyone contributing is a Contributor by default. The
per-language `*-committers` and `*-maintainers` teams, and the per-language
`hiero-sdk-python-triage` team, are retired into the shared teams above.

> **Naming note.** Every team in `config.yaml` uses the `hiero-` prefix
> (`hiero-sdk-java-maintainers`, `hiero-consensus-node-committers`, …). To stay
> consistent we recommend the prefixed names `hiero-sdk-committers` and
> `hiero-sdk-maintainers`. They are sometimes referred to informally as just
> "sdk-committers" / "sdk-maintainers" — same teams.

In `config.yaml` the merged teams look like this (handles are illustrative):

```yaml
- name: hiero-sdk-committers
  maintainers:
    - <community-lead-handle>
  members:
    - <handle>
    - <handle>
    # … everyone previously in any hiero-sdk-<lang>-committers team

- name: hiero-sdk-maintainers
  maintainers:
    - <community-lead-handle>
  members:
    - <handle>
    - <handle>
    # … everyone previously in any hiero-sdk-<lang>-maintainers team
```

The repository-to-team permission mapping (which team gets `write` / `maintain`
/ `triage` on which repo) lives in the same `config.yaml` and will be updated so
that the shared teams are granted on each SDK repository.

> **About `maintainers:` vs `members:` in `config.yaml`.** The file is the
> configuration for [clowarden](https://clowarden.io), the tool that manages
> Hiero's teams. As the governance doc explains, the `maintainers:` block is
> **not** the project's Maintainers — clowarden simply requires at least one
> existing org member there to create a valid team, and the best practice is to
> list exactly one person. The actual people in a role go in `members:`.

## 3. What changes for you

- If you are a **committer** on any SDK today, you become a member of
  `hiero-sdk-committers` and keep committer rights — now across all SDK repos.
- If you are a **maintainer** on any SDK today, you become a member of
  `hiero-sdk-maintainers` and gain maintainer rights across all SDK repos.
- The old per-language `*-committers` / `*-maintainers` teams are retired once
  everyone has been moved over.

### Triage & onboarding teams

These teams all carry the same `triage` permission, but they serve different
purposes and are handled differently:

- **`hiero-sdk-python-triage` is merged into `hiero-sdk-triage`.** It is a
  per-language triage team doing the same job as the shared one — the same
  pattern as the committer/maintainer merge, and it maps to the **Junior
  Committer** role (`TRIAGE`). `hiero-sdk-triage` is already granted on
  `hiero-sdk-python`, so no new repository grants are needed; the members simply
  move over. Note for the Python triagers: your triage scope is Python-only today
  and becomes all SDK repositories after the merge. Governance already lists this
  team as a naming inconsistency in its
  [Open Topics](https://github.com/hiero-ledger/governance/blob/main/roles/roles-and-groups.md#open-topics),
  so folding it in also resolves that gap.
- **`hiero-sdk-triage` stays** as the single SDK-wide triage team (it is already
  granted on every SDK repo, `hiero-enterprise-java`, and this hub).
- **`hiero-sdk-good-first-issue-support` is renamed to
  `hiero-good-first-issue-support`.** Governance documents this as a
  **cross-project** team granted `triage` on multiple SDK repositories *and* the
  Hiero website, whose purpose is to create and curate good first issues across
  the ecosystem (see
  [Additional Roles](https://github.com/hiero-ledger/governance/blob/main/roles/roles-and-groups.md#additional-roles-for-supporting-onboarding-contribution-and-project-management)).
  Because it is an org-wide onboarding function rather than an SDK team, the `sdk`
  is dropped from the slug. Its membership and grants are otherwise unchanged. The
  rename has to be made in `config.yaml` **and** in `roles-and-groups.md`, which
  refers to the team by its current name.

> **Heads-up when renaming a team:** changing the team name changes its GitHub
> slug. Update every reference to the old slug at the same time — any
> `CODEOWNERS` entries, good-first-issue labelling/automation, `@`-mentions in
> issues/PRs, and docs that point at `@hiero-ledger/hiero-sdk-good-first-issue-support`.

### Out of scope (not affected by this work)

- The DID / identity SDKs (`hiero-did-sdk-js`, `hiero-did-sdk-python`) are part
  of the separate **Identity** community and governed via the
  [`identity-collaboration-hub`](https://github.com/hiero-ledger/identity-collaboration-hub),
  not this SDK consolidation.

If you think a team should be in scope (or out of it), raise it on the SDK
Community Call before the migration.

## 4. Why one shared maintainer team is safe: `CODEOWNERS`

A shared `hiero-sdk-maintainers` team means everyone in it *can* approve and
merge in every SDK repo. That is intentional — but some SDKs will legitimately
want to keep sign-off on sensitive areas (cryptography, transaction signing,
release/build configuration, the public API surface, …) in the hands of people
who know that codebase deeply.

GitHub's `CODEOWNERS` mechanism is exactly the tool for this, and it lets each
SDK decide for itself **without** going back to fragmented teams. Hiero
governance treats `CODEOWNERS` as a best practice that is **encouraged but not
required**, and documents how it interacts with our roles (see
[Usage of GitHub CODEOWNERS](https://github.com/hiero-ledger/governance/blob/main/roles/roles-and-groups.md#usage-of-github-codeowners-in-combination-with-our-roles-teams-and-rights)).

### How `CODEOWNERS` actually works

- A `CODEOWNERS` file maps file paths (glob patterns) to owners (teams or users).
- When combined with the branch-protection rule **"Require review from Code
  Owners"**, any PR that touches a matching path **must** be approved by a
  member of the owning team before it can be merged.
- **Last match wins.** If two patterns match the same file, only the *last* one
  in the file applies. This is what lets you carve out specific paths: a broad
  default of `* → hiero-sdk-maintainers` can be overridden for a single
  directory by a more specific line further down.

So `CODEOWNERS` does **not** take write access away from the shared maintainer
team — it adds a *required reviewer* for the paths you choose. The broad team
still does the everyday work everywhere; the designated code owners act as the
mandatory gate on the parts that need it.

### Two things people often get wrong

1. **The file is named `CODEOWNERS`, with no extension** (not `CODEOWNERS.md`).
   GitHub also accepts it in the repo root or in `docs/`, but to keep every repo
   consistent **the SDKs standardize on `.github/CODEOWNERS`** — put it there and
   nowhere else.
2. **The owner team must have access to the repo.** A team referenced in
   `CODEOWNERS` only takes effect if it has at least write access to that
   repository and is part of the org. Define and grant the team in
   `config.yaml` first (see below), then reference it in `CODEOWNERS`.

## 5. How an SDK can scope its own code ownership (opt-in)

If the current maintainers of an SDK want to keep sign-off over part of their
repository after the merge, follow these four steps. This is decided per SDK —
doing nothing is also fine; then the whole repo is simply owned by the shared
`hiero-sdk-maintainers` team.

### Step 1 — Define a code-owner team in `config.yaml`

Governance defines the naming pattern `PROJECT-SUBNAME-codeowners` for exactly
this purpose — "for large projects, smaller teams can be defined to assign
codeownership over specific parts of the codebase" (see
[Definition of teams](https://github.com/hiero-ledger/governance/blob/main/roles/roles-and-groups.md#definition-of-teams)).
The consensus-node repo already uses it, e.g.
`hiero-consensus-node-execution-codeowners`. For the Java SDK that means one team
per area:

```yaml
- name: hiero-sdk-java-crypto-codeowners
  maintainers:
    - <handle>          # clowarden requires one org member here; not the "owners"
  members:
    - <handle>
    - <handle>
- name: hiero-sdk-java-release-codeowners
  maintainers:
    - <handle>
  members:
    - <handle>
```

A code-owner team is granted `WRITE`. Per governance, a codeowner **must already
be a Committer or Maintainer** on the project — `write` is the minimum permission
for a codeowner, so Contributors and Junior Committers cannot be code owners.

### Step 2 — Grant the team access to the repository

In the repository-permission section of `config.yaml`, give the new team `write`
access to the SDK repo so GitHub honours it as a valid code owner.

### Step 3 — Add or update the `CODEOWNERS` file

In the SDK repository, create `.github/CODEOWNERS`. Set a broad
default to the shared team, then override the sensitive paths with your
code-owner team. Paths below are **illustrative — adapt them to your repo's
real layout**:

```text
# Everything in the repo defaults to the shared SDK maintainer community
*                                   @hiero-ledger/hiero-sdk-maintainers

# Security-critical code requires sign-off from the Java crypto code owners
/src/main/java/.../crypto/          @hiero-ledger/hiero-sdk-java-crypto-codeowners

# Build & release configuration
/gradle/                            @hiero-ledger/hiero-sdk-java-release-codeowners
/*.gradle                           @hiero-ledger/hiero-sdk-java-release-codeowners
/.github/workflows/                 @hiero-ledger/hiero-sdk-java-release-codeowners
```

Because last match wins, a PR that only touches `crypto/` requires a review from
`hiero-sdk-java-crypto-codeowners`; a PR touching unrelated source files requires
a review from the shared `hiero-sdk-maintainers` team.

### Step 4 — Enable the branch-protection rule

In the repo's branch-protection settings for the default branch, enable **"Require
review from Code Owners"**. Without this, `CODEOWNERS` only *requests* reviews —
it does not enforce them.

## 6. Worked example: the Java SDK

1. The Java maintainers decide that cryptography and the release pipeline must
   keep dedicated sign-off, but everything else is open to the whole SDK
   community.
2. They add `hiero-sdk-java-crypto-codeowners` and
   `hiero-sdk-java-release-codeowners` to `config.yaml` with the relevant people
   (all of whom are already SDK committers or maintainers) and grant each `write`
   on `hiero-sdk-java`.
3. They add a `CODEOWNERS` file with `*` owned by `hiero-sdk-maintainers`, the
   `crypto` path owned by `hiero-sdk-java-crypto-codeowners`, and the
   build/release paths owned by `hiero-sdk-java-release-codeowners`.
4. They turn on "Require review from Code Owners".

Result: any maintainer in the SDK community can review and merge ordinary Java
PRs, but a change to signing code or the release workflow still needs a green
review from the Java code owners.

## 7. Timeline & what you need to do

- **Now → migration:** review this document, ask questions on the SDK Community
  Call, and — if your SDK wants scoped ownership — agree internally on which
  paths and which people should be code owners.
- **Before the migration is executed:** SDKs that want code-owner teams should
  prepare their `config.yaml` and `CODEOWNERS` changes so they can land together
  with the merge (no gap where the broad team is the only owner).
- **At migration:** the per-language committer/maintainer teams are merged into
  the two shared teams; the exact date will be announced separately. The merge,
  the `hiero-sdk-python-triage` fold-in, and the `hiero-good-first-issue-support`
  rename are all executed as `config.yaml` PRs under the normal
  [voting process](https://github.com/hiero-ledger/governance/blob/main/roles/roles-and-groups.md#voting).

If your SDK is happy for the whole repo to be owned by the shared maintainer
community, **you do not need to do anything.**

## 8. FAQ

**Will I lose access to my SDK?**
No. Everyone keeps the equivalent role they have today; maintainers gain access
across all SDKs rather than losing any.

**Does the merge weaken protection on my SDK?**
Only if you choose to do nothing *and* you previously relied on team membership
to gate reviews. Use `CODEOWNERS` + "Require review from Code Owners" to keep or
strengthen that gate on the specific paths you care about.

**Can a code-owner team block the shared maintainers entirely?**
You can set `* @hiero-ledger/hiero-sdk-<lang>-<area>-codeowners` as the default so
the code-owner team must review every PR — but that works against the "one
community" goal. Prefer scoping to specific sensitive paths.

**Do `CODEOWNERS` entries control who can push, or who must review?**
Who must **review**. Push/merge access comes from the repo permission granted to
the teams in `config.yaml`. `CODEOWNERS` adds required reviewers on top of that.

**Where do I propose a change to teams or permissions?**
Open a pull request against
[`hiero-ledger/governance/config.yaml`](https://github.com/hiero-ledger/governance/blob/main/config.yaml),
following the
[Creating a PR](https://github.com/hiero-ledger/governance/blob/main/roles/roles-and-groups.md#creating-a-pr-to-add-or-remove-a-person-for-a-specific-role)
and [Voting](https://github.com/hiero-ledger/governance/blob/main/roles/roles-and-groups.md#voting)
processes — role changes are decided by a maintainer vote run with
[GitVote](https://github.com/cncf/gitvote). Discuss bigger structural questions on
the SDK Community Call first.

## References

- **Roles and Groups (authoritative governance):**
  <https://github.com/hiero-ledger/governance/blob/main/roles/roles-and-groups.md>
- Governance config (`config.yaml`, managed via clowarden):
  <https://github.com/hiero-ledger/governance/blob/main/config.yaml>
- Asynchronous voting guideline:
  <https://github.com/hiero-ledger/governance/blob/main/rules-and-guidelines/asynchronous-voting.md>
- Project rules / exceptions tracker:
  <https://github.com/hiero-ledger/governance/blob/main/project-rules-exceptions.md>
- SDK Collaboration Hub: <https://github.com/hiero-ledger/sdk-collaboration-hub>
- GitHub docs — About code owners:
  <https://docs.github.com/articles/about-code-owners>
- GitHub docs — About protected branches (require code-owner review):
  <https://docs.github.com/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches>