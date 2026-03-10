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
| `type`            | `java.lang.Class<?>`                                                                                                               | Used for runtime type information, typically with generics `Class<T>`            |

### Type Parameter for Runtime Type Information

The meta-language defines `type` as a way to specify runtime type information.
In Java, this maps to `java.lang.Class<?>`.

**Basic Usage**:

```java
// Meta-language definition
Container {
    type getInnerType ()
}

// Java implementation
public interface Container {
    @NonNull
    Class<?> getInnerType();
}
```

**Preferred: Generic Type-Safe Usage**:

When possible, use generics to provide type safety:

```java
// Meta-language definition with generic
abstraction Container

<$$T> {
    $$T getInnerType ()
}

// Java implementation with generics
public interface Container<T> {
    @NonNull
    T getInnerType();
}
```

**Usage Examples**:

```java
// Basic usage with Class<?>
Container container = new ServiceContainer();
Object service = container.create(MyService.class);

// Type-safe usage with generics
Container<Transaction> txContainer = new TypedServiceContainer<>();
Transaction tx = txContainer.create(Transaction.class); // Type-safe, no cast needed

// Common pattern: factory with type parameter
public <T extends Transaction> T createTransaction(@NonNull final Class<T> transactionType) {
    Objects.requireNonNull(transactionType, "transactionType must not be null");
    // Create instance based on type
    return instantiate(transactionType);
}
```

**Best Practices for `type` → `Class<?>`**:

1. **Prefer generics**: Use `Class<T>` with generic type parameters when possible for type safety
2. **Null checks**: Always validate that Class parameters are not null
3. **Bounded wildcards**: Use `Class<? extends BaseType>` to constrain acceptable types
4. **Avoid raw types**: Never use raw `Class` without type parameter
5. **Document constraints**: Clearly document which types are acceptable and any requirements (e.g., must have no-arg
   constructor)
6. **Consider alternatives**: For simple cases, consider using enums or sealed types instead of runtime type parameters

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

Avoid using `Optional` as a method parameter in the public API. Instead, use `@Nullable` for optional parameters and
document the semantics for `null` explicitly.

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

## Avoid Usage of Lombok

Do not use Lombok in our SDK and library code.
With modern Java features such as records, sealed classes, and pattern matching, the boilerplate that Lombok was
traditionally used to eliminate is no longer a significant concern.
Records cover the most common use case (immutable data carriers) natively, and explicit constructors, getters, and
`equals`/`hashCode` implementations keep the codebase transparent and easy to navigate without requiring an annotation
processor.

## Implementing `equals` and `hashCode`

Java records automatically generate correct `equals` and `hashCode` implementations based on all record components.
For non-record classes, both methods **must** be implemented manually whenever instances may be compared for equality,
used in collections such as `Set` or as `Map` keys, or tested in unit tests.

### Rules

- Always override `equals` and `hashCode` together. Overriding only one violates the general contract and leads to
  subtle bugs (e.g., an object may be `.equals()` to another but land in a different hash bucket).
- Use `java.util.Objects.equals(Object, Object)` for field-by-field comparison and `java.util.Objects.hash(Object...)`
  for computing hash codes. These helpers handle `null` safely and keep the implementation concise.
- Use `instanceof` with pattern matching in `equals` instead of `getClass()` comparisons.
  This keeps the check readable and in a single expression.
- Include exactly the same set of fields in both methods.
  A field that participates in equality must also contribute to the hash code.
- Prefer using only immutable (`final`) fields in `equals` and `hashCode`. If a mutable field is included in the hash
  code computation, any mutation after the object has been inserted into a hash-based collection (`HashSet`, `HashMap`)
  will change its hash code, causing the object to become unreachable in the collection. Before including a mutable
  field, always reconsider whether that field truly needs to be mutable.
- Be aware that even a `final` reference field can cause unstable hash codes if the referenced object is itself mutable
  or implements `hashCode` based on mutable internal state. When including a field of a complex custom type, verify
  that the type's `hashCode` implementation is stable — i.e., it does not change over the lifetime of the object.
- Mark both methods with `@Override` so the compiler catches signature mistakes.

### Example

```java
import java.util.Objects;

public class Person {

    private final String name;

    private final int age;

    public Person(@NonNull final String name, final int age) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.age = age;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Person other)) {
            return false;
        }
        return age == other.age && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }
}
```

