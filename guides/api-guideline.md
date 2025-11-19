# API Guideline

This document outlines the best practices and conventions for designing and implementing APIs for the SDKs.
Following these guidelines will help ensure that APIs are consistent, easy to use, and maintainable.
Next to that the guidelines defines a syntax for documenting the APIs in a language-agnostic way for proposals.

The [proposals](../proposals) folder contains design documents of new features or changes for our SDKs.
Each proposal should include a section that documents the API in a language-agnostic way by using the guidelines defined in this document.

## Syntax

The following syntax should be used to document the APIs in a language-agnostic way

### Basic data types

The following basic data types should be used in the API documentation.

| Data Type         | Description                                                   |
|-------------------|---------------------------------------------------------------|
| `string`          | A sequence of characters                                      |
| `intX`            | A signed integer of X bits                                    |
| `uintX`           | An unsigned integer of X bits                                 |
| `double`          | A native floating-point number in 64-bit base-2 format        |
| `decimal`         | A decimal number with arbitrary precision                     |
| `bool`            | A boolean value                                               |
| `bytes`           | A sequence of bytes                                           |
| `list<TYPE>`      | A list of elements of type TYPE                               |
| `set<TYPE>`       | A set of elements of type TYPE                                |
| `map<KEY, VALUE>` | A map of elements of type TYPE                                |
| `date`            | A date value (ISO 8601 calendar date)                         |
| `time`            | A time value without date or timezone (nanosecond precision)  |
| `dateTime`        | A date and time value without timezone (nanosecond precision) |
| `zonedDateTime`   | A date and time value with timezone (nanosecond precision)    |

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
- `@@oneOf(field1, field2[, ...])`: Exactly one of the referenced fields can be non-null/non-undefined at any given time.
- `@@oneOrNoneOf(field1, field2[, ...])`: Exactly one of the referenced fields can be non-null/non-undefined at any given time or all fields are null/undefined.
- `@@finalType`: Indicates that the type is final and cannot be extended.

Rules and recommendations for `@@oneOf`:
- All listed fields must be declared with `@@nullable`
- None or all listed fields must be declared with `@@immutable`.
- If the fields are not `@@immutable` and a field is set, an SDK must unset all other fields
- If the fields are not `@@immutable` and the currently set field is unset, an SDK must throw an error.
- None or exactly one of the listed fields must be annotated by `@@default(value)`
- If the fields are not `@@immutable` and none is annotated by `@@default(value)` at least one must be set by the constructor to not end in an invalid state. 

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

A Generic types may have multiple parameters are written in angle brackets: `TypeName<$$T1, $$T2, ...>`.

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

[Attributes](#attributes) can be added to enumerations. Attributes added to enumerations must be immutable (annotated with `@@immutable`).

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

#### Attribute annotations

Attribute annotations can be used to provide additional information about attributes in complex data types.
The following annotations should be used:
- `@@immutable`: Indicates that the field is immutable and cannot be changed after creation.
- `@@nullable`: Indicates that the field can be null or undefined (language specific).
- `@@default(value)`: Indicates that the field has a default value.
- `@@min(value)`: Indicates the minimum value for numeric fields. Should be included if the value must be enforced at the SDK level.
- `@@max(value)`: Indicates the maximum value for numeric fields. Should be included if the value must be enforced at the SDK level.
- `@@minLength(value)`: Indicates the minimum length for string fields. Should be included if the value must be enforced at the SDK level.
- `@@maxLength(value)`: Indicates the maximum length for string fields. Should be included if the value must be enforced at the SDK level.
- `@@pattern(regex)`: Indicates a regex pattern that the string field must match. Should be included if the value must be enforced at the SDK level.

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
- `@@throws(error-type-a[, ...])`: Indicates that the method can throw an exception/error.
  The error-types should be stable identifiers, not transport-specific.
  Use lowercase-kebab for error identifiers (e.g., `not-found-error`, `parse-error`).

#### Method Parameter annotations

The following attribute annotations can be used on method parameters: `@@nullable`, `@@min(value)`, `@@max(value)`, `@@minLength(value)`, `@@maxLength(value)`, `@@pattern(regex)`

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

### Naming conventions

To keep the API surface consistent and predictable, use the following naming rules:

- Types (complex types, interfaces, enums, namespaces): PascalCase (e.g., UserProfile, Fetchable).
- Fields and methods: lowerCamelCase (e.g., employeeNumber, fetchById).
- Enum values: UPPER_SNAKE_CASE (e.g., PENDING, COMPLETED).
- Namespace names: lowerCamelCase (e.g., transactions).
- Error identifiers (in `@@throws`): lowercase-kebab-case (e.g., not-found-error, parse-error).
