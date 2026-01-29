# JavaScript API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete JavaScript
patterns, conventions, and ready‑to‑copy code templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

## Type Mapping

| Generic Type      | JavaScript Type                          | Notes                          |
|-------------------|------------------------------------------|--------------------------------|
| `string`          | `string`                                 | -                              |
| `intX`/`uintX`    | `number` or `BigInt`                     | Use `BigInt` for values > 2^53 |
| `double`          | `number`                                 | -                              |
| `decimal`         | Third-party library (e.g., `decimal.js`) | -                              |
| `bool`            | `boolean`                                | -                              |
| `bytes`           | `Uint8Array` or `ArrayBuffer`            | -                              |
| `list<TYPE>`      | `Array<TYPE>` or `TYPE[]`                | -                              |
| `set<TYPE>`       | `Set<TYPE>`                              | -                              |
| `map<KEY, VALUE>` | `Map<KEY, VALUE>`                        | -                              |
| `date`            | `Date` or custom                         | JS `Date` includes time        |
| `time`            | Custom                                   | No native time-only type       |
| `dateTime`        | `Date`                                   | -                              |
| `zonedDateTime`   | `Date` or `Temporal` (future)            | -                              |

## Immutable Objects

Use `Object.freeze()`:

```javascript
class Person {
    constructor(name, age) {
        if (!name) {
            throw new Error('name must not be empty');
        }
        this.name = name;
        this.age = age;
        Object.freeze(this);
    }
}
```