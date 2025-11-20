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

## Collections

The public API must never return `null` for collections. Instead, an empty collection must be returned.
The most easy way to achieve this is to use one of the static `java.util.Collections.emptyList()`/`java.util.Collections.emptySet()`/`java.util.Collections.emptyMap()`/`List.of()`/`Set.of()`/`Map.of()` factory methods.
Since the API must never return `null` for collections it never makes sense to wrap a collection in a `java.util.Optional` in the public API.

The public API must always use `java.util.List<TYPE>`, `java.util.Set<TYPE>`, and `java.util.Map<KEY, VALUE>` and never expose concrete implementations.
Collections must be immutable wherever possible.
This is not directly related to the `@@immutable` annotation but is a general rule.
In general the factory methods of the `java.util.Collections` class (`unmodifiableList(...)`/`unmodifiableMap(...)`/`unmodifiableSet(...)`) or the direct factory methods in the collection interfaces (`List.of(...)`, `List.copyOf(...)`, `Set.of(...)`, `Set.copyOf(...)`, `Map.of(...)`, `Map.copyOf(...)`) should be used.
The mentioned methods have slightly different functionality since some create a view of the collection while others create a copy of the collection.
Here it is important to understand the difference between the two and what the correct choice is.
Currently, the meta-language does not specify if a collection is a view or a copy.
In most cases it makes most sense to return a view.

Examples of how immutable collections can be implemented in Java:

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Team {

    private final List<String> names = new CopyOnWriteArrayList<>();

    // Returning an unmodifiable list
    public List<String> getSomeNames() {
        return List.of("John", "Jane");
    }

    // Returning an unmodifiable view of the inner list
    public List<String> getNames() {
        return Collections.unmodifiableList(names);
    }

    //Returning an unmodifiable copy of the inner list
    public List<String> getAllNames() {
        return List.copyOf(names);
    }

}
```

If a collection is passed as a parameter to a constructor and stored in a field, it must be copied before being stored.
The following example shows how to do this:

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class Team {

    private final List<String> names;

    public Team(@NonNull final List<String> names) {
        Objects.requireNonNull(names, "names must not be null");
        this.names = new CopyOnWriteArrayList<>(names);
    }

}
```

If a full collection can be replaced at runtime (no @@immutable annotation defined in meta-language) the collection should newer be recreated but the full content should be replaced.
The following example shows how to do this:

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class Team {

    private final List<String> names;

    public Team() {
        this.names = new CopyOnWriteArrayList<>();
    }
    
    public void setNames(@NonNull final List<String> names) {
        Objects.requireNonNull(names, "names must not be null");
        this.names.clear();
        this.names.addAll(names);
    }

}
```

Since we never know how the API will be used, it is important to always consider that functionalities can be called in parallel.
This means that the collection can be modified while it is being iterated over.
This can happen if the collection is used in multiple functions that are exposed to the public API (even by sub callers). 
Therefore, it is important to use `java.util.concurrent.CopyOnWriteArrayList` or `java.util.concurrent.CopyOnWriteArraySet` instead of `java.util.ArrayList` or `java.util.HashSet` for mutable instances of `java.util.List` and `java.util.Set`.
The copy-on-write functionality ensures that the collection is not modified while it is being iterated over.
By doing so we can avoid the risk of concurrent modification exceptions at runtime.
For `java.util.Map`, `java.util.concurrent.ConcurrentHashMap` must be used for mutable instances.
The following example shows how to do this:

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class Team {

    private final List<String> names;

    public Team() {
        this.names = new CopyOnWriteArrayList<>();
    }
    
    @NonNull
    public List<String> getNames() {
        return Collections.unmodifiableList(names);
    }
    
    public void setNames(@NonNull final List<String> names) {
        Objects.requireNonNull(names, "names must not be null");
        this.names.clear();
        this.names.addAll(names);
    }
    
    public void addName(@NonNull final String name) {
        Objects.requireNonNull(name, "name must not be null");
        names.add(name);
    }
    
    public void removeName(@NonNull final String name) {
        Objects.requireNonNull(name, "name must not be null");
        names.remove(name);
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

## Implementation of Attribute annotations

Attribute annotations that are defined in the language agnostic specification should be implemented in Java in the following ways.

The handling of `@@immutable` is defined in the ['Immutable Objects' section](#immutable-objects).
The handling of `@@nullable` is defined in the ['Null handling' section](#null-handling).

The `@@default(value)` annotation defines a default value for a field. In Java that will always be set in the constructor or directly in the field declaration.
The following example shows how to implement this annotation:

```java
import java.time.LocalDate;

public class Example {

    // example of an attribute that is defined as @@immutable @@default("John") name:string
    // note that the default value is set in the constructor
    private final String name;

    // example of an attribute that is defined as @@immutable @@default(NOW) name:date
    // note that the default value is set in the field declaration
    private final LocalDate birthdate = LocalDate.now();

    // example of an attribute that is defined as @@default(20) age:int32
    // note that the default value is set in the constructor
    private int age;

    // example of an attribute that is defined as @@default(true) active:bool
    // note that the default value is set in the constructor
    private boolean active = true;

    public Example() {
        name = "John";
        age = 20;
    }

    // the @@default(value) does not forbid to initialize the attribute with different values, independent of the @@immutable annotation
    public Example(final String name, final int age) {
       this.name = name;
       this.age = age;
    }

    // attributes that are not annotated with @@immutable can be modified even if they are specified with @@default(value)
    public void setAge(final int age) {
        this.age = age;
    }

    // attributes that are not annotated with @@immutable can be modified even if they are specified with @@default(value)
    public void setActive(final boolean active) {
        this.active = active;
    }
}
```

The `@@min(value)`, `@@max(value)`, `@@minLength(value)`, `@@maxLength(value)`, and `@@pattern(regex)` annotations are all handled in the same way: Checks based on the value must be added to the constructor and setter methods.
Attributes annotated with any of those annotations must never be set directly in the field declaration.
The following example shows how to implement this annotation:

```java
import java.time.LocalDate;

public class Example {

    // example of an attribute that is defined as @@minLength(1) name:string
    private final String name;

    // example of an attribute that is defined as @@min(0) age:int32
    private int age;
    
    //instead of implementing the checks 2 times the setter method can be called directly in the constructor
    public Example(final String name, final int age) {
       setName(name);
       setAge(age);
    }

    public void setAge(final int age) {
        if(age < 0) {
            throw new IllegalArgumentException("age must be minimum 0");
        }
        this.age = age;
    }
    
    public void setName(final String name) {
        if(name.length() < 1) {
            throw new IllegalArgumentException("name must be minimum 1 character long");
        }
        this.name = name;
    }
}
```