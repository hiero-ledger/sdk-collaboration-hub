# CLAUDE.md

## Project Overview

This is the **Hiero SDK Collaboration Hub** — a centralized resource for design proposals, best practices, and V3 SDK
definition for the [Hiero](https://github.com/hiero-ledger) organization. This is **not** an SDK codebase; it contains
documentation, guidelines, prototypes, and API specifications only.

## Hiero SDK Repositories

The guidelines and proposals in this hub apply to the following SDK repositories under
[hiero-ledger](https://github.com/hiero-ledger):

| Repository       | Language                | URL                                              |
|------------------|-------------------------|--------------------------------------------------|
| hiero-sdk-java   | Java                    | https://github.com/hiero-ledger/hiero-sdk-java   |
| hiero-sdk-js     | JavaScript / TypeScript | https://github.com/hiero-ledger/hiero-sdk-js     |
| hiero-sdk-go     | Go                      | https://github.com/hiero-ledger/hiero-sdk-go     |
| hiero-sdk-rust   | Rust                    | https://github.com/hiero-ledger/hiero-sdk-rust   |
| hiero-sdk-python | Python                  | https://github.com/hiero-ledger/hiero-sdk-python |
| hiero-sdk-cpp    | C++                     | https://github.com/hiero-ledger/hiero-sdk-cpp    |
| hiero-sdk-swift  | Swift                   | https://github.com/hiero-ledger/hiero-sdk-swift  |

Related repositories:

| Repository    | Purpose                                                        | URL                                           |
|---------------|----------------------------------------------------------------|-----------------------------------------------|
| hiero-sdk-tck | Technology Compatibility Kit for verifying SDK implementations | https://github.com/hiero-ledger/hiero-sdk-tck |

## Repository Structure

- `guides/` — Language-agnostic API guideline and language-specific best practices
- `proposals/` — Design proposals for SDK features and changes (including [HIPs](https://hips.hedera.com))
- `v3-sandbox/` — Draft API definitions and prototypes for a possible V3 of the Hiero SDKs
- `templates/` — Reusable templates for proposals, issues, and PRs

## Key Documents

- `guides/api-guideline.md` — The **central meta-language specification**. Defines the syntax for documenting APIs in a
  language-agnostic way (types, annotations, methods, enumerations, namespaces, constants). All language-specific guides
  and V3 API definitions build on this.
- `guides/api-best-practices-{lang}.md` — Language-specific implementation guidelines translating the meta-language into
  concrete patterns for Java, TypeScript, Python, Rust, Go, C++, JavaScript, and Swift.
- `v3-sandbox/prototype-api/` — The draft public API for the V3 SDK, written using the meta-language from
  `api-guideline.md`. Covers common types, configuration, keys, client, transactions, and transactions SPI.

## Working with Guides

When editing or adding to language-specific best practice files (`guides/api-best-practices-*.md`):

- Always check `guides/api-guideline.md` for the canonical meta-language definitions. Language guides must be consistent
  with the meta-language specification.
- The meta-language defines: basic data types, complex types with annotations (`@@immutable`, `@@nullable`,
  `@@default`, `@@min`, `@@max`, `@@minLength`, `@@maxLength`, `@@pattern`), type-level annotations (`@@oneOf`,
  `@@oneOrNoneOf`, `@@finalType`), methods with annotations (`@@async`, `@@throws`), enumerations, generic type
  parameters (`$$T`), namespaces, and constants.
- Each language guide should provide a type mapping table and concrete code examples for how meta-language concepts
  translate to that language.
- Code examples in guides should be compilable/runnable and follow the conventions defined in the guide itself.

## Working with V3 API Definitions

- V3 API definitions in `v3-sandbox/prototype-api/` use the meta-language syntax from `guides/api-guideline.md`.
- The V3 API is designed as an ideal API without backward compatibility constraints to V2.
- The V3 draft is work in progress — no API is fixed yet.
- The Java PoC in `v3-sandbox/prototype-keys-java/` is an outdated Maven project used for early exploration but may not
  reflect the latest API draft.

## Writing Proposals

- Proposals go in the `proposals/` folder.
- Each proposal should include a section that documents the API in the language-agnostic meta-language syntax defined in
  `guides/api-guideline.md`.
- Use templates from `templates/` as a starting point.

## Style and Conventions

- This repository contains only Markdown documentation and small prototype code. No build system at the root level.
- Keep documentation concise and example-driven.
- Do not add emojis to guide or proposal documents.
- Use fenced code blocks with language tags (e.g., ` ```java `, ` ```swift `) for all code examples.