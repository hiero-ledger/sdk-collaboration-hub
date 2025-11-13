# Java API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete Java patterns,
conventions, and ready‑to‑copy code templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

## Type Mapping

Use the following canonical mappings when turning meta types into Java:

| Generic Type      | Java Type     | Notes                |
|-------------------|---------------|------------------------------------------------|
| `string`          | `java.lang.String` | -                                      |
| `intX`            | `byte`/`short`/`int`/`long`/`java.lang.Byte`/`java.lang.Short`/`java.lang.Integer`/`java.lang.Long` | -                            |
| `uintX`           | `byte`/`short`/`int`/`long`/`java.lang.Byte`/`java.lang.Short`/`java.lang.Integer`/`java.lang.Long` | -                            |
| `double`          | `double`/`java.lang.Double` | -        |
| `decimal`         | `java.math.BigDecimal`         | -                    |
| `bool`            |  `boolean`/`java.lang.Boolean`        | -                                               |
| `bytes`           |  `byte[]`       | -                                       |
| `list<TYPE>`      |  `java.util.List<TYPE>`        | -                         |
| `set<TYPE>`       |  `java.util.Set<TYPE>`     | -                          |
| `map<KEY, VALUE>` |  `java.util.Map<KEY, VALUE>`        | -                               |
| `date`            | `java.time.LocalDate`         | -                       |
| `time`            |  `java.time.LocalTime`         | -  |
| `dateTime`        |  `java.time.LocalDateTime`         | - |
| `zonedDateTime`   |  `java.time.ZonedDateTime`        | -    |

## Immutable Objects

If a non-abstract type and all the types its extends are only contain fields annotated with `@@immutable`, the type should be declared as a Java `record`.
The following example shows how such type can look like:

```
// Definition of the Person type in the language agnostic specification
Person {
    @@immutable @@nullable name:string
    @@immutable @@nullable age:int32
}

// Implementation of the Person type in Java
record Person(String name, int age) {}
```

## Null handling

In Java we use the `org.jspecify:jspecify` library to annotate nullability of types.
The 2 annotations `org.jspecify.annotations.NonNull` and `org.jspecify.annotations.Nullable` are used to annotate nullability of types.
All non-primitive constructor parameters, method parameters and method return values must be annotated with one of these annotations.

For fields and method parameters that are not annotated with `@@nullable` in the language agnostic specification, a concrete check must be performed in Java to ensure that the parameter is not null.
Here `java.util.Objects.requireNonNull(param)` or better `java.util.Objects.requireNonNull(param, msg)` must be used.

For the generic types `intX`, `uintX`, `double`, and `bool` wrapper classes
(`java.lang.Byte`/`java.lang.Short`/`java.lang.Integer`/`java.lang.Long`/`java.lang.Integer`,`java.lang.Double`, and `java.lang.Boolean`)
must be used if the parameter is annotated with `@@nullable` in the language agnostic specification.
Otherwise the primitive type must be used.

### Null handling of mutable and nullable fields

If a field is not annotated with `@@immutable` and `@@nullable` in the language agnostic specification, the Java getter and setter must be annotated with `org.jspecify.annotations.Nullable`.
Let's assume we have the following language agnostic specification:

```
class Example {
    @@nullable name:string    
}
```

A class can be implemented as follows:

```java
public class Example {
    private String name;
    
    public void setName(@Nullable final String name) {
        this.name = name;   
    }
    
    @Nullable
    public String getName() {
        return name;
    }
}
```

### Null handling of mutable and not nullable fields

If a field parameter is not annotated with `@@immutable` or `@@nullable` in the language agnostic specification, the Java getter and setter must be annotated with `org.jspecify.annotations.NonNull`.

Let's assume we have the following language agnostic specification:

```
class Example {
    @@immutable name:string    
}
```

The given sample can be implemented as a class.
A class can be implemented as follows:

```java
public class Example {
    
    private String name;
    
    public Example(@NonNull final String name) {
       this.name = Objects.requireNonNull(name, "name must not be null");  
    }
    
    public void setName(@NonNull final String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");  
    }
        
    @NonNull
    public String getName() {
        return name;
    }
}
```

### Null handling of immutable and nullable fields

If a field is annotated with `@@immutable` and `@@nullable` in the language agnostic specification, the Java parameter of the constructor must be annotated with `org.jspecify.annotations.Nullable`.
Let's assume we have the following language agnostic specification:

```
class Example {
    @@immutable @@nullable name:string    
}
```

The given sample can be implemented as a class or record in Java.
A class can be implemented as follows:

```java
public class Example {
    private final String name;
    
    public Example(@Nullable final String name) {
        this.name = name;
    }
}
```

A record can be implemented as follows:

```java
public record Example(@Nullable final String name) {
}
```

### Null handling of immutable and not nullable fields

If a field parameter is annotated with `@@immutable` but not with `@@nullable` in the language agnostic specification, the Java parameter of the constructor must be annotated with `org.jspecify.annotations.NonNull`.

Let's assume we have the following language agnostic specification:

```
class Example {
    @@immutable name:string    
}
```

The given sample can be implemented as a class or record in Java.
A class can be implemented as follows:

```java
public class Example {
    
    private final String name;
    
    public Example(@NonNull final String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");   
    }
}
```

A record can be implemented as follows:

```java
public record Example(@NonNull final String name) {
    
    public Example {
        Objects.requireNonNull(name, "name must not be null");   
    }
    
}
``` 
 
### Null handling of nullable method parameter

TODO

### Null handling of not nullable method parameter

TODO

### Null handling of nullable method return value

TODO

### Null handling of not nullable method return value

TODO


