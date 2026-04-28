# API Guideline

This document outlines the best practices and conventions for designing and implementing APIs for the SDKs.
Following these guidelines will help ensure that APIs are consistent, easy to use, and maintainable.
Next to that the guidelines define a syntax for documenting the APIs in a language-agnostic way for proposals.

The [proposals](../proposals) folder contains design documents of new features or changes for our SDKs.
Each proposal should include a section that documents the API in a language-agnostic way by using the guidelines defined
in this document.

## Concrete implementations

We provide concrete implementation guidelines for the following programming languages:

- [Rust API Implementation Guideline](api-best-practices-rust.md)
- [C++ API Implementation Guideline](api-best-practices-cpp.md)
- [TypeScript API Implementation Guideline](api-best-practices-ts.md)
- [Python API Implementation Guideline](api-best-practices-python.md)
- [Java API Implementation Guideline](api-best-practices-java.md)
- [Go API Implementation Guideline](api-best-practices-go.md)
- [JavaScript API Implementation Guideline](api-best-practices-js.md)
- [Swift API Implementation Guideline](api-best-practices-swift.md)

## Syntax

The following syntax should be used to document the APIs in a language-agnostic way

### Basic data types

The following basic data types should be used in the API documentation.

| Data Type                  | Description                                                           |
|----------------------------|-----------------------------------------------------------------------|
| `string`                   | A sequence of characters                                              |
| `intX`                     | A signed integer of X bits (8 <= X <= 256)                            |
| `uintX`                    | An unsigned integer of X bits (8 <= X <= 256)                         |
| `double`                   | A native floating-point number in 64-bit base-2 format                |
| `decimal`                  | A decimal number with arbitrary precision                             |
| `bool`                     | A boolean value                                                       |
| `bytes`                    | A sequence of bytes                                                   |
| `list<TYPE>`               | A list of elements of type TYPE                                       |
| `set<TYPE>`                | A set of elements of type TYPE                                        |
| `collection<TYPE>`         | A collection of elements of type TYPE                                 |
| `map<KEY, VALUE>`          | A map that maps KEY values to VALUE values                            |
| `type`                     | A type identity that can be used to specify a complex type at runtime |
| `uuid`                     | A universally unique identifier                                       |
| `date`                     | A date value (ISO 8601 calendar date)                                 |
| `time`                     | A time value without date or timezone (nanosecond precision)          |
| `dateTime`                 | A date and time value without timezone (nanosecond precision)         |
| `zonedDateTime`            | A date and time value with timezone (nanosecond precision)            |
| `streamResult<TYPE>`       | A stream item that is either a success value of TYPE or an error      |
| `function<R m(p: T, ...)>` | A function type (often called lambda/callable)                        |
| `ANY`                      | A top type — accepts any value. Use sparingly (see best practices)    |

### Function Types

As shown in the Basic data types table function types are defined using the following syntax:
`function<R m(p: T, ...)>`.
The expression within the angle brackets is based on the syntax of a method declaration.
`R` defines that return type (that can be void).
`m` represents the function name that should be a descriptive name for the function.
The parameters are defined using the following syntax: `paramName: DataType`.
A lambda expression can have zero or more parameters.

The following example shows the typical usage of a function type to define a callback (or listener):

```
subscribe(callback: function<void onEvent(event: Event)>)
```

The following example shows a function type without any parameters that is often used for async handling:

```
execute(action: function<void run()>)
```

### Complex Types

