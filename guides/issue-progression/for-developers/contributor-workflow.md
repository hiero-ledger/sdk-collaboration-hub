# Contribution Workflow

This guide explains the recommended workflow for contributing to this repository.  
It covers tooling, repository setup, branching, commit standards, testing, and submitting pull requests.

## Table of Contents

- [1. Get Assigned to an Issue](#1-get-assigned-to-an-issue)

- [2. Create a Branch](#2-create-a-branch)

- [3. Commit Your Changes](#3-commit-your-changes)
  - [Achieving Conventionally Named Commits](#achieving-conventionally-named-commits)
  - [DCO and GPG Signing Commits](#dco-and-gpg-signing-commits)
  - [Breaking Changes](#breaking-changes)
    - [Identifying Whether Your Pull Request Introduces a Breaking Change](#identifying-whether-your-pull-request-introduces-a-breaking-change)
    - [What to Do If a Breaking Change Is Unavoidable](#what-to-do-if-a-breaking-change-is-unavoidable)
- [4. Add Changelog Entry](#4-create-a-changelog-entry)
- [5. Submitting a Pull Request](#5-submitting-a-pull-request)
---


## 1. Get Assigned to an Issue

Claim an issue by commenting: /assign

> ⚠️ **WARNING: You may have to wait for assignment**. This is not always automatic.

Key steps:
1. Find an available `Good First Issue` that interests you and is not yet assigned.

- [See Unassigned Good First Issues at Hiero](https://github.com/issues?q=is%3Aopen%20is%3Aissue%20org%3Ahiero-ledger%20archived%3Afalse%20no%3Aassignee%20(label%3A%22good%20first%20issue%22%20OR%20label%3A%22skill%3A%20good%20first%20issue%22)%20(repo%3Ahiero-ledger%2Fhiero-sdk-cpp%20OR%20repo%3Ahiero-ledger%2Fhiero-sdk-swift%20OR%20repo%3Ahiero-ledger%2Fhiero-sdk-python%20OR%20repo%3Ahiero-ledger%2Fhiero-sdk-js%20OR%20repo%3Ahiero-ledger%2Fhiero-website))

2. Comment replying to the issue with: `/assign`
3. You'll be assigned by a maintainer or bot

Once assigned, you are ready to work.

> ⚠️ **WARNING: Wait for assignment**. Commence work only after assigned.

> ⚠️ **WARNING: If the issue is already assigned, it is claimed and PR's will be closed**. We will not re-assign unless the issue is stale.


## 2. Create a Branch

Work on a branch to help keep the repository history clean and avoid major issues.

Before you create a branch, remember to pull in all recent changes from main.

One-time only:
```bash
git remote add upstream https://github.com/hiero-ledger/your-repository-name.git
```

Verify it is correctly set:
```bash
git remote -v
```

You should now see:
origin → your fork
upstream → the official repository

Once correctly set, pull any changes from upstream:
```bash
git checkout main
git fetch upstream
git pull upstream main
git push origin
```

Lastly, create a branch:
```bash
git checkout -b my-new-branch-name
```

Eventually, you'll need to regularly rebase to keep your branch in sync with the upstream repository [Rebase Guide](rebasing.md)

## 3. Create a Changelog Entry

Create a changelog entry by reading [Guide](guides/issue-progression/docs/changelog-guide.md)


## 3. Commit Your Changes
Solve the issue and commit your changes.

Make sure to:
- ✅ Read the Issue Description Carefully
- ✅ Ensure you are meeting all requirements

As you commit, make sure the commits are:
- ✅ Conventionally Named [Guide](https://www.conventionalcommits.org/en/v1.0.0/#summary)
- ✅ Signed correctly [Guide](guides/issue-progression/docs/signing.md)

### Achieving Conventionally Named Commits
A conventionally named commit is one that summarises in words what was just commited with a suitable pre-fix.

This is correct:

```bash
git commit -S -s -m "fix: fixed receipt status error catching in get_name"
```

This is incorrect:

```bash
git commit -S -s -m "looks like its mostly working now"
```

Read about conventional commit messages here: [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/#summary)

## DCO and GPG Signing Commits
**Each** commit in a pull request needs to be:
- `DCO` signed with an `-s` flag
- `GPG` key signed with an `-S` flag and a GPG key set up

For example:
```bash
git commit -S -s -m "chore: changelog entry for TokenCreateTransaction"
```

Follow our [Signing Guide](signing.md) with step-by-step instructions.

**⚠️ WARNING**: using the default commit button on GitHub desktop or VS Studio will result in un-signed commits.

**⚠️ WARNING** any merge or rebase operations will cause a loss of signing status unless you preserve signing: `git rebase main -S`

## Breaking Changes

Breaking changes are generally not acceptable. This is because they can:
- Stop existing code from working
- Force users to spend time and resources updating their applications
- Remove functionality that users may rely on, with no equivalent replacement

Breaking changes damage functionality and trust with our users and should be avoided whenever possible.

### Identifying Whether Your Pull Request Introduces a Breaking Change

Even if an issue does not mention breaking changes, a pull request may still introduce one.

Common examples include:
- Removing or renaming an existing function or class
- Changing the return type or structure of a function or method
- Modifying the parameters a function accepts (adding, removing, or changing types)
- Refactoring a function or class in a way that changes its behaviour, even subtly
- Changing default values or altering side effects

When preparing a pull request, always evaluate whether any existing user code would stop working as a result of your changes even if its 'better'.

For example - before:
```python
def transfer_tokens(account_id: str, amount: int):
    ...
```

For example - after - breaking:
```python
def transfer_tokens(account_id: AccountId, amount: int, memo: str = None):
    ...
```
User code passing a string account_id now fails, and adding a required memo parameter breaks all existing calls.


### What to Do If a Breaking Change Is Unavoidable

Breaking changes should be avoided, but in rare cases they are necessary.

Examples include:
- Correcting significant errors or faulty behaviour
- Implementing new standards or APIs (such as HIPS)
- Replacing deprecated functionality that cannot be maintained

If a breaking change must occur:
- Clearly communicate it in your commit messages and changelog.
- Provide a detailed explanation in the changelog.
- When possible, implement or propose backwards compatibility solutions (deprecation warnings, transitional methods, alternative APIs, etc.).

Example changelog entry:

`BREAKING CHANGE: transfer_tokens() now requires an AccountId object instead of a string.`


Breaking changes are typically scheduled for major releases, giving users time to prepare and migrate safely.

## 5. Submitting a Pull Request

Once you have completed your work on a dedicated branch and followed all contribution requirements, you are ready to submit a pull request (PR)!

This guide walks you through each step of the PR process.


1. Get Ready
Verify you are on your feature branch:
```
git branch --show-current
```

2. Push Your Changes
If you haven’t already pushed your changes to your fork:

```bash
git push origin <your-branch-name>
```

3. Open a Pull Request to the Repository

Navigate the repository pull request section:
`https://github.com/hiero-ledger/repository-name/pulls`

You will see a banner showing your branch with a “Compare & pull request” button. Click it.

3. Write a Good Title and Description
Conventionally Name your *Pull Request* Title [Guide](https://www.conventionalcommits.org/en/v1.0.0/#summary)

For example:
`chore: Fix typo Tokenyd`

Add a brief description and any important notes.

4. Link the Pull Request to an Issue
To link a pull request to an issue, add one of the following lines
to the **pull request description**:

- Fixes #ISSUE_NUMBER

Example:
```
Fixes #123
```

This helps reviewers understand the context of the change and allows
GitHub to automatically manage issue state.

Double check the content, then click 'ready to review' and submit!

4. Wait for Checks
We have several security and quality checks.

Please review and check they all pass.

**⚠️ WARNING** You will have to wait for some workflows to be approved by maintainers

**⚠️ WARNING** Assignment checks may fail until maintainer intervention. This is normal and you have to wait.

If checks are solving and you require help, you can:
- Contact us on [discord](discord.md)
- Attend [Office Hours](https://zoom-lfx.platform.linuxfoundation.org/meeting/99912667426?password=5b584a0e-1ed7-49d3-b2fc-dc5ddc888338) 
- Attend [Community Calls](https://zoom-lfx.platform.linuxfoundation.org/meeting/92041330205?password=2f345bee-0c14-4dd5-9883-06fbc9c60581)
- Ask for help on the pull request

5. Once approved
Once your pull request passes checks and is approved, it will shortly be merged to main.
Congratulations!