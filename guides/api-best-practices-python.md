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
