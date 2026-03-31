## Issue Progression System

The Issue Progression system is a consistent, predictable and scalable system for open-source onboarding.

### Goals

The Issue Progression system is designed to help contributors:
- enter a project
- take on increasing levels of responsibility
- progress to triage, committers and maintainers

It is designed to help maintainers:
- streamline review time
- have documentation and templates to help them support starters
- attract right-fit contributors to each issue
- build their trained developer ecosystem

---

## Issue Progression by Level

Issues are labelled by **difficulty level**.

Each level represents an increase in:
- responsibility  
- independence  
- expectations on quality  


| Difficulty              | Focus                                                                 | Example                                                                 |
|------------------------|-------------------------------------------------------------------------|-------------------------------------------------------------------------|
| Good First Issue (GFI) | Clear, executable tasks for new contributors to learn the workflow                          | *How do I contribute to this repository?*<br/><br/>```Correct 'tokenId' in line 107 to 'TokenId'``` |
| Beginner               | Learning existing patterns in the codebase                              | *What methods does this repository use that are similar to the problem?*<br/><br/>```Add a string method to TokenId``` |
| Intermediate           | Implementing features using established patterns independently          | *What components or steps are required to solve this complex problem?*<br/><br/>```Extend TokenCreateTransaction to support NFTs``` |
| Advanced               | Defining, discussing, and architecting complex solutions                | *What do I want to create and how does it tie in to existing conventions?*<br/><br/>```Implement HIP 1027``` |


Due to the nature of tasks, the developer should generally meet these (non-AI) language skills for each level:

| Level        | Minimum Language Skill  |
|--------------|-----------------|
| GFI          | Beginner        |
| Beginner     | Beginner+       |
| Intermediate | Intermediate+   |
| Advanced     | Advanced        |

Due to the nature of tasks, the developer should generally meet these repository specific knowledge requirements:


| Level        | Repository Knowledge                  |
|--------------|--------------------------------------|
| GFI          | None                                 |
| Beginner     | Narrow but good in areas             |
| Intermediate | Broad but superficial in areas       |
| Advanced     | Broad and in-depth                   |

Thus we estimate developers will invest significant time and provide many contributions to progress through the difficulty levels:


| Level        | Number of Repo-specific Contributions | Time-to-reach Level  |
|--------------|----------------------------------|-------------|
| GFI          | 0 Contributions                  | New         |
| Beginner     | 3–15 Relevant Contributions      | 1 Month     |
| Intermediate | 10–50 Relevant Contributions     | 2–5 Months  |
| Advanced     | 40+ Relevant Contributions       | 4 Months    |


NOTE: Developers should be encouraged to identify appropriate issues for their skills. This means moving up and down levels depending on the unique issue requirements.

---

### Issue Progression by Documentation

It is helpful for issues to be documented to suit developers at the level they are at.

For the most seemless experience, the we recommend:

- Issue templates tailored for each level
- Complementary documentation (signing, workflow, testing, etc) linked in the templates

The Issue Progression System offers pre-existing templates and documentation to fine-tune to repository needs.

---

### Growing the Team

As developers progress in skill and ownership, they may be ready to take on a team role in a repository. However, building maintainer skills takes time and a new set of skills to build.

The Issue Progression System suggests creating the following roles which train the following skills:
| Role   | Actions                                                                 | Skills Built                                                                 |
|-------------|---------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| Triage      | Interface with contributors, manage labels, perform initial reviews       | Issue classification, communication, expectation setting, pattern recognition       |
| Committer   | Identify issues, create progession pipelines, and perform write reviews       | Codebase maintenance, advanced review skills    |
| Maintainer  | Merge contributions and ensure project health and consistency       | System design, decision-making, codebase stewardship |

The Issue Progression System recommends each role focuses on best-fit-issues:

| Role       | Recommended Review Scope |
|------------|--------------------------|
| Triage     | GFI, Beginner            |
| Committer  | Beginner, Intermediate   |
| Maintainer | Advanced                 |

This ensures efficient distribution of tasks while ensuring review quality and skill progression.

#### Progressing Team Members

The Issue Progression Initiative suggests the following mapping between team roles and difficulty level which should be supplemented by the observed skills above: 

| Role         | Minimum Difficulty Reached  | Logic                                                                        |
|--------------|---------------------|------------------------------------------------------------------------------|
| Triage       | Intermediate        | Demonstrated knowledge of the repo and benefits from the safety of read-only |
| Committer    | Advanced            | Demonstrated deep understanding and responsibility for write permissions     |
| Maintainer   | Advanced+           | Superior architectural knowledge suitable to help direct a project           |

For example, a developer may be considered as a candidate for a maintainer once they have a track record of completing at least two months of advanced issues with overwhelmingly positive feedback, plus have shown the ability and interest to label, create issues at each difficulty and engage with contributors. They should have also shown good interest and competence at reviewing advanced issues.

---

## Recommendations

- Every issue must have exactly one difficulty label  
- Labels must consistently reflect objective difficulty
- Use complementary templates and provide documentation 
- Engage contributors with challenges and greater ownership
- Involve contributors in the review process, at appropriate levels

## Further Reading for Maintainers
Follow these recommended steps:

[1. Label Issues by Difficulty](guides/issue-progression/for-maintainers/labelling-issues-examples.md)

[Creating Issues Examples](guides/issue-progression/for-maintainers/creating-issues-examples.md)

[AI Prompts to Create Issues](guides/issue-progression/for-maintainers/creating-issues-AI-prompts.md)

[Reviewing Strategies At Each Difficulty](guides/issue-progression/for-maintainers/reviewing-pull-requests.md)

## Resources

[Issue Templates for Maintainers Tailored to Difficulty](guides/issue-progression/for-maintainers/editable-templates)

[Documents for Developers Tailored to Difficulty](guides/issue-progression/for-developers)


---

