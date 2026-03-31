### AI Prompts

These are **short, copy-paste prompts** for maintainers to generate issues aligned with each difficulty level.

Each prompt enforces:
- correct level of guidance  
- appropriate scope  
- clear expectations  

*Note*: the output should be inserted inside the relevant section in the templates.

We do not recommend generating full issue descriptions with these prompts as it will lack complementary documentation and information.

---

## Good First Issue (GFI)

```text
Write a Good First Issue.

Requirements:
- Fully specify the task (no ambiguity)
- Include exact file paths and changes
- Provide step-by-step implementation instructions
- Include expected final result and validation
- No decision-making required

Structure:
- Problem
- Solution
- Implementation (step-by-step)
- Validation

Keep it short and precise.

Ensure the issue:
- Matches the intended difficulty level exactly
- Does not drift into another level
- Is concise, structured, and unambiguous
```

## Beginner Issue

```
Write a Beginner-level issue.

Requirements:
- Clearly describe the problem and desired outcome
- Reference an existing pattern in the codebase
- Do NOT give step-by-step instructions
- Include light guidance on where to look
- Keep scope small (1–3 files)

Structure:
- Problem
- Solution
- Research guidance (reference pattern)
- Implementation (high-level)
- Validation

Ensure the contributor must follow a pattern, not design a solution.

Ensure the issue:
- Matches the intended difficulty level exactly
- Does not drift into another level
- Is concise, structured, and unambiguous
```

## Intermediate Issue

```
Write an Intermediate-level issue.

Requirements:
- Clearly define the problem and expected outcome
- Do NOT prescribe implementation steps
- Explicitly define constraints (APIs, patterns, no breaking changes)
- Include clear quality expectations (tests, maintainability)
- Include validation criteria

Structure:
- Problem
- Expected outcome
- Constraints
- Research guidance
- Implementation (high-level only)
- Validation

Focus on clarity of outcome and quality, not how to implement.

Ensure the issue:
- Matches the intended difficulty level exactly
- Does not drift into another level
- Is concise, structured, and unambiguous
```

## Advanced Issue

```
Write an Advanced-level issue.

Requirements:
- Define the problem space, not the solution
- Include system context and why the problem matters
- Clearly define constraints and risks (backward compatibility, performance, security)
- Require design thinking and trade-off evaluation
- Do NOT include implementation steps

Structure:
- Problem
- Context
- Desired outcome
- Constraints
- Design expectations
- Validation

Focus on architecture, not implementation.


```