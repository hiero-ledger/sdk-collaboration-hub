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


### 3. Quality Expectations

> The higher the level, the more you must emphasise *quality and maintainability expectations*.

| Level        | Quality Expectation                                                                 |
|--------------|--------------------------------------------------------------------------------------|
| GFI          | Correctness                                                                          |
| Beginner     | Correctness + alignment with existing patterns                                       |
| Intermediate | Correctness + patterns + clear, maintainable, well-tested code                       |
| Advanced     | Correctness + patterns + maintainability + strong design and long-term considerations|

### 4. Quality Specification

> The higher the level, the more explicitly you must define **quality, constraints, and validation expectations**

| Level        | Quality Specification Requirement                                                |
|--------------|----------------------------------------------------------------------------------|
| GFI          | Implicit (correctness is obvious)                                                |
| Beginner     | Light (quality is inferred from reference patterns)                              |
| Intermediate | Explicit (must define validation, constraints, and testing expectations)         |
| Advanced     | Explicit at a systems level (constraints, risks, and design expectations defined)|

This rest of this guide provides **structured prompts maintainers can use** and **examples to refer to** to write high-quality problems, solutions, context or implementations at each level.


# Good First Issue (GFI)

## Goal

Eliminate all ambiguity. The contributor should only execute the task as described.

A Good First Issue should require:
- no decision-making  
- no codebase exploration  
- no interpretation  

---

## Prompts

### Problem Description

- What exactly is wrong or needs to change?
- Where is the problem located (file path + line number)?
- What is the current incorrect state?

---

### Solution

- What exact change should be made?
- What should the final result look like (exact wording, value, or structure)?

---

### Implementation

- What are the exact steps to perform?
- What file(s) should be modified?
- What should NOT be changed?

---

### Validation

- How can the contributor confirm success?
- What should they see after making the change?
- What checks (if any) should pass?

---

## Quality Specification

At the GFI level, quality is implicit and minimal.

- The change must be correct  
- The change must match the instructions exactly  
- No additional improvements or refactoring should be made  

---

## Common Mistakes

- leaving ambiguity in the instructions  
- not specifying exact file locations  
- requiring interpretation or decision-making  
- including hidden expectations (e.g. formatting, refactoring not mentioned)  

---

## Example Structure

```markdown
File: `README.md`, line 42

Change “enviroment” → “environment”

No other changes.


# Beginner

## Goal

Guide the contributor to apply an existing pattern correctly.

A Beginner issue should require:
- understanding a small part of the codebase  
- identifying and following an existing pattern  
- making small, local decisions  

It should not require:
- designing new solutions  
- exploring large parts of the system  
- making architectural decisions  

---

## Prompts

### Problem Description

- What is missing or incorrect?
- Where is it located (file or module)?
- Why does this matter?

---

### Solution

- What should be added or changed?
- What similar implementation should be followed?

---

### Research Guidance

- Which file/class shows the correct pattern?
- What existing implementation should be used as a reference?

---

### Implementation

- What should be built or modified?
- What constraints must be respected?
- What parts of the codebase are relevant?

---

### Validation

- What behavior should exist after completion?
- What existing behavior must remain unchanged?
- Are there tests or examples that should pass?

---

## Quality Specification

At the Beginner level, quality is partially inferred from existing patterns.

- The solution must follow existing code patterns and conventions  
- The change must be consistent with similar implementations  
- The code should be clear and readable  
- Basic validation or tests may be expected  

---

## Common Mistakes

- not providing a reference pattern  
- requiring too much exploration of the codebase  
- leaving the expected outcome unclear  
- introducing tasks that require design decisions  

---

## Example Structure

```markdown
Add a `toString()` method to `TokenId`.

