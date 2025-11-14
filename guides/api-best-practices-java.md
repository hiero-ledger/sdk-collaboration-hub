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

If a non-abstract type and all the types it extends only contain fields annotated with `@@immutable`, the type should be declared as a Java `record`.
The following example gives an example of such a type:

```
// Definition of the Person type in the language agnostic specification
Person {
    @@immutable @@nullable name:string
    @@immutable @@nullable age:int32
}
```

```java
// Implementation of the Person type in Java
public record Person(@Nullable String name, @Nullable int age) {} // Usage of the @Nullable annotation is described in the following chapter
```

If only some fields are annotated with `@@immutable`, the type should be declared as a Java `class`.
Here all fields that are not annotated with `@@immutable` must be declared as `final`, set in the constructor and only accessible via getters.
The following example gives an example of such a type:

```
// Definition of the Person type in the language agnostic specification
Person {
@@immutable name:string
age:int32
}
```

```java
public class Person {

    private final String name;
    
    private int age;

    public Person(@NonNull final String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }
    
    @NonNull
    public String getName() {
        return name;
    }
    
    public int getAge() {
        return age;
    }
    
    public void setAge(final int age) {
        this.age = age;
    }
}
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

### Null handling of nullable fields

If a field is annotated with `@@nullable` in the language agnostic specification, the Java getter and setter must be annotated with `org.jspecify.annotations.Nullable`.
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

### Null handling of not nullable fields

If a field parameter is not annotated with `@@nullable` in the language agnostic specification, the Java getter and setter must be annotated with `org.jspecify.annotations.NonNull`.
Next to that the field must be initialized in the constructor if it is not annotated by `@@default(value)` in the language agnostic specification.

Let's assume we have the following language agnostic specification:

```
class Example {
    name:string    
}
```

The given sample can be implemented as a class.
A class can be implemented as follows:

```java
public class Example {
    
    private String name;
    
    public Example(@NonNull final String name) {
        setName(name);
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

### Null handling of immutable fields

If a field is annotated with `@@immutable` and `@@nullable` in the language agnostic specification and the Java implementation is a record, the Java parameter of the `record` must be annotated with `org.jspecify.annotations.NonNull` and checked in the compact constructor.
Let's assume we have the following language agnostic specification:

```
class Example {
    @@immutable name:string    
}
```

The given sample can be implemented as a class or record in Java.
A class can be implemented as follows:

```java
public record Example(@NonNull final String name) {

    public Example {
        Objects.requireNonNull(name, "name must not be null");
    }

}
```

If a field is annotated with `@@immutable` and `@@nullable` in the language agnostic specification and the Java implementation is a `class`, the Java parameter of the constructor must be annotated with `org.jspecify.annotations.NonNull` and checked in the constructor.
Let's assume we have the following language agnostic specification:

```
class Example {
    @@immutable name:string
    //some other nullable fields    
}
```

The given sample can be implemented as a class or record in Java.
A class can be implemented as follows:

```java
public class Example {

    private final String name;

    //other non-final fields
    
    public Example(@NonNull final String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }
    
    @NonNull
    public String getName() {
        return name;
    }
    
    //other getters and setters
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
 
### Null handling method parameters

TODO

### Null handling of method return value

TODO
