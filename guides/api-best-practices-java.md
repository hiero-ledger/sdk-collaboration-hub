# Java API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete Java patterns,
conventions, and ready‑to‑copy code templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

## Type Mapping

Use the following canonical mappings when turning meta types into Java:

| Generic Type      | Java Type                                                                                                                          | Notes                                                                            |
|-------------------|------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| `string`          | `java.lang.String`                                                                                                                 | -                                                                                |
| `intX`            | `byte`, `short`, `int`, `long`, `java.lang.Byte`, `java.lang.Short`, `java.lang.Integer`, `java.lang.Long`, `java.math.BigInteger` | For all definitions that are not `@@nullable` the primitive types should be used |
| `uintX`           | `byte`, `short`, `int`, `long`, `java.lang.Byte`, `java.lang.Short`, `java.lang.Integer`, `java.lang.Long`, `java.math.BigInteger` | For all definitions that are not `@@nullable` the primitive types should be used |
| `double`          | `double`/`java.lang.Double`                                                                                                        | For all definitions that are not `@@nullable` the primitive types should be used |
| `decimal`         | `java.math.BigDecimal`                                                                                                             | -                                                                                |
| `bool`            | `boolean`/`java.lang.Boolean`                                                                                                      | For all definitions that are not `@@nullable` the primitive types should be used |
| `bytes`           | `byte[]`/`java.lang.Byte[]`                                                                                                        | For all definitions that are not `@@nullable` the primitive types should be used |
| `list<TYPE>`      | `java.util.List<TYPE>`                                                                                                             | lists in the public API should always be immutable                               |
| `set<TYPE>`       | `java.util.Set<TYPE>`                                                                                                              | sets in the public API should always be immutable                                |
| `map<KEY, VALUE>` | `java.util.Map<KEY, VALUE>`                                                                                                        | maps in the public API should always be immutable                                |
| `date`            | `java.time.LocalDate`                                                                                                              | -                                                                                |
| `time`            | `java.time.LocalTime`                                                                                                              | -                                                                                |
| `dateTime`        | `java.time.LocalDateTime`                                                                                                          | -                                                                                |
| `zonedDateTime`   | `java.time.ZonedDateTime`                                                                                                          | -                                                                                |

### Numeric Types

The following table shows the mapping of numeric types between the meta-language and Java.
For each numeric Java type a maximum numeric type is defined.

| Max Numeric Type | Java Type                  |
|------------------|----------------------------|
| `int8`           | `byte`, `java.lang.Byte`   |
| `int16`          | `short`, `java.lang.Short` |
| `int32`          | `int`, `java.lang.Integer` |
| `int64`          | `long`, `java.lang.Long`   |
| `int256`         | `java.math.BigInteger`     |
| `uint8`          | `byte`, `java.lang.Byte`   |
| `uint16`         | `short`, `java.lang.Short` |
| `uint32`         | `int`, `java.lang.Integer` |
| `uint64`         | `long`, `java.lang.Long`   |
| `uint256`        | `java.math.BigInteger`     |

## Immutable Objects

