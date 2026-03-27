## Productive PR Reviewing by Difficulty Level

### Purpose

This guide defines how to **review pull requests effectively at each difficulty level**, while supporting contributor progression in a sustainable way.

It ensures:
- consistent review standards  
- reduced review time  
- clear expectations for contributors  
- long-term contributor growth  

---

## Progression Principle

> Help contributors grow — but only within the boundaries of the current level and in a sustainable way.

- provide informative, actionable feedback  
- Do not over-teach beyond the level  
- Do not compensate for missing prerequisites  
- suggest the next level when appropriate  

Instead:
- provide targeted feedback  
- reinforce expectations  
- guide contributors toward the next or below level  

Redirect a contributor if:

- they require guidance beyond the level  
- they repeatedly miss expectations  
- the PR would require significant rework  

---

### Progression Path

- GFI → learn workflow  
- Beginner → learn patterns  
- Intermediate → make decisions  
- Advanced → design systems  

Each level builds on the previous one. Feedback should change by level, we assume the pre-requisite skills are understood. If that is not the case, we recommend diverting back.

---

## Final Rule

> Good reviewing helps contributors grow over time — not in a single pull request.

- Not every PR should be merged  
- Not every contributor is ready for every level  
- Clear boundaries improve contributor growth and protect project quality  

# Good First Issue (GFI)

### Reviewer Focus

- correctness only  
- exact match to instructions  

---

### What Good Looks Like

- change matches instructions exactly  
- correct file and location  
- no additional changes  
- signing requirements met

---

### When to Push Back

- incorrect implementation  
- extra or unrelated changes  
- incomplete signing

---

### Standard Responses

**Incorrect implementation**
> The change does not match the instructions.  
> Please follow the steps exactly as described in the issue.

---

**Extra changes**
> Please limit your changes strictly to what is described in the issue.

---

**Incomplete Signing**
> Your commits are not being marked as verified as you need to upload your GPG key to github as per this document

---

### Reviewer Suggestions

- inform of the issue
- point to documentation that resolves it

```
Hi ABC, I see you have signed your commits with a GPG key but note in the /commit tab they are marked as `unverified` because your GPG key is not uploaded to github. Please follow this guide on how to do so.
```
---

### When to Close

- repeated incorrect submissions  
- contributor not following instructions  
- issue is no longer active after 7 days

```
Hi ABC, I am closing this pull request and unassigning you from the issue as we have seen no activity here for a few days, despite suggesting changes. Please do feel free to try a new issue when you have more availability. In the future, you can always inform us you no longer want to work on the issue by commenting `/unassign`
```

---

# Beginner

### Reviewer Focus

- correct outcome  
- alignment with existing patterns  
- basic code quality  

---

### What Good Looks Like

- solution follows existing patterns  
- implementation is consistent with similar code  
- clear and readable code  
- basic validation or tests included (if expected)  

---

### When to Push Back

- deviation from established patterns  
- unclear or inconsistent implementation  
- unnecessary complexity  
- missing basic validation  

```
Hi ABC, I notice your solution is functional, congratulations! We have a string method. Though it is lacking the docstrings. Additionally, the formatting of the types differs significantly to the reference example provided. Please review and re-attempt
```
---

### Standard Responses

**Wrong approach**
> Please review the referenced implementation and align your solution with that pattern.

---

**Inconsistent implementation**
> This does not fully match existing patterns in the codebase.  
> Please align your implementation with similar examples.

---

**Scope creep**
> Please keep the scope limited to the issue unless discussed beforehand.

---

### Reviewer Behavior

- guide toward patterns  
- avoid giving full solutions  
- reinforce consistency  

---

### When to Redirect

- contributor cannot follow patterns  
- requires step-by-step guidance  

> Recommend returning to Good First Issues  

```
Hi ABC, I am closing this pull request and unassigning you from the issue as we believe you can benefit from spending more time securing stronger foundations. Please do browse available good first issues and try again. We look forward to reviewing your next pull request and helping your skills progress safely.
```

---

# Intermediate

### Reviewer Focus

- correctness  
- adherence to constraints  
- code quality and maintainability  
- test quality  

---

### What Good Looks Like

- meets the defined expected outcome  
- respects all constraints (no breaking changes)  
- aligns with existing patterns  
- clear, maintainable, readable code  
- includes meaningful tests  
- handles edge cases  

---

### When to Push Back

- unclear or incorrect behavior  
- missing or weak tests  
- violation of constraints  
- poor code quality  
- “works but messy” implementations  

```
Hi ABC, you have implemented custom workflow methods creating many lines of code that might become difficult to maintain. Can you use existing packages to keep it more simple and maintainable? We would like the outcome to not just be functional, but easy to understand and maintain.
```
---

### Standard Responses

**Incorrect approach**
> This approach does not meet the constraints defined in the issue.  
> Please revisit the expected outcome and adjust your implementation.

---

**Missing tests**
> This change requires meaningful tests covering expected behavior and edge cases.

---

**Low-quality implementation**
> While functional, this implementation does not meet the required quality standards.  
> Please improve maintainability and alignment with existing patterns.

---

**Breaking changes**
> This introduces behavior changes that are not allowed.  
> Please ensure backward compatibility.

---

### Reviewer Behavior

- enforce standards  
- do not provide implementation details  
- request improvements, not step-by-step fixes  

---

### When to Reject

- ignores constraints  
- lacks proper testing  
- requires excessive guidance  
- poor code quality  

> Recommend returning to Beginner issues
```
Hi ABC, I am closing this pull request and unassigning you from the issue as we believe you can benefit from spending more time securing a better understanding of the codebase and our best practices. Please do browse available beginner issues and try again. We look forward to reviewing your next pull request and helping your skills progress safely.
```
---

# Advanced

### Reviewer Focus

- design correctness  
- architectural alignment  
- risk awareness  
- long-term maintainability  

---

### What Good Looks Like

- design is well-reasoned and justified  
- aligns with existing architecture  
- avoids unnecessary complexity  
- preserves backward compatibility  
- addresses edge cases and failure modes  
- includes comprehensive testing  
- production-quality implementation  

---

### When to Push Back

- “it works” but poorly designed  
- missing design reasoning  
- introduces risk or breaking behavior  
- inconsistent with architecture  
- over-engineered or under-specified solutions  

```
Hi ABC, this is a thoughtful and fairly robust implementation of HIP 1037, though could you compare to other SDKs and double-check we support similar methods and follow similar conventions?
```
---

### Standard Responses

**Poor design**
> This implementation works, but does not align with the system architecture.  
> Please revisit the design and justify your approach.

---

**Missing reasoning**
> Please provide reasoning for your design decisions and any trade-offs considered.

---

**Risk introduced**
> This change introduces potential risk or instability.  
> Please revise to ensure safety and backward compatibility.

---

**Insufficient quality**
> This PR does not meet the expected quality for this level.  
> Please improve design, testing, and maintainability.

---

### Reviewer Behavior

- evaluate design before code  
- challenge assumptions  
- encourage early discussion (before full implementation)  
- reject early if not at level  

---

### When to Reject Immediately

- no design reasoning  
- unsafe or risky changes  
- AI-generated code without understanding  
- requires fundamental redesign via review  

> Recommend returning to Intermediate issues
```
Hi ABC, I am closing this pull request and unassigning you from the issue as we believe you can benefit from spending more time securing more practice at implementing narrower scoped issues with clearer outcomes. Please do browse available intermediate issues and try again. We look forward to reviewing your next pull request and helping your skills progress safely.
```
---
