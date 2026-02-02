# Rust API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete Rust patterns,
conventions, and ready‑to‑copy code templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

## Type Mapping

| Generic Type      | Rust Type                                       | Notes                                       |
|-------------------|-------------------------------------------------|---------------------------------------------|
| `string`          | `String` or `&str`                              | Use `String` for owned, `&str` for borrowed |
| `intX`            | `i8`, `i16`, `i32`, `i64`, `i128`               | -                                           |
| `uintX`           | `u8`, `u16`, `u32`, `u64`, `u128`               | -                                           |
| `double`          | `f64`                                           | -                                           |
| `decimal`         | Third-party crate (e.g., `rust_decimal`)        | -                                           |
| `bool`            | `bool`                                          | -                                           |
| `bytes`           | `Vec<u8>` or `&[u8]`                            | -                                           |
| `list<TYPE>`      | `Vec<TYPE>`                                     | -                                           |
| `set<TYPE>`       | `HashSet<TYPE>` or `BTreeSet<TYPE>`             | -                                           |
| `map<KEY, VALUE>` | `HashMap<KEY, VALUE>` or `BTreeMap<KEY, VALUE>` | -                                           |
| `date`            | `chrono::NaiveDate`                             | Using `chrono` crate                        |
| `time`            | `chrono::NaiveTime`                             | Using `chrono` crate                        |
| `dateTime`        | `chrono::NaiveDateTime`                         | Using `chrono` crate                        |
| `zonedDateTime`   | `chrono::DateTime<Tz>`                          