Complex data types can be defined using the basic data types and other complex data types.
A complex data type definition can contain [attributes](#attributes) and [methods](#methods).
The following syntax should be used to define complex data types:

```
DataTypeName {
    fieldName1: DataType1
    fieldName2: DataType2
    ReturnType methodName(param1: DataType1, param2: DataType2)
}
```

#### Complex Type annotations

Type annotations apply to a complex data type as a whole (as opposed to a single attribute or a method).
The following annotations should be used:

- `@@oneOf(field1, field2[, ...])`: Exactly one of the referenced fields can be non-null/non-undefined at any given
  time.
- `@@oneOrNoneOf(field1, field2[, ...])`: Exactly one of the referenced fields can be non-null/non-undefined at any
  given time or all fields are null/undefined.
- `@@finalType`: Indicates that the type is final and cannot be extended.

Rules and recommendations for `@@oneOf`:

- All listed fields must be declared with `@@nullable`
- None or all listed fields must be declared with `@@immutable`.
- If the fields are not `@@immutable` and a field is set, an SDK must unset all other fields
- If the fields are not `@@immutable` and the currently set field is unset, an SDK must throw an error.
- None or exactly one of the listed fields must be annotated by `@@default(value)`
- If the fields are not `@@immutable` and none is annotated by `@@default(value)` at least one must be set by the
  constructor to not end in an invalid state.

Rules and recommendations for `@@oneOrNoneOf`:

- All listed fields must be declared with `@@nullable`
- None or all listed fields must be declared with `@@immutable`.
- If the fields are not `@@immutable` and a field is set, an SDK must unset all other fields
- None or exactly one of the listed fields must be annotated by `@@default(value)`

Examples:

```
@@oneOf(email, phone)
ContactInfo {    
    @@nullable email: string
    @@nullable phone: string
}

```

#### Abstraction & Inheritance

Complex data types can inherit from other complex data types to reuse fields and methods.

Definition of an abstract type:

- Use `abstraction` to declare an abstract type.
- We do not define if it should be a class or an interface (that is language specific).

Syntax example:

```
abstraction Copyable {
    copy(): Copyable
}
```

Definition of a child type:

- Use `extends` to declare a child type.
- Multiple inheritance is supported but should be avoided in general.
- Inherited members: All fields and methods from the parent are inherited by the child (since we only define public API)

Syntax examples:

```
Person {
    name: string
}

Employee extends Person {
    employeeNumber: string
}
```

#### Generic Type parameters

Complex types may declare generic type parameters.
Generic type parameter names must be written with a leading `$$` to distinguish them from concrete types.
The name of a generic type parameter can be any name and does not have to be related to a complex type name.
The name of a generic type parameter must be unique within the scope of the containing type.
Examples of type parameter names:

- `$$T`, `$$Product`, `$$Self`

A concrete example of the syntax looks like this:

```
abstraction Factory<$$Product> {
    $$Product create()
}
```

The `$$Product` type parameter is used in the `create` method to define the return type of the method.
A complex type that extends a generic type can use the type parameter in its own definition.
A concrete example of the syntax looks like this:

```
CarFactory extends Factory<Car> {
}
```

In that example, the `CarFactory` provides a `create` method that returns a `Car`.
`Car` must be a concrete complex type (not a generic type parameter).

Generic types with multiple parameters are written in angle brackets: `TypeName<$$T1, $$T2, ...>`.

To make a generic type more concrete, the `extends` keyword can be used to constrain the type parameter:

```
abstraction FruitFactory<$$Product extends Fruit> {
    $$Product create()
}
```

In the given example, the `FruitFactory` provides a `create` method that returns a `Fruit`.
`Fruit` must be a concrete complex type (not a generic type parameter).

#### Wildcard type arguments

Sometimes a method or field needs to reference a generic type without committing to a specific type parameter — for
example, a heterogeneous collection whose elements may be different concrete instantiations of the same generic type.
The `ANY` keyword can be used as a wildcard type argument to express this.

Syntax:

```
Type<ANY>             // unbounded — any concrete instantiation of Type
Type<ANY extends T>   // upper-bounded — any instantiation with a subtype of T
```

Example:

```
ContractCall {
    common.ContractId createContract(fileId: common.FileId, constructorParams: ContractParam<ANY>...)
}
```

In this example, `constructorParams` accepts a heterogeneous list of `ContractParam` instances — each element may be
a different concrete instantiation (e.g., `ContractParam<int32>`, `ContractParam<string>`).

Rules:

- `ANY` and `$$T` are distinct concepts: `$$T` is a generic type parameter that the caller binds to a single concrete
  type, while `ANY` is a wildcard meaning "an unknown concrete type" that may differ across uses.
- Languages without native wildcard support must define an idiomatic mapping in their best-practice guideline
  (e.g., `?` / `? extends T` in Java, `any Protocol` in Swift, `Box<dyn Trait>` in Rust, a polymorphic base type or
  `std::variant` in C++, an interface type in Go).

`ANY` can also appear as a standalone type (e.g., a parameter or return type), in which case it acts as the
language's top type — see [Basic data types](#basic-data-types). Standalone use is strongly discouraged; see
[Avoid `ANY` as a standalone type](#avoid-any-as-a-standalone-type) for the rationale and alternatives.

### Enumerations

Enumerations can be defined using the following syntax:

```
enum EnumName {
    VALUE1
    VALUE2
}
```

[Attributes](#attributes) can be added to enumerations. Attributes added to enumerations must be immutable (annotated
with `@@immutable`).

```
enum EnumName {
    VALUE1
    VALUE2
    
    @@immutable primitiveValue: int
}
```

[Methods](#methods) can be added to enumerations:

```
enum EnumName {
    VALUE1
    VALUE2
    
    int calcPrimitiveType()
}
```

### Attributes

Attributes can be defined using the following syntax:

```
    fieldName: DataType
```

By default an attribute is mutable.
Attributes can be declared immutable by annotating them with `@@immutable`.
In general, it is best practice to make all attributes immutable unless there is a clear reason why they cannot be.

Every attribute, including immutable ones, can be accessed.
Here the access is implemented in a language-specific way. That can be a getter method or a native property access.

A mutable attribute can be changed at any time.
Here a language-specific way to change an attribute is implemented.
In most languages this is equivalent to a setter method or a native property access.

Please refer to the best practice guideline for a specific language to understand how attribute access (read/write) must
be implemented.

#### Attribute annotations

Attribute annotations can be used to provide additional information about attributes in complex data types.
The following annotations should be used:

- `@@immutable`: Indicates that the field is immutable and cannot be changed after creation.
- `@@nullable`: Indicates that the field can be null or undefined (language-specific).
- `@@default(value)`: Indicates that the field has a default value.
- `@@min(value)`: Indicates the minimum value for numeric fields. Should be included if the value must be enforced at
  the SDK level.
- `@@max(value)`: Indicates the maximum value for numeric fields. Should be included if the value must be enforced at
  the SDK level.
- `@@minLength(value)`: Indicates the minimum length for string fields. Should be included if the value must be enforced
  at the SDK level.
- `@@maxLength(value)`: Indicates the maximum length for string fields. Should be included if the value must be enforced
  at the SDK level.
- `@@pattern(regex)`: Indicates a regex pattern that the string field must match. Should be included if the value must
  be enforced at the SDK level.
- `@@threadSafe[(groupName)]`: Indicates that the attribute's accessor (getter and, if mutable, setter) can be called
  concurrently by the SDK and must be implemented in a thread-safe manner. The optional `groupName` parameter groups
  attributes (and methods) whose accessors can be called concurrently with each other. See the
  [method annotation](#method-annotations) section for the full semantics of `@@threadSafe`.

### Methods

Methods can be defined using the following syntax:

```
ReturnType methodName(param1: DataType1, param2: DataType2)
```

If a method has no return type, the return type should be `void`.
An example of a method with no return type:

```
void resetCache()
```

#### Variable arguments (varargs)

A method may declare its last parameter as a variable-arguments parameter by appending `...` to the parameter type.
This allows callers to pass an arbitrary number of arguments without explicitly constructing a collection.

Syntax:

```
methodName(paramName: DataType...)
```

Example:

```
addSigners(signers: Key...)
```

Rules:

- A method can have at most one varargs parameter.
- The varargs parameter must always be the last parameter of the method.
- A varargs parameter must not be annotated with `@@nullable`. If no arguments are passed, the implementation must
  treat it as an empty collection (consistent with
  the [Never define nullable collections](#never-define-nullable-collections)
  rule).
- Callers must also be able to pass an existing collection in place of individual arguments. The exact mechanism is
  language-specific (e.g., Go's `slice...` expansion, Java's array pass-through).
- Languages without native varargs support (e.g., Rust, C++) must define an idiomatic mapping in their respective
  best-practice guideline (e.g., `&[T]` or `impl IntoIterator<Item = T>` in Rust, `std::initializer_list<T>` or a
  variadic template in C++).

#### Method annotations

Method annotations can be used to provide additional information about methods.
The following annotations should be used:

- `@@async`: Indicates that the method is non-blocking. The actual work is deferred and executed asynchronously — the
  method returns immediately with a future or promise that represents the eventual result. In concrete terms, a method
  declared as `@@async ReturnType doWork()` returns a language-specific future/promise wrapping `ReturnType` (e.g.,
  `CompletableFuture<ReturnType>` in Java, `Promise<ReturnType>` in TypeScript, `Future<ReturnType>` in Rust).
  To make APIs easily usable by experts and newcomers, it makes sense to often provide a synchronous version of the
  method. An API definition in the meta-language does not need to add the synchronous version explicitly.
  Note: `@@async` applies only to method declarations. It is not used on function types (`function<...>`). Callbacks
  and futures are two distinct async patterns — a callback is invoked when work completes, while a future represents
  a pending result. Combining them (e.g., a callback that returns a future) should be avoided as it creates ambiguity
  about responsibility and completion semantics.
- `@@streaming`: Indicates that the method returns an asynchronous stream of items — a pull-based sequence the
  consumer drives at its own pace. The return type specifies the element type; use `streamResult<TYPE>` when
  per-item errors are possible (non-terminal), or a plain type when all errors are terminal. `@@throws` declares
  terminal stream-level errors (connection failure, authorization revoked, etc.). See [Streaming](#streaming) for
  full semantics, per-item error handling, retry, cancellation, and language mappings.
  Note: `@@streaming` and `@@async` are mutually exclusive — streaming already implies asynchronous item production.
- `@@static`: Indicates that the method belongs to the type itself and can be called without an instance.
  Typical use cases are factory methods and deserialization methods.
  For example, `@@static Transaction fromBytes(payload: bytes)`.
- `@@throws(error-type-a[, ...])`: Indicates that the method can throw an exception/error.
  The error-types should be stable identifiers, not transport-specific.
  Use lowercase-kebab for error identifiers (e.g., `not-found-error`, `parse-error`).
- `@@threadSafe[(groupName)]`: Indicates that the method can be called concurrently by the SDK and must be implemented
  in a thread-safe manner. The implementation is free to choose the appropriate strategy (locks, lock-free structures,
  actors, immutable copies, etc.). The optional `groupName` parameter groups methods that can be called concurrently
  with each other — the SDK may invoke any combination of methods in the same group at the same time. Without a group
  name, the method must be safe for concurrent calls on its own. In single-threaded environments (e.g., JavaScript) the
  annotation serves as documentation of the concurrent intent.

Example:

```
DataCache {
    @@threadSafe(cache)
    void updateCache(data: bytes)

    @@threadSafe(cache)
    bytes readCache()

    @@threadSafe
    void resetStats()
}
```

In this example, `updateCache` and `readCache` are in the `cache` group, meaning the SDK may call them concurrently
with each other. `resetStats` has no group and can only be called concurrently with itself. Note that even a single
method annotated with `@@threadSafe` (without a group) is meaningful — it indicates that the SDK may invoke that method
multiple times in parallel.

**Important:** `@@threadSafe` describes concurrency requirements between the SDK and the implementation of the annotated
method — it does not define thread-safety guarantees for end users of the SDK. User-facing thread-safety is addressed
through other means: preferring immutable types (
see [Prefer immutable fields and objects](#prefer-immutable-fields-and-objects))
ensures that most objects can be shared safely across threads without synchronization. For mutable types, getters and
setters are not individually annotated with `@@threadSafe` — it is the user's responsibility to synchronize access to
mutable objects if they choose to share them across threads.

#### Method Parameter annotations

The following attribute annotations can be used on method parameters: `@@nullable`, `@@min(value)`, `@@max(value)`,
`@@minLength(value)`, `@@maxLength(value)`, `@@pattern(regex)`

#### Method Return Types annotations

The following attribute annotations can be used on method return types: `@@nullable`

### Namespace

Namespaces can be used to group related data types and methods.
Namespace names must use lowerCamelCase identifiers. Dot notation can be used for sub-namespaces (for example,
`keys.io`). Hyphens are not allowed in namespace identifiers.
The following syntax should be used to define namespaces:

```
namespace transactions

    Transaction {
        @@immutable id: uuid
        @@immutable amount: double
        @@immutable date: dateTime
        @@immutable status: TransactionStatus

        @@async
        @@throws(not-found-error)
        @@nullable TransactionDetails fetchDetails(apiKey: string)
    }

    enum TransactionStatus {
        PENDING
        COMPLETED
        FAILED
    }
```

If a namespace depends on other namespaces, use the `requires` keyword to declare the dependencies:

```
namespace transactions
requires common, keys
...
 ```

#### Cross-namespace type references

When a type is used that is defined in a different namespace, it must be qualified with the namespace name using dot
notation: `namespace.Type`. Types within the same namespace can be referenced by their simple name.

Example:

```
namespace orders
requires common

// AccountId is defined in the `common` namespace, so it must be qualified
OrderTransaction {
    @@immutable account: common.AccountId
    @@immutable status: OrderStatus // same namespace, no prefix needed
}
```

This rule also applies to sub-namespaces. A type defined in `keys.io` must be referenced as `keys.io.TypeName` from
outside that namespace, and a type in the parent `keys` namespace must be referenced as `keys.TypeName` from within
`keys.io`.

### Constants

Constants can be defined within a namespace. A constant has always a fixed value and cannot be changed.

Example of a constant definition:

```
namespace transactions

constant MAX_TRANSACTIONS:int32 = 100
```

### Streaming

The meta-language supports declaring methods that return an asynchronous stream of items. A stream is a pull-based async
sequence that the consumer drives at its own pace. The SDK produces items (from a network subscription, gRPC stream,
paginated query, etc.) and the consumer pulls them one at a time using the language's idiomatic async iteration
construct.

#### The `@@streaming` annotation

The `@@streaming` annotation on a method declares that the method returns an asynchronous stream of items instead of a
single value. The return type specifies the element type of the stream.

```
@@streaming
TopicMessage subscribe(topicId: string)
```

The consumer iterates over the stream using the language's native async iteration pattern (e.g., `for await...of` in
TypeScript, `async for` in Python, `while let Some(x) = s.next().await` in Rust). Breaking out of the loop cancels the
stream.

`@@streaming` applies only to method declarations. It must not be combined with `@@async` — a streaming method already
implies asynchronous production of items. `@@async` is for single-value returns; `@@streaming` is for multi-value
returns.

#### Per-item error handling with `streamResult<TYPE>`

Streams use a dedicated result wrapper to separate per-item errors (non-terminal) from stream-level errors (terminal).
The `streamResult<TYPE>` data type represents a single item in the stream that is either a success value or an error:

| Data Type            | Description                                                      |
|----------------------|------------------------------------------------------------------|
| `streamResult<TYPE>` | A stream item that is either a success value of TYPE or an error |

A streaming method that may produce per-item errors uses `streamResult` as its return type:

```
@@streaming
streamResult<TopicMessage> subscribe(topicId: string)
```

Each language maps `streamResult<TYPE>` to its idiomatic result/either type:

| Language   | Mapping                                                                     |
|------------|-----------------------------------------------------------------------------|
| Rust       | `Result<T, E>` (standard library)                                           |
| Swift      | `Result<T, Error>` (standard library)                                       |
| Go         | `(T, error)` multiple return / `iter.Seq2[T, error]`                        |
| C++        | `std::expected<T, E>` (C++23) / `absl::StatusOr<T>`                         |
| Java       | Sealed `StreamItem<T>` interface                                            |
| TypeScript | Discriminated union `{ ok: true, value: T } \| { ok: false, error: Error }` |
| JavaScript | Plain object with `status` field                                            |
| Python     | `Success[T] \| Failure` dataclass union                                     |

When a streaming method does **not** use `streamResult` (i.e., the return type is a plain type), all errors are
stream-level and terminal — the stream ends and the error is delivered through the language's native error mechanism
(exception, panic, etc.).

#### Error levels in streams

Streams have two distinct error levels:

1. **Per-item errors** (non-terminal) — A single item in the stream is broken (parse error, invalid data, etc.). The
   stream continues. These are expressed by yielding an error variant of `streamResult`.
2. **Stream-level errors** (terminal) — The stream itself has failed (connection lost after retries exhausted, resource
   deleted, authorization revoked). The stream ends. These are delivered through the language's native error mechanism.

Consumer code follows a consistent two-level pattern:

```
try {
    for await (item in stream) {
        if (item is error) {
            // per-item: handle, skip, or log
        } else {
            process(item.value)
        }
    }
} catch (error) {
    // stream-level: fatal, stream is over
}
```

#### Retry strategy

Retry and reconnection logic for transient failures (network interruptions, gRPC stream resets) is an SDK-internal
concern. The SDK handles reconnection transparently — the consumer's iteration loop continues without interruption.

User-facing retry configuration is expressed as parameters on the streaming method or on the object that provides the
streaming method, not as control flow around the iteration loop:

```
@@streaming
streamResult<TopicMessage> subscribe(topicId: string, @@nullable retryPolicy: RetryPolicy)
```

If the SDK exhausts its retry budget, the failure surfaces as a terminal stream-level error.

#### Completion

A stream completes when the server closes the connection. Completion is **server-driven**: when an end condition
is set on the request (such as `endTime` or `endBlockNumber`), the server enforces it and closes the stream; the
SDK propagates that closure to the consumer. The SDK does not monitor item values client-side to detect end
conditions.

When no end condition is set, the stream runs indefinitely until the consumer cancels it or a terminal error occurs.

#### Cancellation

Cancellation is implicit: when the consumer stops iterating (breaks out of the loop, the enclosing scope ends, or the
stream handle is explicitly closed/dropped), the SDK must release the underlying resources (close the gRPC stream,
unsubscribe from the topic, etc.). Each language implements this through its native resource management:

- **Rust**: Dropping the `Stream`
- **Swift**: Task cancellation / leaving the `for await` scope
- **Go**: Cancelling the `context.Context`
- **Java**: `AutoCloseable.close()` / try-with-resources
- **TypeScript/JavaScript**: `break` / `return` from the `for await` loop
- **Python**: `break` / `return` from the `async for` loop, or async context manager
- **C++**: RAII destructor

#### Streaming annotation combinations

`@@streaming` can be combined with the following annotations:

- `@@throws(error-type)` — Declares terminal stream-level errors that the stream can produce.

`@@streaming` must **not** be combined with:

- `@@async` — Streaming already implies async item production.
- `@@static` — Streams are produced by instances, not types.

#### Full example

```
namespace topics
requires common

TopicSubscription {
    @@streaming
    @@throws(topic-deleted-error, authorization-error)
    streamResult<TopicMessage> subscribe(topicId: common.TopicId, @@nullable retryPolicy: RetryPolicy)
}

TopicMessage {
    @@immutable sequenceNumber: int64
    @@immutable payload: bytes
    @@immutable timestamp: zonedDateTime
}

RetryPolicy {
    @@immutable @@default(3) maxAttempts: int32
    @@immutable @@default("exponential") backoffStrategy: string
}
```

### Best practices and antipatterns

The following best practices and antipatterns should be followed when defining the API.

#### Prefer immutable fields and objects

Fields should be annotated with `@@immutable` by default. Mutable fields should only be introduced when there is a
clear, justified reason why the value must change after object creation. When designing a new type, start by making
every field immutable and only relax that constraint when mutability is genuinely required.

**Why immutability matters:**

- **Thread safety** – Immutable objects can be shared freely between threads without synchronization. Mutable state
  requires careful coordination (locks, concurrent collections, atomic operations) and is a common source of race
  conditions.
- **Predictability** – When an object cannot change after creation, any code that holds a reference to it can rely on
  its state staying consistent. This eliminates an entire class of bugs where state is modified unexpectedly by another
  part of the system.
- **Simpler `equals`/`hashCode`** – Immutable objects produce stable hash codes, so they can safely be used as keys in
  hash-based collections. Mutable fields in `equals`/`hashCode` can cause objects to "disappear" from sets and maps
  after mutation.
- **Easier testing and debugging** – Immutable objects are easier to reason about in tests because their state is
  fixed at construction time. There is no need to account for intermediate mutations.
- **Safe sharing and caching** – Immutable instances can be cached, reused, and returned from public APIs without
  defensive copying. Mutable objects must be copied every time they cross a trust boundary.

**Strategies for reducing mutability:**

- **Builder or factory pattern** – If an object needs many configuration values, collect them in a mutable builder and
  produce an immutable instance at the end. This avoids mutable setters on the final object.
- **Replace setters with new instances** – Instead of mutating a field in place, provide a method that returns a new
  instance with the updated value (sometimes called a "with" method, e.g., `withName(newName)`). This keeps the
  original object unchanged.
- **Separate identity from state** – If part of an object genuinely changes over time (e.g., a status), consider
  splitting the static identity (immutable) from the changing state. The changing state can live in a separate,
  narrowly scoped mutable object or be retrieved on demand rather than stored.
- **Event-driven updates** – Instead of mutating an existing object, emit a new event or message that represents the
  change. Consumers create a new state snapshot from the event rather than modifying an existing one.
- **Reconsider the lifecycle** – Sometimes mutability exists only because an object is created too early, before all its
  data is available. Deferring construction until all values are known often removes the need for mutability entirely.

When mutability is truly necessary, limit its scope: make only the specific fields that must change mutable, keep the
rest immutable, and document clearly why the mutable fields cannot be immutable.

#### Never define nullable collections

The data types `list`, `set`, and `map` should never be nullable.
It is best practice to return an empty collection instead of `null`.

Some languages (like Go) have custom semantics for the defined behavior (like `nil` in Go).
In that case the language-specific semantics should be used in the implementation.
Such special behavior must be documented in the best-practice guidelines for the specific language.

#### Avoid `ANY` as a standalone type

The `ANY` type is the API equivalent of Java's `Object` or TypeScript's `unknown`/`any` — it accepts any value but
gives up all compile-time type information. It exists in the meta-language because some legitimate use cases require
it (e.g., bridges to dynamic data, generic key/value stores, callbacks that forward opaque user data), but it should
be a last resort, not a default choice.

**Why standalone `ANY` is problematic:**

- **No type safety** — Callers cannot tell what value is expected or returned. The compiler/runtime cannot catch
  type mismatches at the API boundary.
- **Pushes complexity to callers** — Every consumer must check or cast the value before using it, leading to
  boilerplate and runtime errors.
- **Documentation drift** — The actual contract lives in prose ("this returns a `String` if X, otherwise an
  `Integer`"), which is invisible to tooling and easy to get wrong.
- **Inconsistent language semantics** — Java's `Object` boxes primitives, Rust requires explicit downcasting,
  TypeScript distinguishes `any` (unsafe) from `unknown` (safe). Behavior varies more than for typed APIs.

**Prefer instead:**

- **A generic type parameter (`$$T`)** — When the caller knows the type at the call site, expose it as a generic
  parameter so the type flows through the API.
- **A concrete base type or interface** — Model the common contract explicitly (e.g., `ContractParam` instead of
  `ANY` for smart-contract arguments).
- **A sealed/tagged union via `@@oneOf`** — When the value is one of a finite set of known types, model it as a
  union so the consumer can exhaustively handle each case.
- **`bytes` plus a documented schema** — For fully opaque payloads (serialized data, blobs), `bytes` is more honest
  than `ANY` and avoids accidental coupling.

Note that this guidance applies to `ANY` used as a standalone type. `ANY` as a wildcard type argument (e.g.,
`ContractParam<ANY>`) serves a different purpose — see
[Wildcard type arguments](#wildcard-type-arguments) — and is not subject to the same warning.

When `ANY` is genuinely the right choice, document the expected shape and the conditions under which different
runtime types may appear so consumers do not have to guess.

### Naming conventions

To keep the API surface consistent and predictable, use the following naming rules:

- Types (complex types, interfaces, enums): PascalCase (e.g., UserProfile, Fetchable).
- Fields and methods: lowerCamelCase (e.g., employeeNumber, fetchById).
- Enum values: UPPER_SNAKE_CASE (e.g., PENDING, COMPLETED).
- Namespace names: lowerCamelCase (e.g., transactions).
- Error identifiers (in `@@throws`): lowercase-kebab-case (e.g., not-found-error, parse-error).
- Constant names: UPPER_SNAKE_CASE (e.g., MAX_TRANSACTIONS).

## Questions & Comments

