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
