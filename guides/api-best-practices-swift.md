# Swift API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete Swift patterns,
conventions, and ready‑to‑copy code templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

## Type Mapping

| Generic Type      | Swift Type                                    | Notes                                           |
|-------------------|-----------------------------------------------|-------------------------------------------------|
| `string`          | `String`                                      | -                                               |
| `intX`            | `Int8`, `Int16`, `Int32`, `Int64`, `Int`      | Use appropriate size; `Int` for general cases   |
| `uintX`           | `UInt8`, `UInt16`, `UInt32`, `UInt64`, `UInt` | Use appropriate size; `UInt` for general cases  |
| `double`          | `Double`                                      | -                                               |
| `decimal`         | `Decimal`                                     | -                                               |
| `bool`            | `Bool`                                        | -                                               |
| `bytes`           | `Data`                                        | Foundation type for byte arrays                 |
| `list<TYPE>`      | `[TYPE]`                                      | Arrays should be immutable where possible       |
| `set<TYPE>`       | `Set<TYPE>`                                   | Sets should be immutable where possible         |
| `map<KEY, VALUE>` | `[KEY: VALUE]`                                | Dictionaries should be immutable where possible |
| `date`            | `Date` or custom `DateOnly`                   | Consider custom wrapper for date-only values    |
| `time`            | Custom `TimeOnly`                             | Foundation lacks time-only type                 |
| `dateTime`        | `Date`                                        | -                                               |
| `zonedDateTime`   | `Date` with `TimeZone`                        | Store timezone separately if needed             |

## Immutable Objects

For types with only `@@immutable` fields, use `struct` with `let` properties:

```swift
// @@immutable name:string
// @@immutable age:int32
struct Person {
    let name: String
    let age: Int32
}