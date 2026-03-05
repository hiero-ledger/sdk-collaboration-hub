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

## Implementing Abstractions

### Contract types (`contract` keyword)

Implement as a Swift `protocol`. Contracts define only method signatures. Generic type parameters (`$$T`) map to
associated types.

```swift
// Meta-language:
//   contract Executable<$$Response> {
//       @@async $$Response execute(client: HieroClient)
//   }

protocol Executable {
    associatedtype Response
    func execute(client: HieroClient) async throws -> Response
}
```

### Base types (`abstraction` keyword)

Implement as a Swift `class`. Bases carry shared stored properties and method implementations that subtypes inherit.

```swift
// Meta-language:
//   abstraction Request {
//       @@default(10) maxAttempts: int32
//       protected $$Result withRetry(...)
//   }

public class Request {
    public var maxAttempts: Int = 10
    public var maxBackoff: TimeInterval = 8.0
    public var minBackoff: TimeInterval = 0.25
    public var grpcDeadline: TimeInterval?
    public var requestTimeout: TimeInterval?

    internal func withRetry<N, R>(
        network: some Network<N>,
        action: (N) async throws -> R,
        shouldRetry: (Error) -> Bool
    ) async throws -> R {
        // shared retry loop implementation
    }
}
```

### Mixed inheritance (base + contracts)

Use class inheritance for the base and protocol conformance for contracts. Swift supports single class inheritance
with multiple protocol conformance.

```swift
// Meta-language:
//   abstraction Transaction extends ConsensusRequest, Executable<TransactionResponse> { ... }

public class Transaction: ConsensusRequest, Executable {
    public typealias Response = TransactionResponse
    // ...
}
```

### Protected / internal methods

Swift has no `protected` keyword. Use `internal` (the default access level) to limit visibility to the module. For
methods that should only be called by subclasses, document this intent and use `internal` access.

### Callback parameters

Map callbacks to Swift closure types:

| Meta-language | Swift |
|---|---|
| `void callback()` | `@escaping () -> Void` |
| `void callback(item: $$T)` | `@escaping (T) -> Void` |
| `bool callback(error: $$Error)` | `@escaping (Error) -> Bool` |
| `$$R callback(node: $$N)` | `(N) async throws -> R` |

Use `@escaping` when the closure is stored for later invocation. Use `@Sendable` when the closure may be called from
a different concurrency context.

## Immutable Objects

For types with only `@@immutable` fields, use `struct` with `let` properties:

```swift
// @@immutable name:string
// @@immutable age:int32
struct Person {
    let name: String
    let age: Int32
}