### Common pitfalls

- **Using `==` for reference fields** – Use `Objects.equals(a, b)` instead, which handles `null` correctly.
- **Including array fields directly** – Arrays do not override `equals`/`hashCode`. Use `Arrays.equals` and
  `Arrays.hashCode` for array fields.
- **Forgetting `hashCode`** – Collections like `HashSet` and `HashMap` rely on `hashCode`. If only `equals` is
  overridden, logically equal objects may not be found in these collections.

## Implementing `toString`

Java records automatically generate a `toString` implementation that includes the record class name and all component
values.
For non-record classes, `toString` should be overridden to provide a meaningful, human-readable representation that is
useful for debugging and logging.

### Rules

- Always override `toString` on non-record classes that carry state. The default `Object.toString()` output
  (e.g., `com.example.Person@1a2b3c4d`) is not useful for debugging.
- Use a consistent format across the codebase. The recommended format mirrors the one generated by records:
  `ClassName[field1=value1, field2=value2]`.
- Include fields that help identify the object during debugging. Not every field needs to be present — focus on the
  fields that are most useful for understanding the object's state at a glance.
- **Never include sensitive data** such as passwords, tokens, private keys, or personally identifiable information (PII)
  in `toString`. These values can end up in log files, exception messages, or debug output. If a field contains
  sensitive data, either omit it entirely or replace its value with a placeholder such as `***`.
- Use `java.util.Objects.toString(Object, String)` or `String.valueOf(Object)` for individual fields that may be `null`
  to avoid `NullPointerException` inside `toString`.
- Do **not** call methods with side effects or expensive computations inside `toString`. The method should be safe to
  call at any time without altering object state or causing performance issues.
- Mark the method with `@Override` so the compiler catches signature mistakes.

### Example

```java
import java.util.Objects;

public class Person {

    private final String name;

    private int age;

    @Nullable
    private String nickname;

    private String secret;

    public Person(@NonNull final String name, final int age) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.age = age;
    }

    @Override
    public String toString() {
        return "Person[name=" + name
                + ", age=" + age
                + ", nickname=" + Objects.toString(nickname, "N/A")
                + ", secret=***]";
    }
}
```

### Common pitfalls

- **Including sensitive data** – Passwords, tokens, and PII must never appear in `toString` output. Log files and
  stack traces are often stored in plain text and may be accessible to a broad audience.
- **Calling `toString` on circular references** – If two objects reference each other, calling `toString` on either
  can cause a `StackOverflowError`. Break the cycle by printing only an identifier (e.g., an ID field) instead of the
  full referenced object.
- **Relying on `toString` for parsing or logic** – The output of `toString` is for human consumption only. Never parse
  it to extract values or use it in `equals`/`hashCode` comparisons. If a stable text representation is needed, provide
  a dedicated serialization method instead.

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

## Builder Pattern

The Builder Pattern is a recommended design pattern for constructing complex objects with many parameters,
especially when several parameters are optional. This pattern improves API usability and readability by providing a
fluent interface for object construction.

### When to Use Builder Pattern

The Builder Pattern should be used for:

- **Central domain objects** that users frequently create (e.g., Transactions, Queries, Configuration objects)
- Classes with **many parameters** (more than 4-5 parameters)
- Classes with **multiple optional parameters**
- Classes where **parameter order** might be confusing
- Classes that require **validation across multiple fields** before construction

The Builder Pattern is **NOT required** for:

- Simple data classes with few parameters
- Internal implementation classes not exposed in public API
- Immutable records with only required fields

### Builder Pattern Implementation

**Example - Transaction with Builder**:

