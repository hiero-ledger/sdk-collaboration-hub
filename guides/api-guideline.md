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

| Data Type         | Description                                                           |
|-------------------|-----------------------------------------------------------------------|
| `string`          | A sequence of characters                                              |
| `intX`            | A signed integer of X bits (8 <= X <= 256)                            |
| `uintX`           | An unsigned integer of X bits (8 <= X <= 256)                         |
| `double`          | A native floating-point number in 64-bit base-2 format                |
| `decimal`         | A decimal number with arbitrary precision                             |
| `bool`            | A boolean value                                                       |
| `bytes`           | A sequence of bytes                                                   |
| `list<TYPE>`      | A list of elements of type TYPE                                       |
| `set<TYPE>`       | A set of elements of type TYPE                                        |
| `map<KEY, VALUE>` | A map of elements of type TYPE                                        |
| `type`            | A type identity that can be used to specify a complex type at runtime |
| `date`            | A date value (ISO 8601 calendar date)                                 |
| `time`            | A time value without date or timezone (nanosecond precision)          |
| `dateTime`        | A date and time value without timezone (nanosecond precision)         |
| `zonedDateTime`   | A date and time value with timezone (nanosecond precision)            |

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
A complexe type that extends a generic type can use the type parameter in its own definition.
A concrete example of the syntax looks like this:

```
CarFactory extends Factory<Car> {
}
```

In that example, the `CarFactory` provides a `create` method that returns a `Car`.
`Car` must be a concrete complexe type (not a generic type parameter).

Generic types with multiple parameters are written in angle brackets: `TypeName<$$T1, $$T2, ...>`.

To make a generic type more concrete, the `extends` keyword can be used to constrain the type parameter:

```
abstraction FruitFactory<$$Product extends Fruit> {
    $$Product create()
}
```

In the given example, the `FruitFactory` provides a `create` method that returns a `Fruit`.
`Fruit` must be a concrete complexe type (not a generic type parameter).

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
Here the access is implemented in a language-specific way, but in most languages it is equivalent to a getter method.
A mutable attribute can be changed at any time.
Here a language-specific way to change an attribute is implemented.
In most languages this is equivalent to a setter method.

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

#### Method annotations

Method annotations can be used to provide additional information about methods.
The following annotations should be used:

- `@@async`: Indicates that the method is asynchronous and returns a promise or future.
  To make APIs easily useable by experts and newcomers, it makes sense to always provide a synchronous version of the
  method.
  An API definition in the meta-language does not need to add the synchronous version explicitly.
- `@@throws(error-type-a[, ...])`: Indicates that the method can throw an exception/error.
  The error-types should be stable identifiers, not transport-specific.
  Use lowercase-kebab for error identifiers (e.g., `not-found-error`, `parse-error`).

#### Method Parameter annotations

The following attribute annotations can be used on method parameters: `@@nullable`, `@@min(value)`, `@@max(value)`,
`@@minLength(value)`, `@@maxLength(value)`, `@@pattern(regex)`

#### Method Return Types annotations

The following attribute annotations can be used on method return types: `@@nullable`

### Namespace

Namespaces can be used to group related data types and methods.
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

### Constants

Constants can be defined within a namespace. A constant has always a fixed value and cannot be changed.

Example of a constant definition:

```
namespace transactions

constant MAX_TRANSACTIONS:int32 = 100
```

### Best practices and antipatterns

The following best practices and antipattern should be followed when defining the API.

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

Some languages (like GO) have custom semantics for the defined behavior (like `nil` in GO).
In that case the language-specific semantics should be used in the implementation.
Such special behavior must be documented in the best-practice guidelines for the specific language.

### Naming conventions

To keep the API surface consistent and predictable, use the following naming rules:

- Types (complex types, interfaces, enums, namespaces): PascalCase (e.g., UserProfile, Fetchable).
- Fields and methods: lowerCamelCase (e.g., employeeNumber, fetchById).
- Enum values: UPPER_SNAKE_CASE (e.g., PENDING, COMPLETED).
- Namespace names: lowerCamelCase (e.g., transactions).
- Error identifiers (in `@@throws`): lowercase-kebab-case (e.g., not-found-error, parse-error).
- Constant names: UPPER_SNAKE_CASE (e.g., MAX_TRANSACTIONS).

## Questions & Comments

