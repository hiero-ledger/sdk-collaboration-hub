# Contributing Effectively by Difficulty Level

- [Good First Issue (GFI)](#good-first-issue-gfi)
- [Beginner](#beginner)
- [Intermediate](#intermediate)
- [Advanced](#advanced)
- [Final Advice](#final-advice)
- [Progression Path](#progression-path)
- [Final Rule](#final-rule)


## Purpose

This guide helps contributors understand how to:
- produce high-quality pull requests  
- get their work reviewed and merged efficiently  

---

## Core Principle

> Each level requires a different way of thinking and working.

- GFI → follow instructions  
- Beginner → follow patterns  
- Intermediate → make decisions  
- Advanced → design solutions  

---

## General Rules (All Levels)

- Stay within the scope of the issue  
- Do not introduce unrelated changes  
- Keep PRs focused and minimal  
- Ensure DCO and GPG key signing
- Ensure a changelog entry (if relevant)
- Read the issue carefully before starting  

---

## Good First Issue (GFI)

Execute the task exactly as described.

---

### How to Approach

- Read the issue carefully  
- Follow each step exactly  
- Do not improvise or optimize  
- Do not make extra good-to-have changes, they should be a separate issue

---

### What a Good PR Looks Like

- change matches instructions exactly  
- correct file and location  
- no extra changes  
- signing requirements met

---

### Common Mistakes

- changing unrelated code  
- not following instructions exactly  
- signing errors

**Important!!** please check the pull request once it is created for errors before requesting a review.

---

## Beginner

Apply an existing pattern correctly.

---

### How to Approach

- study similar implementations and research their methods
- work to understand the code
- apply the pattern carefully

---

### What a Good PR Looks Like

- follows existing patterns  
- clear and readable  
- well researched

---

### Common Mistakes

- inconsistent implementation  
- using AI without studying the code
- not testing the code

---

## How to Get Approved Quickly

- invest in research time
- leverage existing patterns where possible 
- check your work

---

## Intermediate

Implement a solution that meets defined outcomes and quality expectations.

---

### How to Approach

- understand the expected outcome fully  
- identify constraints (APIs, patterns, behavior)  
- take time to thoroughly understand relevant parts of the codebase  
- choose an approach deliberately  

At this level, 50% of the time should be spent researching and testing.

---

### What a Good PR Looks Like

- meets the expected outcome  
- respects all constraints  
- clean, maintainable code, that goes beyond 'it works'
- robustly tested
- handles edge cases  

---

### Common Mistakes

- functional errors or incomplete implementation  
- not sufficient research to understand how the solution fits in the current systems
- weak or missing tests  
- “it works” but poor quality such as hard-to-read code with obscure methods

---

### How to Get Approved Quickly

- Invest at least 25% of time researching
- Plan your approach and optimize the solution
- verify your solution against requirements  
- write tests early  
- polish the code before submitting
- do not think 'it works' means it is done

---

## Advanced

Architect a solution when the problem or approach is not fully understood.

---

### How to Approach

- understand the system and architecture  
- evaluate multiple approaches  
- consider trade-offs and risks  
- plan before coding  

70% of time should be spent researching and planning.

---

### What a Good PR Looks Like

- clear and justified design  
- aligned with architecture  
- maintainable and scalable  
- safe and backward-compatible  
- thoroughly tested  

Advanced PRs often discuss architectural approach before starting coding.

---

### Common Mistakes

- jumping straight into coding  
- inefficient or inappropriate design
- introducing risk or breaking changes  
- not enough research into understanding methods

---

### How to Get Approved Quickly

- propose your approach before implementing  
- explain your design decisions  
- focus on long-term maintainability  
- test thoroughly  

---

## Final Advice

> The fastest way to get merged is to meet the expectations of the level.

- Read the issue requirements carefully
- Think beyond 'it works'
- Research and testing are key components requiring careful decisions

---

## Progression Path

- Start with GFI to learn workflow  
- Move to Beginner to learn patterns  
- Move to Intermediate to build independence  
- Move to Advanced to design systems  

---

## Final Rule

> High-quality, well-scoped PRs get merged faster than ambitious but incomplete ones.
> Maintainers will direct overly-ambitious PRs to more suitable levels