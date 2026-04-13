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