If a non-abstract type and all the types it extends only contain fields annotated with `@@immutable`, the type should be
declared as a Java `record`.
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
public record Person(@Nullable String name, @Nullable int age) {
} // Usage of the @Nullable annotation is described in the following chapter
```

If only some fields are annotated with `@@immutable`, the type should be declared as a Java `class`.
Here all fields that are not annotated with `@@immutable` must be declared as `final`, set in the constructor and only
accessible via getters.
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

## Complex Types

The meta-language can be used to define complex types.
Here the meta-language makes a difference between abstract types and non-abstract types.
The meta-language does not define if anything should be an interface or abstract class since some languages do not
support this.
Therefore, there is no fix rule that defines if something must be created as interface or abstract class in Java.
Especially with default methods, a lot can be done with interfaces.
Abstract classes will make sense if constructors should be enforced or methods should be defined as final.

Next to that records should be used wherever possible.
If a non-abstract type in the meta-language only contains attributes annotated with `@@immutable`, the type must be
declared as a Java `record`.

## Collections

The public API must never return `null` for collections. Instead, an empty collection must be returned.
The most easy way to achieve this is to use one of the static `java.util.Collections.emptyList()`/
`java.util.Collections.emptySet()`/`java.util.Collections.emptyMap()`/`List.of()`/`Set.of()`/`Map.of()` factory methods.
Since the API must never return `null` for collections it never makes sense to wrap a collection in a
`java.util.Optional` in the public API.

The public API must always use `java.util.List<TYPE>`, `java.util.Set<TYPE>`, and `java.util.Map<KEY, VALUE>` and never
expose concrete implementations.
Collections must be immutable wherever possible.
This is not directly related to the `@@immutable` annotation but is a general rule.
In general the factory methods of the `java.util.Collections` class (`unmodifiableList(...)`/`unmodifiableMap(...)`/
`unmodifiableSet(...)`) or the direct factory methods in the collection interfaces (`List.of(...)`, `List.copyOf(...)`,
`Set.of(...)`, `Set.copyOf(...)`, `Map.of(...)`, `Map.copyOf(...)`) should be used.
The mentioned methods have slightly different functionality since some create a view of the collection while others
create a copy of the collection.
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

If a full collection can be replaced at runtime (no @@immutable annotation defined in meta-language) the collection
should never be recreated but the full content should be replaced.
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

Since we never know how the API will be used, it is important to always consider that functionalities can be called in
parallel.
This means that the collection can be modified while it is being iterated over.
This can happen if the collection is used in multiple functions that are exposed to the public API (even by sub
callers).
Therefore, it is important to use `java.util.concurrent.CopyOnWriteArrayList` or
`java.util.concurrent.CopyOnWriteArraySet` instead of `java.util.ArrayList` or `java.util.HashSet` for mutable instances
of `java.util.List` and `java.util.Set`.
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
The 2 annotations `org.jspecify.annotations.NonNull` and `org.jspecify.annotations.Nullable` are used to annotate
nullability of types.
All non-primitive constructor parameters, method parameters and method return values of the public API must be annotated
with one of these annotations.
The annotations must be used consistently throughout the full implementation.

For the generic types `intX`, `uintX`, `double`, and `bool` wrapper classes
(`java.lang.Byte`/`java.lang.Short`/`java.lang.Integer`/`java.lang.Long`/`java.lang.Integer`,`java.lang.Double`, and
`java.lang.Boolean`)
must be used if the parameter is annotated with `@@nullable` in the language agnostic specification.
Otherwise the primitive type must be used.

For fields and method parameters that are not annotated with `@@nullable` in the language agnostic specification, a
concrete check must be performed in Java to ensure that the parameter is not null.
The check must be performed if the value is stored or accessed directly.
Here `java.util.Objects.requireNonNull(param, msg)` must be used.
The `msg` parameter must be defined as `NAME must not be null`.
We know that Java has some better `NullPointerExceptions` that can provide the name of a null parameter dynamically at
runtime.
However, especially if you only set a field in a constructor a check at access time will happen way later.
We want to avoid this situation and throw a `NullPointerException` as soon as possible.
Therefore, we defined to use `Objects.requireNonNull(param, msg)` for all fields and parameters that must not be null.

An example looks like this

```java
public class Example {

    private String name;

    private String nickName;

    public void setName(@NonNull final String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.nickName = this.name;
    }

    public void setNickName(@NonNull final String nickName) {
        this.nickName = Objects.requireNonNull(nickName, "nickName must not be null");
    }

    public int getNameLength() {
        return name.length(); //Without early checks the exception will be thrown here what can be long after the creation of the object
    }

    public int getNickNameLenght() {
        return nickName.length(); //Without early checks the exception will be thrown here what can be long after the creation of the object
    }
}
```

### Null handling of nullable fields

If a field is annotated with `@@nullable` in the language agnostic specification, the Java getter and setter must be
annotated with `org.jspecify.annotations.Nullable`.
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

It can make sense to add support of `Optional` in Java.
In this case an additional getter can be added:

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

    @NonNull
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }
}
```

