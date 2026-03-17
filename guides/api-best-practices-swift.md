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
```

## Collections

The public API must never return `nil` for collections. Instead, an empty collection must be returned.
Since the data types `list`, `set`, and `map` should never be nullable, it never makes sense to wrap a collection in an
`Optional` in the public API.

In Swift, `Array`, `Set`, and `Dictionary` are value types with copy-on-write semantics. This means they are inherently
safe against shared mutable state — when a collection is assigned or passed, a logical copy is made and mutations to
one copy do not affect the other. This makes Swift collections naturally suited for immutable API surfaces.

Collections exposed by the public API should be immutable wherever possible. Use `let` to declare collection properties
that must not be reassigned, and return plain value types from methods. Since Swift collections are value types,
returning them from a method already provides the caller with an independent copy — no additional wrapping is needed.

```swift
struct Team {
    // Immutable collection — cannot be reassigned
    let members: [String]

    // Mutable collection — can be replaced but callers receive independent copies
    var tags: [String]
}
```

When returning collections from methods, the caller always receives an independent value:

```swift
struct Team {
    private var names: [String] = []

    // Returns an independent copy — callers cannot mutate internal state
    func getNames() -> [String] {
        return names
    }

    // Never return nil — return an empty collection instead
    func getFilteredNames(prefix: String) -> [String] {
        return names.filter { $0.hasPrefix(prefix) }
    }

    mutating func setNames(_ newNames: [String]) {
        self.names = newNames
    }

    mutating func addName(_ name: String) {
        names.append(name)
    }

    mutating func removeName(_ name: String) {
        names.removeAll { $0 == name }
    }
}
```

If a collection is passed as a parameter and stored, Swift's value semantics already ensure the stored value is
independent from the caller's copy. No explicit defensive copying is needed:

```swift
struct Team {
    let names: [String]

    // The initializer stores an independent copy automatically due to value semantics
    init(names: [String]) {
        self.names = names
    }
}
```