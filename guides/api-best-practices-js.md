# JavaScript API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete JavaScript
patterns, conventions, and ready‑to‑copy code templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

## JS Runtime Compatibility

Our JS Libraries should target current, widely used runtimes (modern evergreen browsers and current active Node.js LTS).
Very old runtimes can be ignored.
This guideline assumes support for `class` fields and `#private` fields and a global `fetch` in Node.js 18+.
If `fetch` is not available in the target runtime, inject a fetch-like client or use a polyfill at the application
layer.

## Type Mapping

Use the following canonical mappings when turning meta types into JavaScript:

| Generic Type      | JavaScript Type                          | Notes                                                                                      |
|-------------------|------------------------------------------|--------------------------------------------------------------------------------------------|
| `string`          | `string`                                 | -                                                                                          |
| `intX`/`uintX`    | `number` or `BigInt`                     | Use `BigInt` for values > 2^53 (safe integer limit)                                        |
| `double`          | `number`                                 | IEEE 754 double precision floating-point                                                   |
| `decimal`         | Third-party library (e.g., `decimal.js`) | For arbitrary precision decimal arithmetic -> TODO: define one we want to uses             |
| `bool`            | `boolean`                                | -                                                                                          |
| `bytes`           | `Uint8Array`                             | Preferred over `ArrayBuffer` for type safety                                               |
| `list<TYPE>`      | `Array<TYPE>` or `TYPE[]`                | Arrays in the public API should always be immutable    -> TODO: define one we want to uses |
| `set<TYPE>`       | `Set<TYPE>`                              | Sets in the public API should always be immutable                                          |
| `map<KEY, VALUE>` | `Map<KEY, VALUE>`                        | Maps in the public API should always be immutable                                          |
| `date`            | `Date` or custom                         | JS `Date` includes time component; consider custom classes                                 |
| `time`            | Custom class                             | No native time-only type in JavaScript                                                     |
| `dateTime`        | `Date`                                   | Standard JavaScript Date object                                                            |
| `zonedDateTime`   | `Date` or `Temporal` (future)            | Consider libraries like `date-fns` or `day.js` -> TODO: define one we want to uses         |

### Numeric Types

For numeric types, JavaScript developers should consider the following:

- Use `number` for values that fit within the safe integer range (-2^53 to 2^53 - 1)
- Use `BigInt` for larger integer values or when exact precision is critical
- For `intX` and `uintX` types that are not marked as `@@nullable`, use `number` or `BigInt` as appropriate -> TODO:
  define one we want to use
- For nullable numeric types, use `number | null` or `BigInt | null`  -> TODO: define one we want to uses
- For `double` types, use `number`
- For `decimal` types, use a third-party library like `decimal.js` or `big.js` -> TODO: define one we want to use

## Private Fields (`#`)

Use ECMAScript private fields for all internal state that is not part of the public API.
Private fields are declared with a leading `#` and are only accessible within the class body.
Do not use underscore prefixes (`_field`) for privacy.

## Immutable Objects

JavaScript does not have built-in immutable class syntax like Java records. However, immutability should be enforced
using `Object.freeze()`.
Note that `Object.freeze()` is shallow and does not prevent mutations of nested objects.
If deep immutability is required, create deep copies on input/output or use a dedicated immutable data structure.
An example of that pattern is shown in the "Sets and Maps" section below.

### Completely Immutable Objects

If a type definition in the meta-language only contains fields annotated with `@@immutable`, the object should be frozen
using `Object.freeze()`:

```javascript
class Person {
    constructor(name, age) {
        this.name = name;
        this.age = age;
        Object.freeze(this);
    }

    getName() {
        return this.name;
    }

    getAge() {
        return this.age;
    }
}
```

### Immutable Sets and Maps

For `Set` and `Map` types, return defensive copies. `Object.freeze()` does not make `Set` or `Map` immutable.

```javascript
class TagManager {
    #tags = new Set();
    #metadata = new Map();

    constructor(tags = [], metadata = {}) {
        // Copy the input set
        this.#tags = new Set(tags);
        // Copy the input map
        this.#metadata = new Map(Object.entries(metadata));
    }

    // Return a defensive copy of the set
    getTags() {
        return new Set(this.#tags);
    }

    // Return a defensive copy of the map
    getMetadata() {
        return new Map(this.#metadata);
    }

    addTag(tag) {
        this.#tags.add(tag);
    }

    removeTag(tag) {
        this.#tags.delete(tag);
    }

    setMetadata(key, value) {
        this.#metadata.set(key, value);
    }
}
```
