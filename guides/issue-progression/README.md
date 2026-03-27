## Issue Progression System

The Issue Progression system is a consistent, predictable and scalable system for open-source onboarding.

### Goals

The Issue Progression system is designed to help contributors:
- enter a project
- take on increasing levels of responsibility
- progress to triage, committers and maintainers

It is designed to help maintainers with:
- providing helpful guides, documentation and templates
- attracting right-fit contributors to each issue
- reduced reviewer burden through tailored messaging, complementary documentation and a larger team

---

## Core Principles

Developers are at different stages in their development. 

Issues are labelled by **difficulty level** and **documented appropriately**, challenging developers responsibly and the right amount.

Each level represents an increase in:
- responsibility  
- independence  
- expectations on quality  

and offers a change in: documentation.

Each level has a focus and works to build a skill in the contributor pipeline:


| Level        | Focus                                                       | Skill Built                              |
|--------------|-------------------------------------------------------------|------------------------------------------|
| GFI          | Execute clearly defined tasks                               | Learn contribution workflow              |
| Beginner     | Apply existing patterns within the codebase                 | Understand codebase and conventions      |
| Intermediate | Choose and implement approaches within existing constraints | Independent problem-solving skills       |
| Advanced     | Define, discuss and architect solutions                     | Shape system architecture                |



### Examples:

**Good First Issue:** *how do I contribute to this repository?*
```yml
Correct 'tokenId' in line 107 to 'TokenId' 
```

**Beginner Issue:** *What methods does this repository use that is similar to the problem?*
```yml
Add a string method to TokenId
```

**Intermediate Issue:** *What components or steps are required to solve this complex problem?*
```yml
Extend TokenCreateTransaction to support NFTs
```

**Advanced Issue:** *What do I want to create and how does it tie in to existing conventions?*
```yml
Implement HIP 1027
```

---

### Estimating Developer Skill Levels

The following indicators can be suggested:

| Level        | Contributions                  | Language Skill        | Repository Knowledge          | Time Frame  |
|--------------|--------------------------------|-----------------------|-------------------------------|-------------|
| GFI          | New to Open Source or Repo     | Beginner              | None                          | New         |
| Beginner     | 3-15 Relevant Contributions    | Beginner+             | Narrow but good in areas      | 1 Month     |
| Intermediate | 10-40 Relevant Contributions   | Intermediate+         | Broad but superficial in areas| 2-4 Months  |
| Advanced     | 40+ Relevant Contributions     | Advanced              | Broad and in-depth            | 4 Months    |

---

### Documentation

It is helpful but not necessary to have appropriate documentation in issues to meet developers at the level they are at.

Developers are at different stages in their development, thus they would benefit from certain information and documentation that would be redundant to another level.

The Issue Progression Initiative (IPI) recommends:

- Issue templates tailored for each level
- Complementary documentation (signing, workflow, testing, etc)
- Listening to feedback and adjusting approach

The Issue Progression Initiatives has a variety of pre-existing templates and documentation to leverage and fine-tune to repository needs.

---

### Growing the Team

As developers progress in skill and ownership, they may be ready to take on a team role in a repository.

#### Creating Team Roles

The Issue Progression Initiative suggests creating the following teams. Each should have a role and a skill being trained: 

| Team Name   | Team Role                                                                 | Team Skills Built                                                                 |
|-------------|---------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| Triage      | Interface with contributors, manage labels, perform initial reviews       | Issue classification, communication, expectation setting, pattern recognition       |
| Committer   | Identify issues, create progession pipelines, and perform write reviews       | Codebase maintenance, advanced review skills    |
| Maintainer  | Merge contributions and ensure project health and consistency       | System design, decision-making, codebase stewardship |

#### Progressing Team Members

The Issue Progression Initiative suggests the following mapping between team roles and difficulty level which should be supplemented by the observed skills above: 

| Role         | Difficulty Reached  | Logic                                                                        |
|--------------|---------------------|------------------------------------------------------------------------------|
| Triage       | Intermediate        | Demonstrated knowledge of the repo and benefits from the safety of read-only |
| Committer    | Advanced            | Demonstrated deep understanding and responsibility for write permissions     |
| Maintainer   | Advanced+           | Superior architectural knowledge suitable to help direct a project           |

For example, a developer may be considered as a candidate for a maintainer once they have a track record of completing at least two months of advanced issues with overwhelmingly positive feedback, plus have shown the ability and interest to label, create issues at each difficulty and engage with contributors.

---

## Recommendations

- Regularly create issues and assign developers
- Every issue must have exactly one difficulty label  
- Labels must consistently reflect objective difficulty
- Use complementary templates and provide documentation 
- Engage contributors with challenges and greater ownership

## Next Steps
Follow these recommended steps:

[Learn How to Label Issues by Difficulty](guides/issue-progression/labelling.md)
[Learn How to Describe Issues by Difficulty](guides/issue-progression/describing-issues-difficulty.md)
[Learn How to Use AI to Create Issue Descriptions](guides/issue-progression/creating-issues-AI-prompts.md)
[Learn Reviewing Strategies At Each Difficulty](guides/issue-progression/productive-reviewing-by-level.md)
[Copy Templates Tailored to Difficulty](guides/issue-progression/editable-templates)
[Copy Documents Tailored to Difficulty](guides/issue-progression/docs)


---

