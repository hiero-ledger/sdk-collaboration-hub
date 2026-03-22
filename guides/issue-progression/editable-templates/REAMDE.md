# Issue Templates Setup Guide

## Purpose

This guide explains how to install and use the **Issue Progression Initiative (IPI) templates** in your repository.

When properly configured, these templates enable:
- maintainers to create consistent, well-scoped issues  
- contributors to understand expectations at each level  
- a structured progression from onboarding to advanced contributions  

---

## ⚠️ Important Note

When you add or merge issue templates:

> **All existing upstream templates will be overwritten**

This means:
- You must include **all templates you want to keep**
- Any missing templates will be removed from the repository

---

## Where to Add Templates

All issue templates **must be placed exactly in**:
```
.github/ISSUE_TEMPLATE/
```

> ⚠️ This path is required for GitHub to recognize and render issue templates correctly.

They should be labelled with a XX number prefix so they render in a correct order.

For example:
```
.github/ISSUE_TEMPLATE/01-good-first-issue-candidate-template.yml
```
```
.github/ISSUE_TEMPLATE/02-good-first-issue-template.yml
```

If the templates do not show up on clicking the `Create Issue` button, there is a syntax mistake in the yml.

---

## Adaptable Templates

The Issue Progression Initiative provides adaptable templates that follow best practices.


These templates are designed to:
- be consistent across repositories  
- be thoughtful to the developer skillset and goals
- be adaptable to suit your project’s needs  

---

## Recommended Templates

The Issue Progression Initiative recommends including the following templates:

### Issue Progression Templates

- `01-good-first-issue.yml`
- `02-beginner-issue.yml`
- `03-intermediate-issue.yml`
- `04-advanced-issue.yml`

These support the full contributor progression pipeline.

Optionally:
- `00-good_first_issue_candidate.yml` 

Some repositories prefer avoiding GFIC and labelling good first issues in candidate stage as:
`status: awaiting triage`

**⚠️ Warning!**: for the labels to render on the templates, the labels must be exactly:
`Good First Issue Candidate`
`Good First Issue`
`beginner`
`intermediate`
`advanced`

You can change these labels to your preferences, but you must change the default label attached to the template
---

### Standard Templates

- `bug_report.yml`
- `feature_request.yml`

These cover general repository needs outside the progression system.

Blank templates are optional and are enabled in:
- `config.yml`

---

## Installation Steps

1. Create a pull request

2. Create the required directory with the desired, pre-fixed templates

3. Merge to main
