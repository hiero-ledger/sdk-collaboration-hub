# Changelog Entry Guide

This document explains how to add a changelog entries.

> ⚠️ **WARNING: Do not blindly follow this guide**. Some repositories may have different approaches.

## Contents

- [Why Changelog Entries](#why-changelog-entries)
- [Changelog Sections](#changelog-sections)
- [Step by Step Guide](#step-by-step-guide)

## Why Changelog Entries

The changelog helps maintainers understand what's new or changed, in order to prepare and document a new release. This means a changelog entry helps to create better documented releases, so developers can have as smooth an experience as possible when upgrading.


## Changelog Sections

The `[Unreleased]` section is divided into topic categories. Choose the one that best fits your change:

### Src
For changes, fixes or additions to source code.

### Tests
For changes, fixes or additions to unit, integration or tck tests.

### Examples
For changes, fixes or additions to documentation.

### .github
For changes, fixes or additions to .github file.

### Packages
For updates to packages.

## Step by Step Guide

A good changelog entry should:

1. **Open `CHANGELOG.md`** in the repository root
2. **Write under title: [Unreleased]** - Add your entry at the top of the file, in the appropriate section under `[Unreleased]`
3. **Write under the appropriate subheading(s)** - Find the relevant subheading(s) under [UNRELEASED]
4. **Use conventional commit language** - Be concise but informative with a pre-fix e.g. `feat`
5. **Link to the issue and PR** - Include a reference to the PR and issue resolved
6. **Use markdown** - Follow the bullet point format

```
# UNRELEASED
...

## Src
- feat: added TokenCreateTransaction, issue #100 and PR #90

## Tests
- chore: created unit and integration tests for TokenCreateTransaction, issue #100 and PR #90

## Docs
- chore: wrote an example `token-create-transaction`, issue #100 and PR #90
```

> ⚠️ **WARNING: Update your Changelog with a new Release**. A new release will mean existing pull requests will need their changelog entry migrated to the new unreleased section.
