# Creating or Maintaining Github Workflows

Github workflows provide automations to support activities on a repository.

This guide explains best practices for documenting, creating and maintaing github action workflows.

## Table of Contents

- [How Workflows Are Structured](#how-workflows-are-structured)
- [Workflow Documentation Guide](#workflow-documentation-guide)
- [Workflow Best Practices](#workflow-best-practices)


# How Workflows Are Structured 

Workflows often have an **Orchestration vs. Logic separation**.  
This makes it much easier for you (and future contributors) to **create**, **tweak**, **understand**, and **maintain** automations.

## Two Folders – Two Very Different Jobs

| Folder                        | File type     | Role              | Contains mostly…                                 | Thinking allowed? |
|-------------------------------|---------------|-------------------|--------------------------------------------------|-------------------|
| `.github/workflows/`          | `*.yml`       | **Orchestration** | When to run, permissions, setup, calling script  | Almost none       |
| `.github/scripts/`            | `*.js` (main) | **Business Logic**| Decisions, API calls, calculations, comments     | All of it         |

## What Each Layer Is Responsible For

### Workflows (.github/workflows/*.yml) – Orchestration

Responsible for:

- Defining **triggers** (`on: pull_request`, `on: issue_comment`, etc.)
- Setting **permissions** (`permissions: { issues: write }`)
- Selecting **runners** (`runs-on: ubuntu-latest`)
- Controlling **concurrency** (prevent duplicate runs)
- Wiring **inputs**, **environment variables**, **secrets**
- Calling the script (usually via `actions/github-script`)

Should contain **almost zero decision-making logic**.  
Complex `if:` conditions, string parsing, API calls, etc. do **not** belong here.

### Scripts (.github/scripts/*.js) – Business Logic

Responsible for:

- Interpreting the **event payload** (`context.payload`)
- Making **decisions** (“Is this a valid /assign?”, “Does the issue have the right label?”)
- Calling **GitHub APIs** (`github.rest.issues.addLabels`, `createComment`, …)
- Computing **results**
- Handling **errors** (`try/catch`, `core.setFailed`)
- Producing **logs** (`core.info`, `core.warning`)
- Generating **user-facing comments** (helpful messages, emojis, instructions)

## Naming Convention 

One approach could be to deliberately name workflows and their scripts **very similarly** so you can instantly see which files belong together:

Examples:

- `.github/workflows/bot-gfi-assign-on-comment.yml`  
  → `.github/scripts/bot-gfi-assign-on-comment.js`

This makes scanning `.github/` much faster when you want to understand or fix something.

## Workflow Documentation Guide

Well-documented workflows ensure that contributors, maintainers, and collaborators can quickly understand system.

This guide establishes best practices for documenting GitHub workflows.

### What to Document in YAML Files

Keep documentation concise and focused on **how the workflow runs**, not how it works internally.

Include:

- **Purpose**
  - What the workflow does at a high level

- **Triggers**
  - When and why the workflow runs (e.g. `push`, `pull_request`, `issue_comment`)

- **Key Actions / Packages**
  - Any important actions used (e.g. `actions/checkout`, `actions/github-script`)

- **Execution Flow**
  - High-level summary of steps (no detailed logic)

- **Environment Variables**
  - Required env variables and what they are used for

- **Source of Logic**
  - Reference to the script or file where the actual logic lives

---

### What NOT to Include

- Detailed business logic  
- Step-by-step algorithm explanations  
- Large code snippets from scripts  

---

> **Rule of thumb:**  
> YAML explains *what runs and when*.  
> Scripts explain *how it works*.

### Example: Contributor Self-Assignment Workflow

This example shows a well-documented GitHub Actions workflow (`.github/workflows/bot-gfi-assign-on-comment.yml`):

```
# Automatically assigns a contributor when they comment "/assign" on an issue.
# Only applies to issues (not pull requests).
# Logic is implemented in: .github/scripts/bot-gfi-assign-on-comment.js

name: GFI Assign on /assign

on:
  issue_comment: # Trigger when a new comment is created on an issue
    types:
      - created

permissions:
  issues: write   # Required to assign users and comment on issues
  contents: read  # Required to read the script file

jobs:
  gfi-assign:
    # Ensure this runs only for issues, not pull requests
    if: github.event.issue.pull_request == null
    runs-on: ubuntu-latest

    # Prevent race conditions if multiple users comment at the same time
    concurrency:
      group: gfi-assign-${{ github.event.issue.number }}
      cancel-in-progress: false

    steps:
      - name: Checkout repository
        # Required to access the assignment script in the repo
        uses: actions/checkout@v6.0.1

      - name: Run GFI /assign handler
        # Executes the custom logic for handling "/assign" comments
        uses: actions/github-script@v8.0.0
        with:
          script: |
            const script = require('./.github/scripts/bot-gfi-assign-on-comment.js'); 
            await script({ github, context });
```

## Script File Documentation

Script files (`.github/scripts/*`) contain the actual logic and business rules and are usually written in JavaScript as is a popular language. Documentation here should be comprehensive.

### What to Document in Script Files

- **Purpose** - What problem does this script solve?
- **Called By** - Which workflow(s) execute this script?
- **Major Rules** - What are the critical constraints and validation logic?
- **Dependencies** - External packages or APIs used
- **Related Docs** - Links to associated documentation

### Example: GFI Self-Assignment Script

This example shows snippets of a well-documented script file (`.github/scripts/bot-gfi-assign-on-comment.js`):

```javascript
// PURPOSE
// -------
// Handles contributor self-assignment logic when "/assign" is commented.
// Validates prerequisites and enforces assignment rules for Good First Issues.
//
// CALLED BY
// ---------
// Workflow: .github/workflows/bot-gfi-assign-on-comment.yml
//
// MAJOR RULES
// -----------
// 1. Only allow assignment to Good First Issues
// 2. Never override an existing assignment
// 3. Check user is not on spam list
// 4. Enforce max 2 open assigned issues per user
// 5. Post helpful comment with unassigned GFI link on success
//
// DEPENDENCIES
// ------------
// - @actions/github for GitHub API access
// - @actions/core for logging
//
// RELATED DOCS
// ------------
// - Issue Guidelines: docs/maintainers/good_first_issues_guidelines.md
// - Contributor Workflow: docs/sdk_developers/workflow.md

module.exports = async ({ github, context }) => {
  // Implementation...
};
```

## Write Good Docstrings

Docstrings should explain intent, assumptions, side effects, and edge cases. This ensures maintainers understand not just what the code does, but why and when it applies.

Automation scripts in `.github/scripts/` are written in JavaScript. Here are examples of good documentation:

### Worse Example: Unclear Intent

```javascript
/**
 * Gets skill level
 */
function getSkillLevel(issueLabels) {
  // implementation...
}
```

### Better Example: Clear Intent and Assumptions

```javascript
/**
 * Determines the contributor difficulty tier from issue labels.
 * 
 * Assumes exactly one skill label should be present on the issue.
 * If multiple skill labels exist, the first match in priority order is returned.
 * Returns null if no skill label is found, which triggers maintainer escalation
 * notifications.
 * 
 * @param {string[]} issueLabels - List of GitHub label strings on the issue
 * @returns {string|null} Skill level identifier (Good First Issue, Beginner, 
 *                        Intermediate, Advanced) or null if no skill label found
 * 
 */
function determineContributorSkillLevelFromLabels(issueLabels) {
  const skillLabels = ['Good First Issue', 'Beginner', 'Intermediate', 'Advanced'];
  
  for (const skillLabel of skillLabels) {
    if (issueLabels.includes(skillLabel)) {
      return skillLabel;
    }
  }
  
  return null;
}
```

---

## Make Exit Reasons Obvious

When code exits early or returns a specific state, the reason should be immediately clear. Use clear comments to explain why the function returns or terminates.

Example:

```javascript
/*
 * Safety gate: never override an existing assignment.
 * If someone is already assigned, this workflow must not change it.
 */
if (issue.assignees.length > 0) {
  await github.rest.issues.createComment({
    owner: context.repo.owner,
    repo: context.repo.repo,
    issue_number: issue.number,
    body: `Cannot self-assign: issue is already assigned to @${issue.assignees[0].login}`
  });
  return;
}
```

## Name Things Clearly

Use descriptive names that reveal intent. Function names should be action-oriented and specific.

### Avoid:

```javascript
async function fetch(contributorId) {
}
```

### Better:

```javascript
// Better: Clear, Action-Oriented Names
async function fetchContributorSkillLevelFromDatabase(contributorId) {
  // Retrieve a contributor's verified skill tier
}
```

# Workflow Best Practices

This guide establishes best practices for writing and maintaining GitHub Actions workflows and their companion scripts.

Workflow bugs can:

- Mass-assign or unassign contributors
- Spam dozens of users with incorrect comments
- Leak permissions or secrets
- Create irreversible state changes
- Break the entire contributor experience

A single mistake can create hours of cleanup. These best practices exist to ensure our workflows are **safe**, **predictable**, **debuggable**, and **maintainable at scale**.

---

## 1. Separate the Workflow from the Logic

Separate the major orchestration from the logic:

| Layer | Folder | Responsibility |
| :--- | :--- | :--- |
| **Workflow** (`.yml`) | `.github/workflows/` | Triggers, permissions, env setup, calling scripts |
| **Script** (`.js`) | `.github/scripts/` | Decisions, API calls, error handling, comments |

Avoid adding logic directly to the workflow YAML. Inline logic is brittle —
formatting is harder, some variables can become malformed, and it is difficult to
test.

### ❌ Bad: Logic in YAML

```yaml
- run: |
    ASSIGNEES=$(gh api /repos/$REPO/issues/$NUM/assignees | jq -r '.[].login')
    if [ -z "$ASSIGNEES" ]; then
      gh issue assign $NUM --assignee $USER
    fi
```

### ✅ Good: Logic in a script

```yaml
- uses: actions/github-script@ed597411d8f924073f98dfc5c65a23a2325f34cd # v8.0.0
  with:
    script: |
      const script = require('./.github/scripts/bot-assign.js');
      await script({ github, context, core });
```

---

## 2. Avoid Hardcoding — Use Environment Variables

Ideally, environment variables should be **defined in the workflow YAML
and passed to the script. Environment variables should never be hardcoded.

### ❌ Bad: Hardcoded in the script

```javascript
if (team === '@hiero-ledger/core-maintainers') {
  // ...
}
```

### ✅ Good: Passed from the workflow

**Workflow YAML:**

```yaml
env:
  MAINTAINER_TEAM: '@hiero-ledger/core-maintainers'
  MAX_ASSIGNEES: '3'
  INACTIVITY_DAYS: '21'
```

This improves maintainability (one place to change a value) and security (secrets
stay in the orchestration layer).

---

## 3. Secrets Handling

Secrets are the most sensitive part of any workflow. Follow these rules strictly:

| Rule | Why |
| :--- | :--- |
| **Never log secrets** | Even partial logging can expose tokens |
| **Never transform secrets** | String operations on secrets can leak them via error messages |
| **Never pass secrets unless required** | Minimize the blast radius of a compromise |
| **Use `${{ secrets.NAME }}`** | Always reference secrets through GitHub's secrets mechanism |

### ❌ Bad

```yaml
- run: echo "Token is ${{ secrets.GITHUB_TOKEN }}"
- run: |
    TOKEN="${{ secrets.MY_TOKEN }}"
    TRIMMED=${TOKEN:0:10}
```

### ✅ Good

```yaml
steps:
  - run: gh api /repos/...  # gh CLI reads GITHUB_TOKEN from env
    env:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

## 4. Permissions — Default to Read, Escalate Only When Required

Every workflow must explicitly declare permissions, scoped to the **minimum**
required.

For example:
```yaml
permissions:
  issues: write        # can it read or write to issues and pull requests?
  contents: read       # can it read or write to the code?
  pull-requests: read  # can it read or write to the pull requests?
```

**Never** give a workflow excessive permissions or token access. The higher the permissions granted, the more hardening required.

---

## 5. Log for Debugging and Maintainability

Use logs to make it easy to debug. Logs should show:

- What triggered execution
- What decisions were made
- Why branches were taken
- What API calls occurred
- Why exits happened

#### ❌ Bad

```javascript
console.log('assigned');
console.log('done');
```

#### ✅ Good

```javascript
console.log('[assign-bot] Issue #42 already assigned:', assignees);
console.log('[assign-bot] Exit: user has not completed a Good First Issue');
console.log('[assign-bot] Assigned user @alice to issue #42');
```

### Always Log Exits

Include logs even when exiting early, so you know why the workflow stopped and what
conditions caused it:

```javascript
if (!commentRequestsAssignment(body)) {
  console.log('[assign-bot] Exit: not an /assign request');
  return;
}
```

### What is Acceptable to Log

| ✅ Safe to log | ❌ Never log |
| :--- | :--- |
| Issue numbers | Tokens or secrets |
| Usernames | API keys |
| Label names | Private user data |
| Counts and IDs | Full event payloads |

---

## 6. Error Handling

Automation must fail **predictably** and **helpfully**. Errors fall into three
categories:

### a) Expected Failures

The user did not meet conditions (e.g., eligibility requirements). These often
warrant a helpful comment.

```javascript
if (!hasCompletedGFI) {
  await postComment(
    `Sorry @${user}, we were unable to assign you. ` +
    `You must first complete a Good First Issue.`
  );
  return;
}
```

### b) Operational Failures

The GitHub API malfunctioned or something unexpected happened. Log the error and
optionally notify the user.

```javascript
try {
  await github.rest.issues.addAssignees({ owner, repo, issue_number, assignees });
} catch (error) {
  console.error('[assign-bot] API error:', error.message);
  // Optionally comment to the user
}
```

### c) System Failures

When the workflow cannot safely proceed, tag maintainers for assistance:

```javascript
await postComment(
  `Sorry @${user}, we were unable to assign you due to a technical issue. ` +
  `Requesting @hiero-ledger/maintainers assistance.`
);
core.setFailed('System failure: ' + error.message);
```

**Never exit without logging the exit reason.** Every `return` should have a
corresponding log line so unexpected behavior can be debugged.

---

## 7. Notify Users

### When to Notify

Notify the user when:

- They attempted an action (e.g., `/assign`)
- Eligibility checks failed
- Prerequisites are missing
- They are waiting for a result

Scripts should generate an **informative comment** and post it. Silent failures
leave contributors confused.

### When to Tag Maintainers

Tag maintainers (`@hiero-ledger/maintainers`) when automation cannot safely decide:

- API unavailable or returning unexpected responses
- Ambiguous labels or state
- Permission failures
- Edge cases the script was not designed to handle

---

## 8. Safety Rules

### Pin Action Versions

All third-party actions must be pinned to a **full commit SHA**, not a floating tag.

```yaml
# ❌ Bad — floating tag
uses: actions/checkout@v4

# ✅ Good — pinned SHA with version comment
uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683 # v4.2.2
```

#### Security and supply-chain risk

GitHub Actions referenced using tags such as `latest`, `v1`, or `v4` are **mutable**. If a package is compromised, workflows that rely on floating tags may unknowingly execute untrusted code. 

By pinning a GitHub Action to a specific commit SHA, the exact code being executed is known and cannot change unexpectedly. This makes workflows more secure, auditable, and resistant to supply-chain attacks.

---

#### Step-by-step: How to find the correct commit SHA

##### Step 1: Open the action’s GitHub repository

Start with the official GitHub repository for the action. Prefer repositories linked from the GitHub Marketplace and ensure the project is actively maintained.

Examples:
- actions/checkout
- step-security/harden-runner

##### Step 2: Go to the Releases page

In the repository, navigate to **Releases** and open the **Latest** release.

##### Step 3: Open the release tag

Click the release version (for example `v2.14.0`) to open the release details page.

##### Step 4: Copy the commit SHA

From the release page, open the commit associated with the release and copy the full 40-character commit SHA.

Example SHA: 20cf305ff2072d973412fa9b1e3a4f227bda3c76

##### Step 5: Update the workflow

Replace any floating or version-based reference:

```yaml
uses: step-security/harden-runner@v2
uses: step-security/harden-runner@20cf305ff2072d973412fa9b1e3a4f227bda3c76 # v2.14.0
```

## 9. Do Not Trust User Input

Anything coming from issue titles, comments, labels, PR bodies, or usernames should
be validated before use, moreso, the greater permissions you enable.

#### ❌ Bad

```javascript
body.includes('/assign');
```

#### ✅ Good

```javascript
/^\s*\/assign\s*$/i.test(body);

function isSafeSearchToken(value) {
  return /^[a-zA-Z0-9._/-]+$/.test(value);
}
```

### Avoid `pull_request_target` with Untrusted Code

`pull_request_target` runs with **write permissions on the base repository**, even
for PRs from forks. Never check out or execute code from the fork branch inside
a `pull_request_target` workflow — this allows a contributor to run arbitrary
code with your repository's write token.

```yaml
# ❌ Dangerous — executes fork code with write token
on: pull_request_target
steps:
  - uses: actions/checkout@...
    with:
      ref: ${{ github.event.pull_request.head.sha }}  # attacker-controlled
  - run: npm install && npm test  # runs attacker code
```

If you must use `pull_request_target` (e.g., to post comments on fork PRs),
keep the workflow free of any checkout or execution of PR code, and scope
permissions to the minimum required.

## 10. Concurrency

Workflows might be triggered multiple times simultaneously. Use concurrency controls
to prevent race conditions:

```yaml
concurrency:
  group: assign-${{ github.event.issue.number }}
  cancel-in-progress: false
```

Use `cancel-in-progress: false` for workflows that mutate state (assigning users,
posting comments) to avoid partial execution. Use `cancel-in-progress: true` for
read-only checks (linting, testing) where only the latest run matters.

---

## 11. Test Before Enabling  Actions

No workflow change is considered safe without testing. Because workflow bugs can
mass-edit issues, spam contributors, or leak permissions, **maintainers expect proof
of testing in PRs**.

There are several ways to test actions (dry-runs, local scripts and fork runs). Sometimes, all forms of testing are strongly desirable.
---

### Dry-Run Techniques

Dry runs let you verify logic without performing real mutations. They divert actions
to log instead of execute.

**Workflow YAML:**

```yaml
on:
  workflow_dispatch:
    inputs:
      dry_run:
        description: 'Run in dry-run mode'
        type: boolean
        default: true

env:
  DRY_RUN: ${{ github.event.inputs.dry_run }}
```

**Script:**

```javascript
if (process.env.DRY_RUN === 'true') {
  console.log('[dry-run] Would assign:', username);
  return;
}

await github.rest.issues.addAssignees({ owner, repo, issue_number, assignees: [username] });
```

If dry-run is used, it should be:

- **Clear** — obvious from logs that it is active
- **Well-logged** — every skipped mutation should be logged
- **Default to true** on `workflow_dispatch`
- **Easy to maintain** — simple toggle, not complex branching

### Local Testing

Large parts of behavior can be validated locally without pushing commits:

1. Simulate the GitHub event payload (or extract from a real event)
2. Call your script directly and observe output
3. Verify decision-making, formatting, and branching

**What local testing cannot prove:**

| Cannot validate locally | Requires fork testing |
| :--- | :--- |
| GitHub permissions | ✅ |
| Actual API authorization | ✅ |
| Concurrency behavior | ✅ |
| Runner environment | ✅ |

---

### Fork Testing (Strongly Recommended)

Use your personal fork to validate behavior safely. You can trigger events, create
test issues, simulate comments, and inspect logs without risk to the main repository.

By default, GitHub Actions are often disabled on forks to save resources. You must enable them manually:

1.  Go to your fork on GitHub (e.g., `github.com/<your-username>/repository-name`).
2.  Click on the **Settings** tab.
3.  On the left sidebar, click **Actions** > **General**.
4.  Select **Allow all actions and reusable workflows**.
5.  Click **Save**.

#### The Testing Workflow

Testing a bot or action usually involves two distinct parts:
1.  **The Logic:** The code that runs the bot (e.g., the `.js` script or `.yml` workflow).
2.  **The Trigger:** An event that causes the bot to run (e.g., a new PR, a comment, or a specific time of day).

#### Step 1: Update your Fork's Main Branch
To test your new bot logic:

1.  Create a branch with your changes (e.g., `feat/new-bot-logic`).
2.  Open a Pull Request **targeting your fork's main branch** (Base: `<your-username>:main` ← Compare: `<your-username>:feat/new-bot-logic`).
4.  **Merge** this PR into your fork's `main`.

> **Note:** Your fork's `main` now contains the code that *runs* the tests.

#### Step 2: Create a Test Scenario
Now that your fork has the updated logic, you need to create an event to trigger it.

1.  Create a new dummy branch (e.g., `test/trigger-bot`).
2.  Make the necessary changes to trigger the specific action (see examples below).
3.  Open a Pull Request **within your fork** (Base: `<your-username>:main` ← Compare: `<your-username>:test/trigger-bot`).

---

#### Modifying Test Timescales

Real-world conditions (like "21 days of inactivity") are impractical for testing. You should temporarily modify the code in your feature branch (Step 1) to simulate these conditions immediately.

#### 1. Shortening Time Thresholds
If a bot waits for **21 days**, change the variable to **0 days** or **minutes** in your script.

**Before (Production Code):**
```bash
# Wait for 21 days before marking as stale
DAYS="${DAYS:-21}"
```

**After (Testing Code):**
```bash
# Set to 0 to treat everything as immediately stale for testing
DAYS="${DAYS:-0}" 
```

#### 2. accelerating Cron Schedules
If a workflow runs once a day, you don't want to wait 24 hours. Modify the `.yml` file to run frequently or allow manual triggers.

**Before:**
```yaml
on:
  schedule:
    - cron: "0 12 * * *"  # Runs at 12:00 PM daily
```

**After:**
```yaml
on:
  workflow_dispatch:      # Allows manual button click in Actions tab
  schedule:
    - cron: "*/5 * * * *" # Runs every 5 minutes
```

---

#### Real World Examples

#### Example 1: Testing an Unverified Commit Bot
**Goal:** Ensure the bot posts a warning comment if a PR contains unsigned commits.

1.  **Deploy Logic:** Merge the workflow file that checks for GPG signatures into your fork's `main`.
2.  **Trigger Scenario:**
    *   Create a new branch: `git checkout -b test/unsigned-commit`
    *   Make a dummy change.
    *   Commit **without** your GPG signature using the `--no-gpg-sign` flag:
        ```bash
        git commit -m "test: unsigned commit" --no-gpg-sign
        ```
    *   Push and open a PR to your fork's `main`.
3.  **Verify:** Wait a moment and check the PR timeline. The bot should post a comment or fail the check indicating the commit is unverified.

#### Example 2: Testing the Inactivity Bot
**Goal:** Verify the bot unassigns users after a period of inactivity.

1.  **Modify Logic:** In your script (Step 1), set `DAYS=0` so the bot considers any issue created "now" as stale.
2.  **Deploy:** Merge this change to your fork's `main`.
3.  **Trigger Scenario:**
    *   Create a dummy Issue in your fork.
    *   Assign yourself to the Issue.
    *   Wait for the scheduled workflow to run (e.g., the 5-minute cron you set up) or manually trigger it via the Actions tab.
4.  **Verify:** Check if the bot posted a comment on the issue and removed you from the assignee list.

#### Cleanup

Once you have verified the functionality works as expected:

1.  Delete your test branches (`test/trigger-bot`, etc.).
2.  Close any dummy Pull Requests and Issues in your fork.
3.  **Revert the timescale changes** in your feature branch (e.g., change `DAYS=0` back to `DAYS=21`, remove the `*/5` cron).
4.  Create a final Pull Request from your clean feature branch to the official upstream repository.



