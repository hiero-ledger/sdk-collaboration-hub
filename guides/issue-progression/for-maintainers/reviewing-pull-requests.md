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

> Help contributors grow within the boundaries of their current level.

> Good reviewing helps contributors grow over time — not in a single pull request.

- Not every PR should be merged  
- Not every contributor is ready for every level  
- Clear boundaries improve contributor growth and protect project quality  

---
**Do Not**:
- Review PRs that clearly lack required skills for the level
- Continue reviewing PRs that repeatedly miss expectations
- Compensate for missing prerequisite skills

**Do**:
- Reinforce level-specific expectations
- Redirect contributors to the appropriate lower level when needed

---
**Only proceed with a full review when the PR demonstrates baseline competence for that level.**

Example:
For Advanced PRs, focus on architecture and trade-offs.
If the PR shows key gaps in testing or understanding, close it and redirect to Intermediate.


---

## Reviewing Good First Issues (GFI)

### Reviewer Focus

- correctness only  
- exact match to instructions  
- no additional changes  
- signing requirements met

---

### When to Push Back

- incorrect implementation  
- extra or unrelated changes  
- incomplete signing

---

### Reviewer Style

- inform of the issue
- point to documentation that resolves it


```
Hi ABC, please review the instructions, you have forgotten to sign your commits
```

```
Hi ABC, please limit your changes strictly to what is described in the issue
```

```
Hi ABC, I see you have signed your commits with a GPG key but note in the /commit tab they are marked as `unverified` because your GPG key is not uploaded to github. Please follow this guide on how to do so.
```
---

### When to Close
A PR should be closed when it clearly does not meet the level criteria, or would be very difficult to resolve in the current form for the new developer.

For example:

- contributor not following instructions
- contributor experiencing significant rebase or signing issues.  

```
Hi ABC, I am closing this pull request due to significant signing issues caused by regular merging from main, please try again. Create a new branch from an updated main and only use rebases [Guide].
```

### When to Unassign
A developer should be unassigned from a GFI when:

- contributor not persistently not following instructions
- contributor is submitting spam
- PR is stale for at least 7 days 

```
Hi ABC, I am closing this pull request and unassigning you from the issue as we have seen no activity here for a few days, despite asking for an update. Please do feel free to try a new issue when you have more availability. In the future, you can always inform us you no longer want to work on the issue by commenting `/unassign`
```

---

## Beginner

### Reviewer Focus

- correct outcome  
- alignment with existing patterns  
- good code quality  
- basic validation or tests included (if expected)  

---

### Reviewer Style

- guide toward patterns  
- avoid giving full solutions  
- reinforce consistency  

```
Hi ABC, your solution is functional but lacks the docstring formatting we regularly use. Please review and re-attempt
```

### When to Push Back

- deviation from established patterns  
- unclear or inconsistent implementation  
- unnecessary complexity  
- missing basic validation  


```
Hi ABC, you have created a new class abstraction to solve the issue, which adds unecessary complexity. Please keep to current standards.
```

### When to Close and Unassign
A PR should be closed and the developer unassigned when it clearly does not meet the level criteria.

For example:

- contributor experiencing signing issues
- contributor not following instructions

```
Hi ABC, I am closing this pull request and unassigning you to the issue as there are various workflow issues. Please complete another good first issue to build your workflow skills.
```

---

## Intermediate

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

### Reviewer Style

- enforce standards  
- do not provide implementation details  
- request improvements, not step-by-step fixes  

```
Hi ABC, your solution is functional and well tested. However, you have implemented custom workflow methods creating many lines of code that might become difficult to maintain. Can you use existing packages to keep it more simple and maintainable? We would like the outcome to not just be functional, but easy to understand and maintain.
```

### When to Push Back

- unclear or incorrect behavior  
- missing or weak tests  
- violation of constraints  
- poor code quality  
- “works but messy” implementations  

```
Hi ABC, I am closing this pull request and unassigning you from the issue as we believe you can benefit from spending more time securing a better understanding of the codebase and our best practices. Please do browse available beginner issues and try again. We look forward to reviewing your next pull request and helping your skills progress.
```
---

## Advanced

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

### Reviewer Style

- evaluate design before code  
- challenge assumptions  
- encourage early discussion (before full implementation)  


---
```
Hi ABC, this is a thoughtful and fairly robust implementation of HIP 1037, though could you compare to other SDKs and double-check we support similar methods and follow similar conventions?
```

### When to Push Back

- “it works” but poorly designed  
- missing design reasoning  
- introduces risk or breaking behavior  
- inconsistent with architecture  
- over-engineered or under-specified solutions  

---
```
Hi ABC, I am closing this pull request and unassigning you from the issue as this PR shows you can benefit from building a better understanding of key areas of the codebase. Please do browse available intermediate issues and try again. We look forward to reviewing your next pull request.
```
---
