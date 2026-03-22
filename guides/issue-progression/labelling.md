## How To Label At Each Level

### Purpose

This document provides concrete, practical guidance on how to label issues at difficulty each level.

---

## Overview

| Level        | Responsibility Focus                              | Key Trait                          |
|--------------|--------------------------------------------------|-----------------------------------|
| GFI          | Execute instructions                             | No decision-making                |
| Beginner     | Apply existing patterns                          | Follow a known approach           |
| Intermediate | Choose and implement an approach                 | Evaluate options                  |
| Advanced     | Define and design the solution                   | Own the problem and solution      |

---

## Good First Issue (GFI)

### What It Looks Like

- fully specified task  
- exact instructions provided  
- no ambiguity  
- isolated change  

The contributor should not need to:
- explore the codebase  
- research
- interpret requirements  

---

### Good First Issue Candidates:
- Typo and grammar fixes
- Light file renames or movements
- Specified type fixes
- Specified package updates
- URL fixes

### Good First Issue: Good Fit

```markdown
✅ Fix a typo in `README.md`: change “enviroment” to “environment”
```

### Good First Issue: Too Difficult

```markdown
❌Add a `toString()` method to `TokenId`, following the implementation used in `AccountId`
```
 
**Note:** use `Good First Issue Candidate` instead or append `awaiting triage` label if in doubt.

## Beginner

### What It Looks Like

- clear outcome is defined  
- a reference pattern exists in the codebase  
- small, localized change (typically 1–3 files)  
- limited ambiguity  

The contributor should:
- identify an existing pattern  
- understand how it works  
- apply it consistently  

The contributor should not need to:
- design a new solution  
- explore large parts of the system  
- make architectural decisions  

---

### Beginner Issue Candidates

- adding a method following an existing class pattern  
- adding validation using an existing validation approach  
- refactoring small duplicated logic into a helper  
- improving error messages using existing style  
- adding unit tests following existing test patterns  

---

### Beginner: Good Fit

```markdown
✅ Add a `toString()` method to `TokenId`, following the implementation used in `AccountId`
```

```
✅ Add validation to `createTransaction` to ensure `amount` is not negative, following the pattern in `transferTransaction`
```

```
✅ Refactor duplicated logic in `transaction_builder.py` (lines 45–70 and 120–150) into a helper function, following existing helper patterns
```

### Beginner: Too Easy (Should Be Good First Issue)
```
❌ Change the type of isTokenId from str to bool in line 100
```

## Intermediate

### What It Looks Like

- the problem is clearly defined  
- the expected outcome is specified  
- no exact implementation pattern is provided  
- multiple valid approaches may exist  
- involves multiple components or steps  

The contributor must:
- choose an appropriate approach  
- evaluate trade-offs  
- align with existing patterns and conventions  
- ensure correctness and completeness  

The contributor is expected to:
- explore relevant parts of the codebase  
- reuse existing logic where possible  
- write tests and validate behavior  

The contributor should not:
- invent entirely new abstractions without justification  
- deviate from established patterns without reason  

---

### Intermediate Issue Candidates

- extending existing functionality without a direct pattern  
- implementing workflows or automations similar to others
- adding new services similar to others
- improving API ergonomics or usability  

---

### Intermediate: Good Fit

```markdown
✅ Extend `TokenCreateTransaction` to support NFTs, ensuring compatibility with existing transaction patterns and adding appropriate validation and tests
```

```
✅ Implement a workflow that notifies `team-abc` when the label `discussion` is added to a pull request, aligning with existing CI workflows
```

### Intermediate: Too Easy (Should Be Beginner)
```
❌ Add a `toString()` method to `TokenId`, following the implementation used in `AccountId`
```


## Advanced

### What It Looks Like

- the problem is defined, but the solution is not  
- no clear pattern exists in the codebase  
- multiple valid approaches exist with meaningful trade-offs  
- impacts multiple components or the system as a whole  
- may involve external specifications or cross-system alignment  

The contributor must:
- define the solution  
- design how it integrates with the existing system  
- evaluate trade-offs (performance, compatibility, maintainability)  
- justify decisions clearly  

The contributor is expected to:
- deeply understand relevant parts of the system  
- consider system-wide impact  
- maintain consistency with existing architecture (or justify deviation)  
- ensure backward compatibility where required  
- provide thorough testing and validation  

The contributor should not:
- make arbitrary design decisions  
- introduce unnecessary complexity  
- ignore existing abstractions or conventions  

---

### Advanced Issue Candidates

- implementing features without an existing pattern  
- introducing or evolving abstractions  
- redesigning parts of the system  
- integrating external specifications or protocols  
- improving performance, scalability, or reliability across components  

---

### Advanced: Good Fit

```markdown
✅ Implement HIP-1027 across the SDK, ensuring alignment with existing abstractions and maintaining backward compatibility
```

```
✅ Redesign the event subscription system to improve scalability and reliability while maintaining compatibility with existing APIs
```

### Advanced: Too Easy (Should Be Intermediate)
```
❌ Extend `TokenCreateTransaction` to support NFTs, ensuring compatibility with existing transaction patterns and adding appropriate validation and tests
```