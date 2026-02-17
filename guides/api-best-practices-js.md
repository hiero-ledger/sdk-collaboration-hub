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

## Namespaces

In the meta-language, namespaces are used to group related types, methods, and constants.
In JavaScript, namespaces should be implemented as **ES modules** (files with `.js` or `.mjs` extensions).

### Namespace Mapping

- Each namespace from the meta-language should correspond to a single ES module file
- The module filename should match the namespace name in kebab-case (e.g., `namespace transactions` → `transactions.js`)
- All types, classes, functions, and constants defined within a namespace should be exported from that module
- If a namespace has dependencies (`requires` keyword), import the required modules at the top of the file

### Example

**Meta-language namespace definition:**

```
namespace transactions
requires common, keys

constant MAX_TRANSACTIONS:int32 = 100

Transaction {
    @@immutable id: uuid
    @@immutable amount: double
}
```

**JavaScript implementation (`transactions.js`):**

```javascript
// Import dependencies
import {CommonUtil} from './common.js';
import {KeyManager} from './keys.js';

// Export constants
export const MAX_TRANSACTIONS = 100;

// Export types
export class Transaction {
    #id;
    #amount;

    constructor(id, amount) {
        this.#id = id;
        this.#amount = amount;
        Object.freeze(this);
    }

    getId() {
        return this.#id;
    }

    getAmount() {
        return this.#amount;
    }
}
```

### Base/Shared Utilities

