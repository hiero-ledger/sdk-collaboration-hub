# TypeScript API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete TypeScript
patterns,
conventions, and ready‑to‑copy code templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

## Type Mapping

| Generic Type      | TypeScript Type                                | Notes                                      |
|-------------------|------------------------------------------------|--------------------------------------------|
| `string`          | `string`                                       | -                                          |
| `intX`/`uintX`    | `number` or `bigint`                           | Use `bigint` for values > 2^53             |
| `double`          | `number`                                       | -                                          |
| `decimal`         | Third-party library or custom                  | -                                          |
| `bool`            | `boolean`                                      | -                                          |
| `bytes`           | `Uint8Array` or `ArrayBuffer`                  | -                                          |
| `list<TYPE>`      | `Array<TYPE>` or `TYPE[]`                      | Prefer `ReadonlyArray<TYPE>` for immutable |
| `set<TYPE>`       | `Set<TYPE>` or `ReadonlySet<TYPE>`             | -                                          |
| `map<KEY, VALUE>` | `Map<KEY, VALUE>` or `ReadonlyMap<KEY, VALUE>` | -                                          |
| `date`            | `Date` or custom type                          | -                                          |
| `time`            | Custom type                                    | No native time-only type                   |
| `dateTime`        | `Date`                                         | -                                          |
| `zonedDateTime`   | `Date` or `Temporal` (future)                  | -                                          |
