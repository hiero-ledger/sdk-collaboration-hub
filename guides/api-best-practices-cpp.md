# C++ API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete C++ patterns,
conventions, and ready‑to‑copy code templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

## Type Mapping

| Generic Type      | C++ Type                                      | Notes                                   |
|-------------------|-----------------------------------------------|-----------------------------------------|
| `string`          | `std::string`                                 | -                                       |
| `intX`            | `int8_t`, `int16_t`, `int32_t`, `int64_t`     | From `<cstdint>`                        |
| `uintX`           | `uint8_t`, `uint16_t`, `uint32_t`, `uint64_t` | From `<cstdint>`                        |
| `double`          | `double`                                      | -                                       |
| `decimal`         | Custom or third-party library                 | No standard decimal type                |
| `bool`            | `bool`                                        | -                                       |
| `bytes`           | `std::vector<std::byte>`                      | C++17+                                  |
| `list<TYPE>`      | `std::vector<TYPE>`                           | -                                       |
| `set<TYPE>`       | `std::set<TYPE>`                              | -                                       |
| `map<KEY, VALUE>` | `std::map<KEY, VALUE>`                        | Or `std::unordered_map` for performance |
| `date`            | `std::chrono::year_month_day`                 | C++20                                   |
| `time`            | `std::chrono::time_of_day`                    | C++20                                   |
| `dateTime`        | `std::chrono::system_clock::time_point`       | -                                       |
| `zonedDateTime`   | `std::chrono::zoned_time`                     | C++20                                   |
