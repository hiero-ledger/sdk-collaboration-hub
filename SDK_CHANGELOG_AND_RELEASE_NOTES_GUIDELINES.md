This document outlines the best practices for writing changelogs and release notes for Hiero SDKs. Our goal is to ensure that all documentation is consistent, clear, and useful to developers.

---

## 1. Changelog Guidelines

A changelog is a concise, version-controlled record of changes in your project. We follow the [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) format. The changelog should reside in a file named `CHANGELOG.md` at the root of your repository.

### 1.1. Format

Group changes into the following categories:

- **Added:** New features and functionalities.
- **Changed:** Updates to existing functionality.
- **Fixed:** Bug fixes.
- **Deprecated:** Features that are no longer recommended for use.
- **Removed:** Deprecated features that have been eliminated.
- **Documentation:** Updates or improvements to documentation.

### 1.2. Writing Guidelines

- **Clarity & Conciseness:** Use clear and direct language. Avoid jargon and overly technical terms where possible.
- **Consistent Grouping:** Organize changes by category to help readers quickly find the information they need.
- **Linking HIPs:** When referencing a Hiero Improvement Proposal (HIP), include a link to the full HIP document.
- **Pull Request References:** At the end of each entry, include a link to the relevant pull request.
- **Versioning:** For beta releases, maintain the same structure. When a stable release follows a beta, ensure that the stable version overrides any beta-specific changes.

### 1.3. Example Changelog

```markdown
## [Unreleased]

### Added
- Support for HIP-1021 ([HIP Link](https://hips.hedera.com/hip/hip-1021)): Improves the assignment of auto-renew account IDs for topics, enhancing user experience in key areas. [#1234](link to the PR)
- New APIs in `AccountCreateTransaction`:
  - `setKeyWithAlias(ECDSAKey)`
  - `setKeyWithAlias(Key, ECDSAKey)`
  - `setKeyWithoutAlias(Key)`

  These APIs streamline account creation with aliases. [#1234](link to the PR)

### Changed
- A new query payment is generated when the SDK receives a status of `BUSY`. This change prevents the `DUPLICATE_TRANSACTION` error during heavy network loads. [PR Link]([#1234](link to the PR))

### Fixed
- Resolved an issue where the service port was incorrectly overridden in `NodeUpdateTransaction` when converting from/to bytes. [#1234](link to the PR)

### Removed
- `AccountStakersQuery` has been permanently removed due to long-term lack of support by consensus nodes. [#1234](link to the PR)

### Documentation
- Updated and improved repository documentation for clarity on usage patterns and API integration. [#1234](link to the PR)

```

---

## 2. Release Notes Guidelines

Release notes provide a detailed summary of changes for a given release, including context for major updates and migration instructions if necessary. They should be written in plain language and be accessible to both technical and non-technical stakeholders.

### 2.1. Format

Group release notes into the following categories:

- **Summary:** A brief overview of key changes, enhancements, and improvements.
- **Upgrading:** Instructions or links to migration guides.
- **Breaking Changes:** Clearly highlight any changes that may require action from developers.
- **Deprecated**: Clearly highlight any deprecations.
- **Enhancements:** New features and improvements.
- **Bug Fixes:** A summary of resolved issues.
- **Dependency Changes:** Any updates to libraries or third-party dependencies.
- **Documentation:** Notable updates to documentation.

### 2.2. Writing Guidelines

- **Summarize Key Changes:** Start with a concise summary that explains the major updates and their benefits.
- **Highlight Breaking Changes:** Use a dedicated section to clearly indicate any breaking changes and explain their impact.
- **Upgrade Instructions:** Include specific instructions or links to migration guides to assist developers in transitioning between versions.
- **Link HIPs and PRs:** Just as in the changelog, provide links to HIPs and related pull requests where applicable.

### 2.3. Example Release Notes

```markdown
## [Unreleased]

This release introduces support for HIP-991, which establishes a fixed fee system for topic messages on the Hedera network. For more details, see [HIP-991](https://hips.hedera.com/hip/hip-991). This change improves economic scalability by enhancing resource allocation and revenue distribution for topic operators.

### Breaking Changes
- Shard properties have been restructured. The property `hedera.mirror.rest.shard` has been removed in favor of the unified properties `hedera.mirror.common.shard` and `hedera.mirror.common.realm`. Most users are unaffected; however, if your integration depends on these properties, please update your configuration accordingly.

### Upgrading
For migration details, please refer to the [Migration Guide](/manual/).

### Enhancements
- `PrivateKey.GetRecoveryId`: A new method to retrieve the recovery ID (the 'v' value) associated with ECDSA signatures, which improves the signature verification process. #1324

  **Usage Example:**

  ```go
  r := sig[0:32]
  s := sig[32:64]
  v := ecdsaPrivateKey.GetRecoveryId(r, s, messageBytes)

  eip1559tx.PushBack(NewRLPItem(VALUE_TYPE).AssignValue(recIdBytes))
  eip1559tx.PushBack(NewRLPItem(VALUE_TYPE).AssignValue(r))
  eip1559tx.PushBack(NewRLPItem(VALUE_TYPE).AssignValue(s))
  
- `Revenue Generating Topics`: Added support for HIP-991. This enhancement introduces a fixed fee mechanism for topic messages, addressing economic scalability on the Hedera network. [HIP-991](https://hips.hedera.com/hip/hip-991)

### Bug Fixes

- **VerifyTransaction Issue:** Fixed a bug in the `VerifyTransaction` method where building the proto transaction body resulted in an `INVALID_SIGNATURE` error. The build logic has been removed and replaced with a check for the presence of the public key. [#1234](link to the PR)

### Dependency Changes

- Upgraded `golang.org/x/net` from version 0.34.0 to 0.36.0. [#1234](link to the PR)

### Documentation

- Revised the usage documentation for `MirrorNodeContractQueries`. When using a sender with an ECDSA key and alias, retrieve the account’s EVM address from the Mirror Node API and pass it via `setSenderEvmAddress()` instead of `setSender()`. [#1234](link to the PR
```