### Null handling of not nullable fields

If a field parameter is not annotated with `@@nullable` in the language agnostic specification, the Java getter and
setter must be annotated with `org.jspecify.annotations.NonNull`.
Next to that the field must be initialized in the constructor if it is not annotated by `@@default(value)` in the
language agnostic specification.

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

If a field is annotated with `@@immutable` and `@@nullable` in the language agnostic specification and the Java
implementation is a record, the Java parameter of the `record` must be annotated with `org.jspecify.annotations.NonNull`
and checked in the compact constructor.
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

If a field is annotated with `@@immutable` and `@@nullable` in the language agnostic specification and the Java
implementation is a `class`, the Java parameter of the constructor must be annotated with
`org.jspecify.annotations.NonNull` and checked in the constructor.
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

If a field parameter is annotated with `@@immutable` but not with `@@nullable` in the language agnostic specification,
the Java parameter of the constructor must be annotated with `org.jspecify.annotations.NonNull`.

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

All public constructor and method parameters of reference types must be explicitly annotated with
`org.jspecify.annotations.NonNull` or `org.jspecify.annotations.Nullable`.

Rules:

- Use `@NonNull` for all parameters that are not annotated with `@@nullable` in the meta-language.
- Use `@Nullable` only if the corresponding parameter is annotated with `@@nullable` in the meta-language.
- For numeric and boolean parameters defined as `@@nullable` use wrapper types (
  `Byte/Short/Integer/Long/Double/Boolean`) instead of primitives. Otherwise, prefer primitives.
- Always perform early null checks with `Objects.requireNonNull(param, "paramName must not be null")` when the parameter
  is stored or accessed directly. Prefer performing the check once and reusing the validated value.
- Collections passed as parameters may be nullable only if the meta-language marks them `@@nullable`. If not nullable,
  callers must not pass `null` and implementations must check accordingly. Even if a collection parameter is nullable,
  do not treat `null` and an empty collection as the same value unless explicitly specified by the API contract.

Examples:

```java
public final class UserService {

    // Not nullable parameter, primitive used for non-nullable numeric types
    public void updateAge(@NonNull final String userId, final int age) {
        final String id = Objects.requireNonNull(userId, "userId must not be null");
        // ... use id and age
    }

    // Nullable parameter maps to wrapper and is annotated as @Nullable
    public void setNickname(@NonNull final String userId, @Nullable final String nickname) {
        final String id = Objects.requireNonNull(userId, "userId must not be null");
        // nickname can be null; handle accordingly
    }

    // Collection parameter must not be null unless marked @@nullable in meta-language
    public void replaceTags(@NonNull final String userId, @NonNull final List<String> tags) {
        final String id = Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(tags, "tags must not be null");
        // If stored, copy into a thread-safe collection
        // this.tags.clear(); this.tags.addAll(tags);
    }

    // If a collection parameter is nullable in the meta-language
    public void addTags(@NonNull final String userId, @Nullable final List<String> tagsOrNull) {
        final String id = Objects.requireNonNull(userId, "userId must not be null");
        if (tagsOrNull == null) {
            return; // defined semantics for null (e.g., no-op)
        }
        // handle non-null list
    }
}
```

### Usage of Optional

`Optional` was designed as a return type to represent the absence of a value. It must **never** be used as a method
parameter, constructor parameter, or field type. These are not the intended use cases and lead to awkward APIs, extra
wrapping, and unnecessary complexity. For parameters that may be absent, use `@Nullable` and document the semantics for
`null` explicitly. For fields, store the value directly and use `@Nullable` if the field can be null.

`Optional` should be used in the following cases:

- As a **return type** of a method to signal that the result may be absent.
- As a **temporary variable** inside method bodies, for example when working with streams or transformations.