For utility functions that should be available across all namespaces (like validation helpers), create a dedicated base
module (e.g., `lang-base.js`, `validation.js`, or `utils.js`) that other modules can import.

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
    #name;
    #age;

    constructor(name, age) {
        this.#name = requireNonNullString(name, 'name');
        this.#age = requireNonNullNumber(age, 'age');
        Object.freeze(this);
    }

    getName() {
        return this.#name;
    }

    getAge() {
        return this.#age;
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
        Object.freeze(this);
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

## Null and Undefined Handling

In JavaScript, both `null` and `undefined` represent the absence of a value, but they have different semantics.
This section defines how to handle nullability consistently across the our code to align with the language-agnostic API
specification.

### General Rules

- **Use `null` for intentional absence**: When a value is explicitly absent or not applicable, always use `null`.
- **Never use `undefined` in public APIs**: Do not return `undefined` from public methods or accept it as a parameter
  value. `undefined` should **never** be accepted, even for parameters marked as `@@nullable`. Use `null` instead for
  explicit absence.
- **Validate all parameters against `undefined`**: Every parameter in the public API must be validated with
  `requireDefined()` to ensure `undefined` is never passed, regardless of whether the parameter is nullable.
- **Default parameters**: The meta-languages supports the `@@default(value)` annotation.
  Use default parameters (`param = defaultValue`) in that case. Default parameters naturally handle `undefined` by
  replacing it with the default value.
- **Never return `null` for collections**: Always return an empty array, Set, or Map instead of `null`.
- **Validate non-nullable parameters**: For parameters not marked as `@@nullable` in the meta-language, validate that
  they are not `null` or `undefined` using `requireNonNull()`.
- **Document nullability**: Clearly document which parameters and return values can be `null`.
- **Clearing collections**: To explicitly clear an existing collection, provide a dedicated `clearXYZ()` method rather
  than accepting `null` or an empty array as a signal to clear. This makes the intent explicit and maintains consistency
  with the rule that collections should never be `null`.

### Validation Helpers

All SDKs and libraries should include a shared set of validation functions for consistent null checks.
These functions should be part of a base utility module that can be imported throughout the codebase.

In JavaScript, namespaces from the meta-language should be implemented as ES modules.
For shared utilities that need to be available across all namespaces (like validation helpers),
create a dedicated module (`lang-base.js`) that can be imported where needed.

**File: [`lang-base.js`](js-files/lang-base.js)**

**Usage in other modules:**

```javascript
import {
    requireDefined,
    requireNonNull,
    requireNonNullString,
    requireNullableString,
    requireNonNullNumber,
    requireNullableArray,
    requireNonNullArray
} from './lang-base.js';

class UserService {
    setNickname(userId, nickname) {
        requireNonNullString(userId, 'userId');
        requireDefined(nickname, 'nickname');
        // ... implementation
    }
}
```

### Null Handling for Nullable Fields

If a field is annotated with `@@nullable` in the language-agnostic specification, it may be `null` in JavaScript.
Getters and setters should handle `null` explicitly.

**Meta-language example:**

```
class Example {
    @@nullable name: string
}
```

**JavaScript implementation:**

```javascript
class Example {
    #name = null;

    /**
     * Sets the name.
     * @param {string | null} name - The name or null
     */
    setName(name) {
        // Use the helper for nullable strings
        this.#name = requireNullableString(name, 'name');
    }

    /**
     * Gets the name.
     * @returns {string | null} The name or null
     */
    getName() {
        return this.#name;
    }
}
```

### Null Handling for Non-Nullable Fields

If a field is **not** annotated with `@@nullable` in the language-agnostic specification, it must never be `null` or
`undefined`.
Validate non-null values in constructors and setters.

**Meta-language example:**

```
class Example {
    name: string
}
```

**JavaScript implementation:**

```javascript
class Example {
    #name;

    /**
     * Creates an Example instance.
     * @param {string} name - The name (required)
     */
    constructor(name) {
        this.setName(name);
        Object.freeze(this);
    }

    /**
     * Sets the name.
     * @param {string} name - The name (must not be null)
     * @throws {TypeError} if name is null or undefined
     */
    setName(name) {
        this.#name = requireNonNullString(name, 'name');
    }

    /**
     * Gets the name.
     * @returns {string} The name
     */
    getName() {
        return this.#name;
    }
}
```

### Null Handling for Immutable Fields

If a field is annotated with `@@immutable` (and optionally `@@nullable`), validate it in the constructor and do not
provide a setter.

**Meta-language example (non-nullable):**

```
class Example {
    @@immutable name: string
}
```

**JavaScript implementation:**

```javascript
class Example {
    #name;

    /**
     * Creates an Example instance.
     * @param {string} name - The name (required, immutable)
     * @throws {TypeError} if name is null or undefined
     */
    constructor(name) {
        this.#name = requireNonNullString(name, 'name');
        Object.freeze(this);
    }

    /**
     * Gets the name.
     * @returns {string} The name
     */
    getName() {
        return this.#name;
    }
}
```

**Meta-language example (nullable):**

```
class Example {
    @@immutable @@nullable name: string
}
```

**JavaScript implementation:**

```javascript
class Example {
    #name;

    /**
     * Creates an Example instance.
     * @param {string | null} name - The name (nullable, immutable)
     */
    constructor(name) {
        // Use the helper for nullable strings
        this.#name = requireNullableString(name, 'name');
        Object.freeze(this);
    }

    /**
     * Gets the name.
     * @returns {string | null} The name or null
     */
    getName() {
        return this.#name;
    }
}
```

### Null Handling for Method Parameters

All public method parameters must be validated according to their nullability in the meta-language.

**Rules:**

- **Non-nullable parameters**: Validate with `requireNonNull()` or type-specific validators.
- **Nullable parameters**: Allow `null` explicitly and document the semantics.
- **Collections**: Never accept `null` for collections unless marked `@@nullable`. Even if nullable, do not treat `null`
  and an empty collection as equivalent unless explicitly specified.
- **Optional parameters**: Use default parameter values (e.g., `tags = []`) for parameters that have defaults in the
  meta-language.

**Examples:**

```javascript
class UserService {
    /**
     * Updates the age of a user.
     * @param {string} userId - The user ID (required)
     * @param {number} age - The age (required)
     * @throws {TypeError} if userId is null/undefined
     */
    updateAge(userId, age) {
        requireNonNullString(userId, 'userId');
        requireNonNullNumber(age, 'age');
        // ... implementation
    }

    /**
     * Sets a user's nickname.
     * @param {string} userId - The user ID (required)
     * @param {string | null} nickname - The nickname or null to clear
     * @throws {TypeError} if userId is null/undefined or nickname is undefined
     */
    setNickname(userId, nickname) {
        requireNonNullString(userId, 'userId');
        // Use the helper for nullable strings
        requireNullableString(nickname, 'nickname');
        // nickname can be null; handle accordingly
    }

    /**
     * Replaces a user's tags.
     * @param {string} userId - The user ID (required)
     * @param {string[]} tags - The tags (required, can be empty)
     * @throws {TypeError} if userId or tags is null/undefined
     */
    replaceTags(userId, tags) {
        requireNonNullString(userId, 'userId');
        requireNonNullArray(tags, 'tags');
        // ... implementation
    }

    /**
     * Adds tags to a user (if provided).
     * @param {string} userId - The user ID (required)
     * @param {string[] | null} tagsOrNull - The tags to add, or null to do nothing
     * @throws {TypeError} if userId is null/undefined or tagsOrNull is undefined
     */
    addTags(userId, tagsOrNull) {
        requireNonNullString(userId, 'userId');
        requireNullableArray(tagsOrNull, 'tagsOrNull');
        if (tagsOrNull === null) {
            return; // no-op when null
        }
        // ... implementation
    }
}
```

### Null Handling for Method Return Values

Method return types must follow nullability annotations from the meta-language.

**Rules:**

- **Non-nullable returns**: Never return `null` or `undefined`. Throw an exception if the value cannot be provided.
- **Nullable returns**: Return `null` (not `undefined`) when the value is absent. Document what `null` means.
- **Collections**: Never return `null` for arrays, Sets, or Maps. Return an empty collection instead.

**Examples:**

```javascript
class UserRepository {
    /**
     * Gets a user by ID.
     * @param {string} id - The user ID
     * @returns {User} The user
     * @throws {Error} if user not found
     */
    getById(id) {
        requireNonNullString(id, 'id');
        const user = this.#findUserById(id);
        if (user === null) {
            throw new Error(`User not found: ${id}`);
        }
        return user;
    }

    /**
     * Lists all users.
     * @returns {User[]} An array of users (never null, may be empty)
     */
    listAll() {
        return [...this.#users.values()]; // Return a copy, never null
    }

}
```

### Collections and Null

**Never return `null` for collections.** Even if a field or return value is marked `@@nullable` in the meta-language,
interpret that as allowing an *empty* collection, not `null`.

```javascript
class Team {
    #members = [];

    /**
     * Gets the team members.
     * @returns {string[]} An array of member names (never null)
     */
    getMembers() {
        return [...this.#members]; // Return a copy
    }

    /**
     * Sets the team members.
     * @param {string[]} members - The members (required, can be empty)
     */
    setMembers(members) {
        requireNonNullArray(members, 'members');
        this.#members = [...members];
    }

    /**
     * Clears all team members.
     */
    clearMembers() {
        this.#members = [];
    }
}
```