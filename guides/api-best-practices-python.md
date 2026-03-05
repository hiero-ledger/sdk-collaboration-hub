# Python API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete Python patterns,
conventions, and ready‑to‑copy code templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

## Type Mapping

| Generic Type      | Python Type                       | Notes                                              |
|-------------------|-----------------------------------|----------------------------------------------------|
| `string`          | `str`                             | -                                                  |
| `intX`            | `int`                             | Python `int` is arbitrary precision                |
| `uintX`           | `int`                             | Use validation for non-negative values             |
| `double`          | `float`                           | -                                                  |
| `decimal`         | `decimal.Decimal`                 | -                                                  |
| `bool`            | `bool`                            | -                                                  |
| `bytes`           | `bytes` or `bytearray`            | Use `bytes` for immutable, `bytearray` for mutable |
| `list<TYPE>`      | `list[TYPE]`                      | Use `typing.List` for older Python versions        |
| `set<TYPE>`       | `set[TYPE]`                       | -                                                  |
| `map<KEY, VALUE>` | `dict[KEY, VALUE]`                | -                                                  |
| `date`            | `datetime.date`                   | -                                                  |
| `time`            | `datetime.time`                   | -                                                  |
| `dateTime`        | `datetime.datetime`               | -                                                  |
| `zonedDateTime`   | `datetime.datetime` with `tzinfo` | Use `zoneinfo.ZoneInfo` (Python 3.9+)              |

## Implementing Abstractions

### Contract types (`contract` keyword)

Implement as a `typing.Protocol` (structural subtyping) or an `abc.ABC` with only abstract methods and no concrete
implementation. `Protocol` is preferred because it does not require explicit inheritance — any class implementing the
right methods satisfies it.

```python
from typing import Protocol, TypeVar

R = TypeVar("R")

# Meta-language:
#   contract Executable<$$Response> {
#       @@async $$Response execute(client: HieroClient)
#   }

class Executable(Protocol[R]):
    async def execute(self, client: "HieroClient") -> R: ...
```

### Base types (`abstraction` keyword)

Implement as an `abc.ABC` (Abstract Base Class) with concrete methods and instance attributes. Subtypes inherit
shared behavior.

```python
from abc import ABC

# Meta-language:
#   abstraction Request {
#       @@default(10) maxAttempts: int32
#       protected $$Result withRetry(...)
#   }

class Request(ABC):
    def __init__(self):
        self.max_attempts: int = 10
        self.max_backoff: float = 8.0
        self.min_backoff: float = 0.25
        self.grpc_deadline: float | None = None
        self.request_timeout: float | None = None

    def _with_retry(self, network, action, should_retry):
        """Protected: shared retry loop implementation."""
        ...
```

### Mixed inheritance (base + contracts)

Use multiple inheritance with the ABC as the primary base. Since `Protocol` uses structural subtyping, the class does
not need to explicitly inherit from it — implementing the right methods is sufficient. If explicit conformance is
desired, inherit from both.

```python
# Meta-language:
#   abstraction Transaction extends ConsensusRequest, Executable<TransactionResponse> { ... }

class Transaction(ConsensusRequest):
    # Satisfies Executable by implementing execute():
    async def execute(self, client: "HieroClient") -> "TransactionResponse":
        return self._with_retry(
            self._get_network(client),
            lambda node: ...,
            self._should_retry,
        )
```

### Protected / internal methods

Use the single underscore prefix convention (`_method_name`). This signals the method is for internal/subclass use
and should not be called by public API consumers.

### Callback parameters

Map callbacks to `typing.Callable`:

| Meta-language | Python |
|---|---|
| `void callback()` | `Callable[[], None]` |
| `void callback(item: $$T)` | `Callable[[T], None]` |
| `bool callback(error: $$Error)` | `Callable[[Exception], bool]` |
| `$$R callback(node: $$N)` | `Callable[[N], R]` |

## Immutability

When a type and all of its fields are defined as `@@immutable`, prefer Python's standard immutable patterns:

- Use `@dataclass(frozen=True)` for value objects; this is the most common Python idiom for immutable records.
- Prefer immutable containers in the public API: `tuple` for sequences, `frozenset` for sets, and
  `Mapping`/`MappingProxyType` for read-only dict views.
- If a constructor accepts mutable collections, defensively copy into immutable containers and only expose those copies.
- If a type has mixed mutability, keep immutable fields private and expose them via read-only `@property` without
  setters.

Example:

```python
from dataclasses import dataclass
from types import MappingProxyType
from typing import Mapping, Sequence

@dataclass(frozen=True)
class Person:
    name: str
    tags: tuple[str, ...]

    @classmethod
    def from_tags(cls, name: str, tags: Sequence[str]) -> "Person":
        return cls(name=name, tags=tuple(tags))

class Config:
    def __init__(self, values: Mapping[str, str]) -> None:
        self._values = MappingProxyType(dict(values))

    @property
    def values(self) -> Mapping[str, str]:
        return self._values
```