When creating an `Optional`, prefer `Optional.ofNullable(value)` for any value where nullability is uncertain or
possible. `Optional.of(value)` must only be used when the value is guaranteed to be non-null, as it throws a
`NullPointerException` if passed `null`. In practice, `Optional.ofNullable(value)` is the safer and more common choice.

```java
// WRONG: Do not use Optional as a parameter
public void setName(Optional<String> name) { ... }

// WRONG: Do not use Optional as a field
private Optional<String> name;

// CORRECT: Use @Nullable for parameters that may be absent
public void setName(@Nullable final String name) { ... }

// CORRECT: Use Optional as a return type
@NonNull
public Optional<String> findName() {
    return Optional.ofNullable(name);
}

// CORRECT: Use Optional as a temporary variable in transformations
public String getDisplayName() {
    return Optional.ofNullable(name)
            .map(String::trim)
            .filter(n -> !n.isEmpty())
            .orElse("Unknown");
}

// CORRECT: Use Optional.ofNullable for values that may be null
Optional<User> user = Optional.ofNullable(userMap.get(id));

// Use Optional.of only when the value is guaranteed to be non-null
Optional<String> greeting = Optional.of("Hello");
```

### Null handling of method return value

Method return types in the public API must also be annotated with `@NonNull` or `@Nullable` according to the
meta-language.

Rules:

- If a method return type is not annotated with `@@nullable` in the meta-language, annotate it with `@NonNull` in Java
  and never return `null`.
- If a method return type is annotated with `@@nullable` in the meta-language, annotate it with `@Nullable` in Java and
  document what `null` means.
- Collections must never be returned as `null` (even if `@@nullable` was specified in error). Return empty immutable
  collections instead. Do not use `Optional<List<...>>` in the public API.
- For asynchronous methods (`@@async`), return `CompletionStage<T>` where the `CompletionStage` itself must be non-null.
  Apply nullability to `T` following the same rules as for synchronous returns.
- Optional convenience accessors may be added next to a `@Nullable` return to improve ergonomics, but the canonical
  method should return the raw annotated type.

Examples:

```java
public interface UserRepository {

    // Non-null return value
    @NonNull
    User getById(@NonNull String id);

    // Nullable return value when entity might not exist
    @Nullable
    User findByEmail(@NonNull String email);

    // Never return null for collections – return an immutable empty list
    @NonNull
    List<User> listAll();

    // Async variant: CompletionStage itself is never null
    @NonNull
    CompletionStage<User> getByIdAsync(@NonNull String id);
}

public final class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> byId = new ConcurrentHashMap<>();

    @Override
    public @NonNull User getById(@NonNull final String id) {
        final String nonNullId = Objects.requireNonNull(id, "id must not be null");
        final User user = byId.get(nonNullId);
        if (user == null) {
            throw new NoSuchElementException("User not found: " + nonNullId);
        }
        return user;
    }

    @Override
    public @Nullable User findByEmail(@NonNull final String email) {
        Objects.requireNonNull(email, "email must not be null");
        // return null when not found (documented by @Nullable)
        return byId.values().stream().filter(u -> email.equals(u.email())).findFirst().orElse(null);
    }

    @Override
    public @NonNull List<User> listAll() {
        // Return an immutable view/copy, never null
        return Collections.unmodifiableList(new ArrayList<>(byId.values()));
    }

    @Override
    public @NonNull CompletionStage<User> getByIdAsync(@NonNull final String id) {
        Objects.requireNonNull(id, "id must not be null");
        return CompletableFuture.supplyAsync(() -> getById(id));
    }
}
```

Optional convenience accessor for a nullable return:

```java
public interface ProfileService {

    @Nullable
    Profile findProfile(@NonNull String userId);

    @NonNull
    default Optional<Profile> findProfileOptional(@NonNull final String userId) {
        return Optional.ofNullable(findProfile(userId));
    }
}
```

## Implementation of Attribute annotations

Attribute annotations that are defined in the language agnostic specification should be implemented in Java in the
following ways.