Follow the implementation used in `AccountId` for formatting and structure.
Create a simple unit test to verify expected behaviour and add to the TokenId example.
```

# Intermediate

## Goal

Define a clear outcome and quality bar. The contributor is responsible for choosing how to implement it.

An Intermediate issue should require:
- selecting an appropriate approach  
- evaluating trade-offs  
- working across multiple components  
- ensuring correctness, quality, and completeness  

It should not require:
- designing new architecture from scratch  
- redefining core abstractions  

---

## Prompts

### Problem Description

- What is the problem or limitation?
- Where does it occur (files/modules)?
- Why does this matter?

---

### Expected Outcome (CRITICAL)

- What should the system be able to do after this change?
- What inputs/outputs should exist?
- What behavior must be preserved?

---

### Constraints

- What must NOT change? (e.g. APIs, behavior)
- What patterns must be followed?
- Are there compatibility, performance, or security constraints?

---

### Research Guidance

- Which parts of the codebase are relevant?
- Are there similar features or related implementations?

---

### Implementation

- What should be built or extended (high-level only)?
- What components are likely involved?

---

### Validation

- How should correctness be verified?
- What tests should be written?
- What scenarios must pass?

---

## Quality Specification

At the Intermediate level, quality must be explicitly defined.

- The solution must follow existing patterns and conventions  
- The code must be clear, maintainable, and readable  
- Edge cases must be handled appropriately  
- Tests must be included and meaningful  
- The solution must not introduce breaking changes  

---

## Common Mistakes

- providing step-by-step implementation (reduces to Beginner)  
- not defining the expected outcome clearly  
- omitting constraints (leading to incorrect implementations)  
- not specifying testing or validation expectations  

---

## Example Structure

```markdown
Extend `TokenCreateTransaction` to support NFTs.

Expected outcome:
- NFT fields can be set and validated
- Existing transaction behavior remains unchanged

Constraints:
- Must follow existing transaction patterns
- Must not introduce breaking API changes

Quality expectations:
- Reuse existing validation logic where possible
- Handle invalid inputs correctly
- Include unit tests for NFT scenarios
- Easy to read, maintainable code

Validation:
- Existing tests pass
- New tests verify NFT behavior
```

# Advanced

## Goal

Define the problem space, constraints, and expectations.  
The contributor is responsible for designing the solution and ensuring it fits the system.

An Advanced issue should require:
- architectural thinking  
- evaluating multiple approaches  
- understanding system-wide impact  
- making and justifying design decisions  

It should not:
- prescribe a specific implementation  
- reduce the problem to a step-by-step task  

---

## Prompts

### Problem Description

- What is the current limitation, gap, or design issue?
- Why is the current approach insufficient?
- What parts of the system are affected?

---

### Context

- How does this relate to existing architecture?
- What assumptions or constraints already exist?
- Are there related components, patterns, or prior decisions?

---

### Desired Outcome

- What high-level capability or improvement should exist after completion?
- What problems should be resolved?

---

### Constraints (CRITICAL)

- What must remain unchanged? (APIs, compatibility, behavior)
- What risks must be avoided? (security, performance, regressions)
- What areas must NOT be modified?

---

### Design Expectations (CRITICAL)

- Should multiple approaches be considered?
- Are trade-offs expected to be evaluated?
- Should the contributor propose a design before implementation?

---

### Validation

- How should correctness be demonstrated?
- What level of testing is required?
- What evidence is expected? (tests, examples, benchmarks)

---

## Quality Specification

At the Advanced level, quality must be defined at a system level.

- The solution must align with existing architecture (or justify deviation)  
- The design must be maintainable and scalable  
- Backward compatibility must be preserved unless explicitly approved  
- Edge cases, failure modes, and risks must be addressed  
- Tests must be comprehensive and meaningful  
- The implementation must be production-grade, not just functional  

---

## Common Mistakes

- defining the solution instead of the problem  
- under-specifying constraints or risks  
- allowing breaking changes without explicit approval  
- accepting “it works” solutions without design justification  
- not requiring sufficient testing or validation  

---

## Example Structure

```markdown
Implement HIP-1027 across the SDK.

Problem:
The SDK does not currently support the full specification, limiting interoperability.

Desired outcome:
Full support for HIP-1027 aligned with existing abstractions.

Constraints:
- Maintain backward compatibility
- Avoid breaking public APIs

Design expectations:
- Evaluate multiple approaches before implementation
- Ensure alignment with existing architecture

Quality expectations:
- Clean, maintainable design
- Comprehensive test coverage
- Consistent with SDK patterns

Validation:
- All existing tests pass
- New tests cover all new behavior
```