```java
public final class Transaction {

    private final String transactionId;
    private final BigDecimal amount;
    private final String fromAccount;
    private final String toAccount;
    private final LocalDateTime timestamp;
    private final String memo;
    private final TransactionType type;

    // Constructor with all parameters - allows direct instantiation without builder
    public Transaction(@NonNull final String transactionId,
                       @NonNull final BigDecimal amount,
                       @NonNull final String fromAccount,
                       @NonNull final String toAccount,
                       @NonNull final TransactionType type,
                       @NonNull final LocalDateTime timestamp,
                       @Nullable final String memo) {
        // Validate required fields
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.fromAccount = Objects.requireNonNull(fromAccount, "fromAccount must not be null");
        this.toAccount = Objects.requireNonNull(toAccount, "toAccount must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.memo = memo;

        // Cross-field validation
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (fromAccount.equals(toAccount)) {
            throw new IllegalArgumentException("fromAccount and toAccount must be different");
        }
    }

    // Private constructor for builder
    private Transaction(@NonNull final Builder builder) {
        this(
                builder.transactionId,
                builder.amount,
                builder.fromAccount,
                builder.toAccount,
                builder.type,
                builder.timestamp,
                builder.memo
        );
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    @NonNull
    public String getTransactionId() {
        return transactionId;
    }

    @NonNull
    public BigDecimal getAmount() {
        return amount;
    }

    @NonNull
    public String getFromAccount() {
        return fromAccount;
    }

    @NonNull
    public String getToAccount() {
        return toAccount;
    }

    @NonNull
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Nullable
    public String getMemo() {
        return memo;
    }

    @NonNull
    public TransactionType getType() {
        return type;
    }

    public static final class Builder {

        private String transactionId;
        private BigDecimal amount;
        private String fromAccount;
        private String toAccount;
        private LocalDateTime timestamp = LocalDateTime.now(); // default value
        private String memo;
        private TransactionType type;

        private Builder() {
            // Private constructor - use Transaction.builder()
        }

        @NonNull
        public Builder transactionId(@NonNull final String transactionId) {
            this.transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
            return this;
        }

        @NonNull
        public Builder amount(@NonNull final BigDecimal amount) {
            this.amount = Objects.requireNonNull(amount, "amount must not be null");
            return this;
        }

        @NonNull
        public Builder fromAccount(@NonNull final String fromAccount) {
            this.fromAccount = Objects.requireNonNull(fromAccount, "fromAccount must not be null");
            return this;
        }

        @NonNull
        public Builder toAccount(@NonNull final String toAccount) {
            this.toAccount = Objects.requireNonNull(toAccount, "toAccount must not be null");
            return this;
        }

        @NonNull
        public Builder timestamp(@NonNull final LocalDateTime timestamp) {
            this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
            return this;
        }

        @NonNull
        public Builder memo(@Nullable final String memo) {
            this.memo = memo;
            return this;
        }

        @NonNull
        public Builder type(@NonNull final TransactionType type) {
            this.type = Objects.requireNonNull(type, "type must not be null");
            return this;
        }

        @NonNull
        public Transaction build() {
            return new Transaction(this);
        }
    }
}
```

**Usage Example**:

```java
// Option 1: Creating a transaction with builder - fluent and readable
Transaction transaction = Transaction.builder()
                .transactionId("tx-12345")
                .amount(new BigDecimal("100.50"))
                .fromAccount("account-a")
                .toAccount("account-b")
                .type(TransactionType.TRANSFER)
                .memo("Payment for services")
                .build();

// Option 2: Creating a transaction with constructor - direct instantiation
Transaction directTransaction = new Transaction(
        "tx-12345",
        new BigDecimal("100.50"),
        "account-a",
        "account-b",
        TransactionType.TRANSFER,
        LocalDateTime.now(),
        "Payment for services"
);

// Builder with optional parameters omitted
Transaction simpleTransaction = Transaction.builder()
        .transactionId("tx-67890")
        .amount(new BigDecimal("50.00"))
        .fromAccount("account-c")
        .toAccount("account-d")
        .type(TransactionType.TRANSFER)
        .build(); // timestamp will use default (now), memo is null
```

### Builder Pattern with Records

For immutable data classes (records), a builder can still be useful when there are many optional parameters:

```java
public record QueryOptions(
        @NonNull String query,
        int limit,
        int offset,
        @Nullable String sortField,
        @NonNull SortOrder sortOrder,
        boolean includeMetadata
) {
    // Compact constructor with validation
    public QueryOptions {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(sortOrder, "sortOrder must not be null");
        if (limit < 0) {
            throw new IllegalArgumentException("limit must be non-negative");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be non-negative");
        }
    }

    // Constructor for builder
    private QueryOptions(Builder builder) {
        this(
                builder.query,
                builder.limit,
                builder.offset,
                builder.sortField,
                builder.sortOrder != null ? builder.sortOrder : SortOrder.ASC,
                builder.includeMetadata
        );
    }

    @NonNull
    public static Builder builder(@NonNull final String query) {
        return new Builder(query);
    }

    public static final class Builder {
        private final String query;
        private int limit = 100; // default
        private int offset = 0; // default
        private String sortField;
        private SortOrder sortOrder;
        private boolean includeMetadata = false; // default

        private Builder(@NonNull final String query) {
            this.query = Objects.requireNonNull(query, "query must not be null");
        }

        @NonNull
        public Builder limit(final int limit) {
            this.limit = limit;
            return this;
        }

        @NonNull
        public Builder offset(final int offset) {
            this.offset = offset;
            return this;
        }

        @NonNull
        public Builder sortField(@Nullable final String sortField) {
            this.sortField = sortField;
            return this;
        }

        @NonNull
        public Builder sortOrder(@NonNull final SortOrder sortOrder) {
            this.sortOrder = Objects.requireNonNull(sortOrder, "sortOrder must not be null");
            return this;
        }

        @NonNull
        public Builder includeMetadata(final boolean includeMetadata) {
            this.includeMetadata = includeMetadata;
            return this;
        }

        @NonNull
        public QueryOptions build() {
            return new QueryOptions(this);
        }
    }
}
```

**Usage with Record Builder**:

```java
// Using builder with defaults
QueryOptions options = QueryOptions.builder("SELECT * FROM users")
                .limit(50)
                .sortField("name")
                .build();

// Direct instantiation with record constructor
QueryOptions directOptions = new QueryOptions(
        "SELECT * FROM users",
        50,
        0,
        "name",
        SortOrder.ASC,
        false
);
```

### Builder Pattern Best Practices

1. **Static factory method**: Provide a static `builder()` method to obtain a Builder instance
2. **Fluent interface**: All builder methods should return `this` (except `build()`)
3. **Immutable product**: The constructed object should be immutable
4. **Public constructor with all parameters**: Provide a public constructor accepting all parameters for direct
   instantiation without builder
5. **Private builder constructor**: The builder-specific constructor should be private and delegate to the public
   constructor
6. **Initialize defaults in Builder**: Set default values directly in the Builder field declarations (e.g.,
   `private LocalDateTime timestamp = LocalDateTime.now();`), not in the build() method
7. **Validation in constructor**: Perform all validation in the main class constructor, not in builder methods
8. **Null checks in builder methods**: Check for null on required parameters in builder setter methods
9. **Meaningful method names**: Builder methods should have clear, descriptive names matching the field names
10. **Final Builder class**: Make the Builder class `static final` nested within the product class
11. **Builder reuse**: Document whether builders can be reused (generally they should not be)
12. **Thread safety**: Builders are not thread-safe; each thread should use its own builder instance

### Alternative: Factory Methods with Parameters

For simpler cases with fewer optional parameters, consider using factory methods with parameter objects or overloaded
factory methods instead of a full builder.

**Factory Methods with Classes**:

```java
public final class PublicKey {

    // Simple case - factory methods are sufficient
    @NonNull
    public static PublicKey create(@NonNull final KeyAlgorithm algorithm, @NonNull final byte[] rawBytes) {
        return KeyFactory.createPublicKey(algorithm, rawBytes);
    }

    @NonNull
    public static PublicKey create(@NonNull final String pemEncoded) {
        return KeyFactory.createPublicKey(EncodedKeyContainer.SPKI_WITH_PEM, pemEncoded);
    }
}
```

**Factory Methods with Records**:

Records work perfectly with factory methods for common creation patterns:

```java
public record KeyPair(
        @NonNull PublicKey publicKey,
        @NonNull PrivateKey privateKey
) {
    // Compact constructor with validation
    public KeyPair {
        Objects.requireNonNull(publicKey, "publicKey must not be null");
        Objects.requireNonNull(privateKey, "privateKey must not be null");
    }

    // Factory method for generating a new key pair
    @NonNull
    public static KeyPair generate(@NonNull final KeyAlgorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        final PrivateKey privateKey = PrivateKey.generate(algorithm);
        final PublicKey publicKey = privateKey.derivePublicKey();
        return new KeyPair(publicKey, privateKey);
    }

    // Factory method for importing from bytes
    @NonNull
    public static KeyPair fromBytes(@NonNull final KeyAlgorithm algorithm,
                                    @NonNull final byte[] publicKeyBytes,
                                    @NonNull final byte[] privateKeyBytes) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        Objects.requireNonNull(publicKeyBytes, "publicKeyBytes must not be null");
        Objects.requireNonNull(privateKeyBytes, "privateKeyBytes must not be null");

        final PublicKey publicKey = PublicKey.create(algorithm, publicKeyBytes);
        final PrivateKey privateKey = PrivateKey.create(algorithm, privateKeyBytes);
        return new KeyPair(publicKey, privateKey);
    }
}
```