The handling of `@@immutable` is defined in the ['Immutable Objects' section](#immutable-objects).
The handling of `@@nullable` is defined in the ['Null handling' section](#null-handling).

The `@@default(value)` annotation defines a default value for a field. In Java that will always be set in the
constructor or directly in the field declaration.
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

The `@@min(value)`, `@@max(value)`, `@@minLength(value)`, `@@maxLength(value)`, and `@@pattern(regex)` annotations are
all handled in the same way: Checks based on the value must be added to the constructor and setter methods.
Attributes annotated with any of those annotations must never be set directly in the field declaration.
The following example shows how to implement this annotation:

```java
import java.time.LocalDate;
import java.util.Objects;

public class Example {

    // example of an attribute that is defined as @@minLength(1) name:string
    private String name;

    // example of an attribute that is defined as @@min(0) age:int32
    private int age;

    //instead of implementing the checks 2 times the setter method can be called directly in the constructor
    public Example(@NonNull final String name, final int age) {
        setName(name);
        setAge(age);
    }

    public void setAge(final int age) {
        if (age < 0) {
            throw new IllegalArgumentException("age must be minimum 0");
        }
        this.age = age;
    }

    public void setName(@NonNull final String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (name.length() < 1) {
            throw new IllegalArgumentException("name must be minimum 1 character long");
        }
        this.name = name;
    }
}
```

## Asynchronous methods

Methods that are annotated with `@@async` in the meta-language must return a `java.util.concurrent.CompletionStage<T>`
instead of a concrete type.
The benefit of `java.util.concurrent.CompletionStage<T>` against `java.util.concurrent.CompletableFuture<T>` is, that it
is an interface.
Implementations can use `java.util.concurrent.CompletableFuture<T>` since it implements the
`java.util.concurrent.CompletionStage<T>` interface.
Against `java.util.concurrent.Future<T>` the `java.util.concurrent.CompletionStage<T>` interface is more flexible and
provides more functionality.
`java.util.concurrent.CompletionStage<T>` contains the `CompletableFuture<T> toCompletableFuture()` method that can be
used to transform it to a `java.util.concurrent.CompletableFuture<T>` or `java.util.concurrent.Future<T>`.

Example of an asynchronous method:

```java
public interface ExampleService {

    CompletionStage<Example> getExample(final String id);

}
```

In general an API should provide a synchronous method as alternative to the asynchronous method.
The synchronous method can be defined and implemented as follows:

```java
public interface ExampleService {

    CompletionStage<Example> getExample();

    default Example getExampleSync(final long timeout, final TimeUnit unit) {
        return getExample(id).toCompletableFuture().get(timeout, unit);
    }

}
```

The sample uses `long timeout, TimeUnit unit` as parameters for the synchronous method.
That is the best practice in Java to ensure that the synchronous method can be called from multiple threads.
Instead of just defining a `long timeoutInMs` the usage of `TimeUnit` is recommended.

## Usage of final Keyword

The `final` keyword should be used consistently throughout the full implementation.
Whenever possible, the `final` keyword should be used in the following cases:

- In the declaration of a field.
- In the declaration of a parameter of a method or constructor.
- In the declaration of a local variable.
- In the declaration of a class or record.

## Avoid Usage of var

Do not use `var` in our SDK and library code.
While `var` can reduce verbosity, it often makes code less readable and obscures API intent.
Prefer explicit types for local variables.

## Factories

Often factories are defined in the meta-language to create instances of a type.
In Java, factories should be implemented as static methods in the type that is created by the factory method instead of
creating a factory class per namespace.
The following example shows how to implement a factory method:

```java
public class Example {

    private final Address address;

    public Example(final @NonNull Address address) {
        this.address = Objects.requireNonNull(address, "address must not be null");
    }

    // Definition of the factory method
    public static Example createExample(final @NonNull String name) {
        final Address address = Address.createAddress(name); // Here another factory method is called
        return new Example(address);
    }
}
```

## Logging

Use `java.lang.System.Logger` (introduced in Java 9) for all logging in the SDK and library code.
By using the JDK's built-in logging facade, no external logger dependency (such as SLF4J or Log4j) is needed.
Consumers of the library can plug in any logging backend they prefer via the `System.LoggerFinder` SPI.

Example usage:

```java
public class ExampleService {

    private static final System.Logger LOGGER = System.getLogger(ExampleService.class.getName());

    public void doWork() {
        LOGGER.log(System.Logger.Level.INFO, "Starting work");
        try {
            // ...
        } catch (final Exception e) {
            LOGGER.log(System.Logger.Level.ERROR, "Unexpected error during work", e);
        }
    }
}
```

### Parameterized and Expensive Log Messages

The `System.Logger` API provides `log` methods that accept message parameters via `{0}`, `{1}`, etc. placeholders
(using `java.text.MessageFormat` syntax). These should be preferred over string concatenation since the message is only
formatted when the log level is active.

```java
LOGGER.log(System.Logger.Level.DEBUG, "Processing item {0} of {1}",currentIndex, totalCount);
```

For log messages where constructing the message itself is expensive (e.g. calling `toString()` on complex objects or
performing computations), use a `Supplier<String>` or guard the call with `isLoggable(level)`. Both approaches ensure
the expensive operation is only performed when the message will actually be logged.

```java
// Using a Supplier to defer expensive message construction
LOGGER.log(System.Logger.Level.DEBUG, () ->"State snapshot: "+

buildExpensiveSnapshot());

// Using isLoggable to guard expensive operations
        if(LOGGER.

isLoggable(System.Logger.Level.TRACE)){
final String dump = generateDetailedDump();
    LOGGER.

log(System.Logger.Level.TRACE, "Full dump: {0}",dump);
}
```

The `Supplier` variant is generally more concise, while `isLoggable` is useful when multiple statements or
side-effect-free preparations are needed before logging.

## SPI (Service Provider Interface)

When defining an SPI, two discovery approaches must be provided so that both modular and classpath-based projects are
supported:

1. **Java Module System (`module-info.java`)** — For projects using the Java Platform Module System (JPMS), the service
   provider must be declared via `provides ... with ...` in the module descriptor.
2. **Google AutoService** — For projects that run on the classpath (without `module-info.java`), the
   `@com.google.auto.service.AutoService` annotation must be used on the provider implementation. The AutoService
   annotation processor must be configured in the build so that the `META-INF/services` file is automatically generated
   at compile time. Both the annotation dependency and the annotation processor must be declared — the annotation alone
   is not sufficient. For example, in Gradle:
   ```groovy
   dependencies {
       compileOnly 'com.google.auto.service:auto-service-annotations:1.1.1'
       annotationProcessor 'com.google.auto.service:auto-service:1.1.1'
   }
   ```
   In Maven:
   ```xml
   <dependencies>
       <dependency>
           <groupId>com.google.auto.service</groupId>
           <artifactId>auto-service-annotations</artifactId>
           <version>1.1.1</version>
           <optional>true</optional>
       </dependency>
       <dependency>
           <groupId>com.google.auto.service</groupId>
           <artifactId>auto-service</artifactId>
           <version>1.1.1</version>
           <optional>true</optional>
           <scope>provided</scope>
       </dependency>
   </dependencies>
   ```

Both approaches must be present so that consumers can use the SPI regardless of whether they use the module system or
not.

Example of a service provider supporting both approaches:

```java
// Service interface
public interface ExampleProvider {
    String provide();
}
```

```java
// Provider implementation with AutoService annotation for classpath-based discovery
@com.google.auto.service.AutoService(ExampleProvider.class)
public class DefaultExampleProvider implements ExampleProvider {

    @Override
    public String provide() {
        return "default";
    }
}
```

```java
// module-info.java for modular discovery
module com.example.provider {
    requires com.example.api;
    provides com.example.api.ExampleProvider
            with com.example.provider.DefaultExampleProvider;
}
```

## Questions & Comments

## Todos based on AI tests for generating Java code
