# Go API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete Go patterns,
conventions, and ready‑to‑copy code templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

## Type Mapping

| Generic Type      | Go Type                                          | Notes                  |
|-------------------|--------------------------------------------------|------------------------|
| `string`          | `string`                                         | -                      |
| `intX`            | `int8`, `int16`, `int32`, `int64`                | -                      |
| `uintX`           | `uint8`, `uint16`, `uint32`, `uint64`            | -                      |
| `double`          | `float64`                                        | -                      |
| `decimal`         | Third-party package (e.g., `shopspring/decimal`) | -                      |
| `bool`            | `bool`                                           | -                      |
| `bytes`           | `[]byte`                                         | -                      |
| `list<TYPE>`      | `[]TYPE`                                         | -                      |
| `set<TYPE>`       | `map[TYPE]struct{}`                              | No native set type     |
| `map<KEY, VALUE>` | `map[KEY]VALUE`                                  | -                      |
| `date`            | `time.Time`                                      | Store only date part   |
| `time`            | Custom or `time.Time`                            | -                      |
| `dateTime`        | `time.Time`                                      | -                      |
| `zonedDateTime`   | `time.Time`                                      | Includes location info |

## Implementing Abstractions

Go does not have classes or traditional inheritance. The meta-language `contract` and `abstraction` keywords map to either a Go
`interface` or an embedded struct depending on the keyword used.

### Contract types (`contract` keyword)

Implement as a Go `interface`. Contracts define only method signatures. Generic type parameters (`$$T`) map to Go
type parameters (`[T any]`).

```go
// Meta-language:
//   contract Executable<$$Response> {
//       @@async $$Response execute(client: HieroClient)
//   }

type Executable[R any] interface {
    Execute(client *HieroClient) (R, error)
}
```

### Base types (`abstraction` keyword)

Implement as an unexported struct that is embedded by subtypes. Bases carry shared fields and methods that subtypes
inherit via embedding.

```go
// Meta-language:
//   abstraction Request {
//       @@default(10) maxAttempts: int32
//       protected $$Result withRetry(...)
//   }

type request struct {
    maxAttempts    int
    maxBackoff     time.Duration
    minBackoff     time.Duration
    grpcDeadline   *time.Duration
    requestTimeout *time.Duration
}

func (r *request) withRetry(network Network, action func(node any) (any, error), shouldRetry func(error) bool) (any, error) {
    // shared retry loop implementation
}
```

### Mixed inheritance (base + contracts)

When a type extends both a base and one or more contracts, embed the base struct and satisfy the contract interfaces
by implementing their methods on the composite type.

```go
// Meta-language:
//   abstraction Transaction extends ConsensusRequest, Executable<TransactionResponse> { ... }

type Transaction[T TransactionInterface] struct {
    consensusRequest           // embedded base (inherits fields + methods)
    childTransaction T
}

// Transaction satisfies Executable by implementing Execute:
func (tx *Transaction[T]) Execute(client *HieroClient) (TransactionResponse, error) {
    return tx.withRetry(tx.getNetwork(client), func(node any) (any, error) {
        // ...
    }, tx.shouldRetry)
}
```

### Protected / internal methods

Go has no `protected` keyword. Use unexported methods (lowercase names) to limit visibility to the package. If
subtypes are in the same package, they can call unexported methods directly. If subtypes are in different packages,
consider exposing internal methods through an unexported interface.

### Callback parameters

Map callbacks to Go function types:

| Meta-language | Go |
|---|---|
| `void callback()` | `func()` |
| `void callback(item: $$T)` | `func(T)` |
| `bool callback(error: $$Error)` | `func(error) bool` |
| `$$R callback(node: $$N)` | `func(N) (R, error)` |

## Immutable Objects

Go doesn't enforce immutability; use conventions:

```go
type Person struct {
    name string  // Unexported = "private"
    age  int32
}

func NewPerson(name string, age int32) (*Person, error) {
    if name == "" {
        return nil, errors.New("name must not be empty")
    }
    return &Person{name: name, age: age}, nil
}

func (p *Person) Name() string {
    return p.name
}

func (p *Person) Age() int32 {
    return p.age
}
```