**Usage**:

```java
// Using factory method - clean and expressive
KeyPair keyPair = KeyPair.generate(KeyAlgorithm.ED25519);

// Using factory method with parameters
KeyPair imported = KeyPair.fromBytes(
        KeyAlgorithm.ED25519,
        publicKeyBytes,
        privateKeyBytes
);

// Direct constructor still available
KeyPair direct = new KeyPair(publicKey, privateKey);
```

## Testing

Multiple types of tests must be written for each new feature to ensure quality and correctness.
Understanding the different test types and their purposes is crucial for contributors.
In Java JUnit (version 5+) is used for all tests.

**Unit Tests**:

- Test individual classes and methods in isolation
- Run quickly without external dependencies
- Mock or stub external dependencies
- Must be written for all public API functionality

**Integration Tests**:

- Test interaction between multiple components
- May require external services or infrastructure
- **Status**: TBD - integration test strategy is being defined

**TCK (Technology Compatibility Kit)**:

- External test suite: [Hiero SDK TCK](https://github.com/hiero-ledger/hiero-sdk-tck)
- Provides comprehensive functional and integration tests
- Tests are executed against all SDK implementations via a driver
- Ensures consistency across SDKs (Java, JavaScript, Go, etc.)

### Unit Test Guidelines

All unit tests must follow the Given-When-Then pattern for clarity and readability.

**Example**:

```java
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PublicKeyTest {

    @Test
    void testVerifyValidSignature() {
        // given
        final byte[] message = "Hello, World!".getBytes();
        final PrivateKey privateKey = PrivateKey.create(KeyAlgorithm.ED25519);
        final PublicKey publicKey = privateKey.getPublicKey();
        final byte[] signature = privateKey.sign(message);

        // when
        final boolean result = publicKey.verify(message, signature);

        // then
        assertTrue(result, "Valid signature should be verified successfully");
    }

    @Test
    void testVerifyInvalidSignature() {
        // given
        final byte[] message = "Hello, World!".getBytes();
        final byte[] invalidSignature = new byte[64];
        final PublicKey publicKey = PublicKey.create(KeyAlgorithm.ED25519);

        // when
        final boolean result = publicKey.verify(message, invalidSignature);

        // then
        assertFalse(result, "Invalid signature should not be verified");
    }
}
```

**Best Practices for Unit Tests**:

1. **Use Given-When-Then structure** - clearly separate test phases with comments
2. **Test one thing per test** - each test should verify a single behavior
3. **Clear assertions** - provide meaningful assertion messages
5. **Test edge cases** - null values, empty collections, boundary values
6. **Test error conditions** - verify exceptions are thrown when expected
7. **Independent tests** - tests should not depend on execution order
8. **Fast execution** - unit tests should run in milliseconds

### Integration Test Guidelines

**Status**: TBD

## Java Platform Module System (JPMS)

All SDK modules must fully support the Java Platform Module System (JPMS) introduced in Java 9.
Each module must provide a `module-info.java` file that clearly defines:

- Exported packages (public API)
- Required dependencies
- Provided services (if applicable)
- Used services (if applicable)

### Public API vs. Internal Implementation

JPMS provides a clear separation between public API and internal implementation:

- **Exported packages**: These contain the public API that external consumers can use. All types in exported packages
  are accessible to consumers.
- **Non-exported packages**: These contain internal implementation details that are not part of the public API. Types in
  non-exported packages are not accessible to external modules, even if they are declared `public` in Java.

This separation allows for a clean distinction between what is part of the public contract and what is an internal
implementation detail that can change without affecting consumers.

### Package Structure Convention

To clearly distinguish between public API and internal implementation, use the following package structure:

```
org.hiero.{module}/              # Public API (exported)
├── {PublicInterfaces}.java
├── {PublicClasses}.java
└── impl/                        # Internal implementation (NOT exported)
    ├── {InternalImplementations}.java
    └── {Factories}.java
```

### Example module-info.java

```java
module org.hiero.keys {
    // Export public API packages
    exports org.hiero.keys;

    // Do NOT export internal implementation packages
    // (org.hiero.keys.impl is not exported and therefore not accessible from outside)

    // Declare compile-time only dependencies (annotations, etc.)
    requires static org.jspecify;

    // Declare runtime dependencies
    requires org.bouncycastle.provider;

    // Optionally provide services
    provides org.hiero.keys.KeyProvider
            with org.hiero.keys.impl.DefaultKeyProvider;
}
```

### Rules for JPMS

1. **Every module must have a `module-info.java`** at the root of its source directory
2. **Only export packages that contain public API** - never export `impl` packages to consumers
3. **Use `requires` for all dependencies** - make dependencies explicit
4. **Use `requires static` for compile-time only dependencies** - annotations (like `org.jspecify`), code generators, or
   other tools that are not needed at runtime must use `requires static`
5. **Avoid `requires transitive` whenever possible** - exposing types from dependencies in your public API should be
   avoided. If unavoidable (e.g., your public API returns or accepts types from another module), you must use
   `requires transitive` so consumers have access to those types
6. **Never use unnamed modules** - all production code must be in named modules
7. **Tests must run on the module path** - test code should also use named modules to ensure JPMS compatibility
8. **Use `exports ... to` for test-only access** - to test internal implementation packages (like `impl`), use qualified
   exports to make them accessible only to test modules
9. **Test modules should use `open module`** - declare test modules as `open module` to allow reflection access for test
   frameworks without needing individual `opens` declarations
10. **Document exported packages** in the module-info.java using comments

### Compile-Time vs Runtime Dependencies

Understanding the difference between compile-time and runtime dependencies is crucial for optimal module design:

**Compile-Time Only (`requires static`)**:

- Annotation libraries (e.g., `org.jspecify`)
- Code generation tools
- Build-time processors
- Any dependency whose types are only referenced in annotations or comments

**Runtime Dependencies (`requires`)**:

- Libraries whose types are used internally but NOT exposed in the public API
- Libraries that provide actual functionality at runtime
- Any dependency needed for the application to run

**Transitive Dependencies (`requires transitive`)** - **AVOID WHENEVER POSSIBLE**:

- Only use when your public API exposes types from another module (e.g., returning or accepting types from that module
  in public methods)
- This creates tight coupling between your module and the dependency
- **Best practice**: Wrap or adapt external types so your public API only uses your own types

**Example - Good Design (avoiding transitive)**:

```java
module org.hiero.keys {
    // Compile-time only: annotations are erased after compilation
    requires static org.jspecify;

    // Runtime: BouncyCastle is used internally but NOT exposed in public API
    requires org.bouncycastle.provider;

    // Export only our own types
    exports org.hiero.keys;
}
```

**Example - When transitive is unavoidable**:

```java
module org.hiero.client {
    requires static org.jspecify;

    // Our public API returns CompletionStage, so we use standard Java types (no transitive needed)
    requires java.base;

    // Our public API exposes Transaction types from org.hiero.transaction
    // Consumers MUST have access to these types
    requires transitive org.hiero.transaction;

    exports org.hiero.client;
}
```

### Testing with JPMS

Tests should run on the module path to ensure full JPMS compatibility. To test internal implementation packages that are
not exported to consumers, use qualified exports with `exports ... to`.

**module-info.java** (in `src/main/java/module-info.java`):

```java
module org.hiero.keys {
    // Export public API to everyone
    exports org.hiero.keys;

    // Export internal implementation ONLY to test module
    exports org.hiero.keys.impl to org.hiero.keys.test;

    requires static org.jspecify;
    requires org.bouncycastle.provider;
}
```

**module-info.java** (in `src/test/java/module-info.java`):

```java
open module org.hiero.keys.test {
    // Require the module under test
    requires org.hiero.keys;

    // Test frameworks
    requires org.junit.jupiter.api;

    // Note: 'open module' makes all packages accessible for reflection
    // No need for individual 'opens' declarations
}
```

**Key Points**:

- The production module uses `exports ... to` to selectively expose `impl` packages only to the test module
- The test module should be declared as `open module` to allow reflection access for test frameworks (JUnit, Mockito,
  etc.)
- Using `open module` eliminates the need for individual `opens` declarations for each test package
- This approach ensures that `impl` packages remain inaccessible to consumers while being fully testable

**Directory Structure**:

```
src/
├── main/
│   └── java/
│       ├── module-info.java
│       └── org/hiero/keys/
│           ├── PublicKey.java
│           └── impl/
│               └── PublicKeyImpl.java
└── test/
    └── java/
        ├── module-info.java
        └── org/hiero/keys/impl/test/
            └── PublicKeyImplTest.java
```

## Namespace Mapping

The meta-language uses namespaces to group related types and functionality. In Java, namespaces map to a combination of
**packages** and **JPMS modules**.

### Namespace Concept

**Meta-language namespace definition**:

```
namespace transactions
requires common, keys

constant MAX_TRANSACTIONS:int32 = 100

Transaction {
    @@immutable id: string
    @@immutable amount: decimal
}

enum TransactionStatus {
    PENDING
    COMPLETED
    FAILED
}
```

### Java Implementation of Namespaces

Namespaces are implemented using:

1. **Java Package** - for code organization (`org.hiero.transactions`)
2. **JPMS Module** - for encapsulation and dependency management

**Package Structure**:

```
org.hiero.transactions/
├── Transaction.java
├── TransactionStatus.java
├── TransactionConstants.java
└── impl/
    └── TransactionImpl.java
```

**module-info.java** (maps namespace dependencies):

```java
module org.hiero.transactions {
    // Namespace 'requires common, keys' maps to:
    requires org.hiero.common;
    requires org.hiero.keys;

    // Compile-time dependencies
    requires static org.jspecify;

    // Export public API (namespace types)
    exports org.hiero.transactions;

    // Do NOT export internal implementation
    // (org.hiero.transactions.impl stays private)
}
```

### Namespace to Package Naming Convention

**Rule**: `namespace NAME` → `org.hiero.NAME` package

| Meta-Language Namespace  | Java Package             |
|--------------------------|--------------------------|
| `namespace transactions` | `org.hiero.transactions` |
| `namespace keys`         | `org.hiero.keys`         |
| `namespace common`       | `org.hiero.common`       |
| `namespace client`       | `org.hiero.client`       |

### Namespace Dependencies

Namespace dependencies in the meta-language map directly to JPMS module dependencies:

```
// Meta-language
namespace transactions
requires common, keys

// Java module-info.java
module org.hiero.transactions {
    requires org.hiero.common;
    requires org.hiero.keys;
}
```

### Constants in Namespaces

Constants defined at namespace level should be placed in a dedicated constants class:

**Meta-language**:

```
namespace transactions

constant MAX_TRANSACTIONS:int32 = 100
constant DEFAULT_TIMEOUT:int64 = 30000
```

**Java Implementation**:

```java
package org.hiero.transactions;

/**
 * Constants for the transactions namespace.
 */
public final class TransactionConstants {

    /** Maximum number of transactions per batch */
    public static final int MAX_TRANSACTIONS = 100;

    /** Default timeout in milliseconds */
    public static final long DEFAULT_TIMEOUT = 30000L;

    private TransactionConstants() {
        // Prevent instantiation
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
}
```

### Cross-Namespace References

When types from one namespace reference types from another:

**Meta-language**:

```
namespace accounts
requires transactions

Account {
    lastTransaction: transactions.Transaction
}
```

**Java Implementation**:

```java
// module-info.java
module org.hiero.accounts {
    requires org.hiero.transactions;
    exports org.hiero.accounts;
}

// Account.java
package org.hiero.accounts;

        import org.hiero.transactions.Transaction;
        import org.jspecify.annotations.Nullable;

        public record Account(
        String id,
        @Nullable Transaction lastTransaction
        ){
        // ...
        }
```

**Note**: This creates a dependency, so `org.hiero.transactions` types appear in the public API. Consider whether
`requires transitive` is needed (see [JPMS section](#java-platform-module-system-jpms)).

## Questions & Comments

## Todos based on AI tests for generating Java code
