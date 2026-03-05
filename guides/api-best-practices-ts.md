# TypeScript API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete TypeScript
patterns,
conventions, and ready‑to‑copy code templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

## Type Mapping

| Generic Type      | TypeScript Type                                | Notes                                      |
|-------------------|------------------------------------------------|--------------------------------------------|
| `string`          | `string`                                       | -                                          |
| `intX`/`uintX`    | `number` or `bigint`                           | Use `bigint` for values > 2^53             |
| `double`          | `number`                                       | -                                          |
| `decimal`         | Third-party library or custom                  | -                                          |
| `bool`            | `boolean`                                      | -                                          |
| `bytes`           | `Uint8Array` or `ArrayBuffer`                  | -                                          |
| `list<TYPE>`      | `Array<TYPE>` or `TYPE[]`                      | Prefer `ReadonlyArray<TYPE>` for immutable |
| `set<TYPE>`       | `Set<TYPE>` or `ReadonlySet<TYPE>`             | -                                          |
| `map<KEY, VALUE>` | `Map<KEY, VALUE>` or `ReadonlyMap<KEY, VALUE>` | -                                          |
| `date`            | `Date` or custom type                          | -                                          |
| `time`            | Custom type                                    | No native time-only type                   |
| `dateTime`        | `Date`                                         | -                                          |
| `zonedDateTime`   | `Date` or `Temporal` (future)                  | -                                          |

## Implementing Abstractions

### Contract types (`contract` keyword)

Implement as a TypeScript `interface`. Contracts define only method signatures. Generic type parameters (`$$T`) map
to TypeScript generics (`<T>`).

```typescript
// Meta-language:
//   contract Executable<$$Response> {
//       @@async $$Response execute(client: HieroClient)
//   }

interface Executable<R> {
    execute(client: HieroClient): Promise<R>;
}
```

### Base types (`abstraction` keyword)

Implement as a TypeScript `abstract class`. Bases carry shared fields and method implementations that subtypes
inherit.

```typescript
// Meta-language:
//   abstraction Request {
//       @@default(10) maxAttempts: int32
//       protected $$Result withRetry(...)
//   }

abstract class Request {
    maxAttempts: number = 10;
    maxBackoff: number = 8000;
    minBackoff: number = 250;
    grpcDeadline?: number;
    requestTimeout?: number;

    protected withRetry<N, R>(
        network: Network<N>,
        action: (node: N) => Promise<R>,
        shouldRetry: (error: Error) => boolean,
    ): Promise<R> {
        // shared retry loop implementation
    }
}
```

### Mixed inheritance (base + contracts)

Use `extends` for the base class and `implements` for contracts. TypeScript supports single class inheritance with
multiple interface implementation.

```typescript
// Meta-language:
//   abstraction Transaction extends ConsensusRequest, Executable<TransactionResponse> { ... }

abstract class Transaction
    extends ConsensusRequest                          // class inheritance (base)
    implements Executable<TransactionResponse> {       // interface implementation (contract)
    // ...
}
```

### Protected methods

Map `protected` directly to the TypeScript `protected` keyword.

### Callback parameters

Map callbacks to TypeScript function types:

| Meta-language | TypeScript |
|---|---|
| `void callback()` | `() => void` |
| `void callback(item: $$T)` | `(item: T) => void` |
| `bool callback(error: $$Error)` | `(error: Error) => boolean` |
| `$$R callback(node: $$N)` | `(node: N) => Promise<R>` or `(node: N) => R` |
