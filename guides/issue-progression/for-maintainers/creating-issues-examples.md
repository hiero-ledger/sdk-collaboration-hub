## How to Describe Issues at Each Difficulty Level

### Purpose

This guide explains how to **write issue descriptions appropriate to each difficulty level**.

Each level requires a different balance of:
- clarity  
- guidance  
- freedom  
- responsibility  

Poor issue descriptions lead to:
- confusion  
- low-quality contributions  
- increased review time  

Use [AI Prompts](guides/issue-progression/creating-issues-AI-prompts.md) to support content generation.
---

## Core Principles


### 1. Implementation Guidance

> The higher the level, the less you prescribe *how* to solve the problem.

| Level        | Description Style                          |
|--------------|--------------------------------------------|
| GFI          | Fully specified instructions               |
| Beginner     | Guided with reference patterns             |
| Intermediate | Clear outcome, minimal implementation guidance  |
| Advanced     | Problem space, solution undefined          |

---

### 2. Workflow Guidance

> The higher the level, the less you provide *workflow instructions*.

| Level        | Workflow Guidance                          |
|--------------|--------------------------------------------|
| GFI          | Fully guided (setup, steps, PR process)    |
| Beginner     | Light reminders and references             |
| Intermediate | None                                       |
| Advanced     | None                                       |

---

### 3. Testing Guidance

> The higher the level, the more then less you emphasise *testing*.

| Level        | Description Style                          |
|--------------|--------------------------------------------|
| GFI          | No testing               |
| Beginner     | Light unit testing with guides            |
| Intermediate | Extensive, multi-faceted testing guides  |
| Advanced     | No testing          |

Testing should be comprehensively understood at the advanced level.

### 3. Maintainability Expectations

> The higher the level, the more you must emphasise *maintainability expectations*.

| Level        | Quality Expectation                                                                 |
|--------------|--------------------------------------------------------------------------------------|
| GFI          | Correctness                                                                          |
| Beginner     | Correctness + alignment with existing patterns                                       |
| Intermediate | Correctness + patterns + maintainability                     |
| Advanced     | Correctness + patterns + maintainability + strong design and long-term considerations|

### 4. Quality Specification

> The higher the level, the more explicitly you must define **quality, constraints, and validation expectations**

| Level        | Quality Specification Requirement                                                |
|--------------|----------------------------------------------------------------------------------|
| GFI          | Implicit (correctness is obvious)                                                |
| Beginner     | Light (quality is inferred from reference patterns)                              |
| Intermediate | Explicit (must define validation, constraints, and testing expectations)         |
| Advanced     | Explicit at a systems level (constraints, risks, and design expectations defined)|

This guide provides **examples** to write high-quality issues at each level.


## Worked Examples


### Good First Issue (GFI)
Zero-ambiguity or decision making, pure execution:

```markdown
Hi! This is an example of a Good First Issue.

In line 42 of `README.md` there is a typo.

incorrect: enviroment
should say: environment

The issue is to correct the typo from “enviroment” → “environment”

No other changes.

You should work on a branch [Guide] and commits need to be DCO and GPG key signed [Guide]

Other helpful guides:
- Setup [Guide]
- Workflow [Guide]
- Rebasing [Guide]
- Resolving merge conflicts [Guide]
```

### Beginner
Guide the contributor to apply an existing pattern correctly.

```markdown
We have a TokenId class that does not have a string method, making it difficult for users to understand the code output.

We should add a `toString()` method to `TokenId`.

Follow the implementation used in `AccountId` for formatting and structure, it should be very similar.
Complement this with your own research into `examples/tokens/tokenId.py`. Change the method and the example and see what would work better. Create a simple unit test to verify expected behaviour.

Please remember our workflow: working on a branch, GCO and GPG key signing.

Other helpful links:
- Testing [Guide]
- AccountId [Link]
- TokenID example [Link]
- Resolving merge conflicts [Guide]
```


### Intermediate
Intermediate issues should be written to describe the solution in detail, and its quality, so the developer can decide what is an appropriate approach:


```markdown
Extend `TokenCreateTransaction` to support NFTs.

Expected outcome:
- NFT fields can be set and validated upon token creation
- Illustrative examples showing best practices
- Existing transaction behavior remains unchanged

Constraints:
- Must follow existing transaction patterns
- Must not introduce breaking API changes
- Must follow the protobuf requirements and naming

Quality expectations:
- Reuse existing validation logic where possible
- Handle invalid inputs correctly
- Easy to read, maintainable code

Testing expectations:
- Happy and unhappy paths
- Unit and integration tests (human-led)
- Future-facing to assist debug and to protect against breaking changes

Helpful Links
- Hedera Protobufs [Guide]
- JS SDK TokenCreateTransaction [Link]
- Go SDK TokenCreateTransaction [Link]
- Java SDK TokenCreateTransaction [Link]
```

## Advanced

The contributor is responsible for designing the solution and ensuring it fits the system. They just need to know the problem clearly, and any other relevant conditions.

```markdown
Implement HIP-1027 across the SDK.

Problem:
The SDK does not currently support HIP-1027 [HIP 1027 document]. 
We should implement it. The developer should consider what complementary infrastructure is required to support it and create any migration tasks.

Helpful Links
- Hedera Protobufs [Guide]
- JS SDK HIP-1027 Implementation [Link]

```