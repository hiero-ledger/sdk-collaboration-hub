# API Guideline

This document outlines the best practices and conventions for designing and implementing APIs for the SDKs.
Following these guidelines will help ensure that APIs are consistent, easy to use, and maintainable.
Next to that the guidelines defines a syntax for documenting the APIs in a language-agnostic way for proposals.

## Language Agnostic API Documentation Syntax

The [proposals](../proposals) folder contains design documents of new features or changes for our SDKs.
Each proposal should include a section that documents the API in a language-agnostic way.

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

### Enumerations

Enumerations can be defined using the following syntax:
```
enum EnumName {
    VALUE1
    VALUE2
}
```

### Attribute annotations

Attribute annotations can be used to provide additional information about attributes in complex data types.
The following annotations should be used:
- `@immutable`: Indicates that the field is immutable and cannot be changed after creation.
- `@optional`: Indicates that the field is optional and can be null or undefined.
- `@default(value)`: Indicates that the field has a default value.
- `@min(value)`: Indicates the minimum value for numeric fields. Should be included if the value must be enforced at the SDK level.
- `@max(value)`: Indicates the maximum value for numeric fields. Should be included if the value must be enforced at the SDK level.
- `@minLength(value)`: Indicates the minimum length for string fields. Should be included if the value must be enforced at the SDK level.
- `@maxLength(value)`: Indicates the maximum length for string fields. Should be included if the value must be enforced at the SDK level.
- `@pattern(regex)`: Indicates a regex pattern that the string field must match. Should be included if the value must be enforced at the SDK level.

### Method annotations

Method annotations can be used to provide additional information about methods.
The following annotations should be used:
- `@async`: Indicates that the method is asynchronous and returns a promise or future.
- `@throws(error-type)`: Indicates that the method can throw an exception/error.

### Method definitions

Methods can be defined using the following syntax:

```
ReturnType methodName(param1: DataType1, param2: DataType2)
```

The following attribute annotations can be used on method parameters: `@optional`, `@min(value)`, `@max(value)`, `@minLength(value)`, `@maxLength(value)`, `@pattern(regex)`

The following attribute annotations can be used on method return types: `@immutable`, `@optional`

A complete method definition example:
```
@async
@throws(parse-error)
@optional ResponseType fetchData(id: uint64, @optional filter: string)
```

### Complex data types

Complex data types can be defined using the basic data types and other complex data types.
The following syntax should be used to define complex data types:
```
DataTypeName {
    fieldName1: DataType1
    fieldName2: DataType2
    ReturnType methodName(param1: DataType1, param2: DataType2)
}
```
A complete complex data type example:
```
User {
    @immutable id: uuid
    @minLength(3) @maxLength(100) email: string
    @optional @min(0) @max(120) age: uint8
    
    @async
    @throws(not-found-error)
    @optional UserProfile fetchProfile(apiKey: string)
}
```

### Inheritance

Complex data types can inherit from other complex data types to reuse fields and methods.

- Syntax: Use `extends` to declare a child type.
- Only single inheritance / one direct parent type is supported.
- Multiple inheritance and mixins are not supported.
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

### Namespace

Namespaces can be used to group related data types and methods.
The following syntax should be used to define namespaces:
```
namespace transactions

    Transaction {
        @immutable id: uuid
        @immutable amount: float
        @immutable date: dateTime
        @immutable status: TransactionStatus

        @async
        @throws(not-found-error)
        @optional TransactionDetails fetchDetails(apiKey: string)
    }

    enum TransactionStatus {
        PENDING
        COMPLETED
        FAILED
    }
```
