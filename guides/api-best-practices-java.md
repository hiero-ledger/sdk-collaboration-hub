# Java API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete Java patterns,
conventions, and ready‑to‑copy code templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

## Defensive Implementation

SDK implementations should be defensively written to minimize the risk of unexpected failures at runtime — even in
situations that are not explicitly required by the API contract. However, defensive measures must never come at the cost
of performance. Only use techniques that have negligible overhead in the normal case.

Examples of good defensive practices:

- **Null checks on public API boundaries** — Validate non-null parameters early with `Objects.requireNonNull` to fail
  fast with a clear message instead of producing a `NullPointerException` deep in the call stack.
- **Thread-safe collection types** — Use `CopyOnWriteArrayList`, `CopyOnWriteArraySet`, and `ConcurrentHashMap` for
  mutable internal collections instead of `ArrayList`, `HashSet`, and `HashMap`. These prevent
  `ConcurrentModificationException` if a user happens to access the SDK from multiple threads, without requiring
  explicit locks. The overhead is minimal for collections that are read far more often than written.
- **Immutable return values** — Return unmodifiable views or copies of internal collections to prevent callers from
  accidentally modifying SDK state.
- **Validation of constraints** — Enforce `@@min`, `@@max`, `@@minLength`, `@@maxLength`, and `@@pattern` annotations
  in constructors and setters to catch invalid data early.

Defensive implementation does **not** mean:

- Adding `synchronized` blocks or locks to every method — this kills performance and is only justified when explicitly
  required (see `@@threadSafe` in the [API guideline](api-guideline.md)).
- Catching and silently swallowing exceptions — errors should propagate clearly.
- Adding redundant checks in internal (non-public) code paths where invariants are already guaranteed.

## Type Mapping

Use the following canonical mappings when turning meta types into Java:

| Generic Type      | Java Type                                                                                                                          | Notes                                                                            |
|-------------------|------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| `string`          | `java.lang.String`                                                                                                                 | -                                                                                |
| `intX`            | `byte`, `short`, `int`, `long`, `java.lang.Byte`, `java.lang.Short`, `java.lang.Integer`, `java.lang.Long`, `java.math.BigInteger` | For all definitions that are not `@@nullable` the primitive types should be used |
| `uintX`           | `byte`, `short`, `int`, `long`, `java.lang.Byte`, `java.lang.Short`, `java.lang.Integer`, `java.lang.Long`, `java.math.BigInteger` | For all definitions that are not `@@nullable` the primitive types should be used |
| `double`          | `double`/ `java.lang.Double`                                                                                                       | For all definitions that are not `@@nullable` the primitive types should be used |
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
| `function<...>`   | `@FunctionalInterface` or `java.util.function.*`                                                                                   | See [Function Types](#function-types) section below                              |

### Type Parameter for Runtime Type Information

The meta-language defines `type` as a way to specify runtime type information.
In Java, this maps to `java.lang.Class<?>`.

**Basic Usage**:

```
// Meta-language definition
Container {
    type getInnerType()
}
```

```java
// Java implementation
public interface Container {
    @NonNull
    Class<?> getInnerType();
}
```

**Preferred: Generic Type-Safe Usage**:

When possible, use generics to provide type safety:

```
// Meta-language definition with generic
abstraction Container<$$T> {
    $$T getInnerType()
}
```

```java
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
Class<?> innerType = container.getInnerType();

// Type-safe usage with generics
Container<Transaction> txContainer = new TypedServiceContainer<>();
Class<Transaction> txClass = txContainer.getInnerType();

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

### Function Types

The meta-language `function<R m(p: T, ...)>` maps to a `@FunctionalInterface` in Java. Where possible, use the
standard interfaces from `java.util.function`. Define a custom `@FunctionalInterface` only when no standard interface
matches.

**Mapping to standard functional interfaces:**

| Meta-Language                                 | Java Type                              |
|-----------------------------------------------|----------------------------------------|
| `function<void run()>`                        | `java.lang.Runnable`                   |
| `function<void accept(value: T)>`             | `java.util.function.Consumer<T>`       |
| `function<R supply()>`                        | `java.util.function.Supplier<R>`       |
| `function<R apply(value: T)>`                 | `java.util.function.Function<T,R>`     |
| `function<bool test(value: T)>`               | `java.util.function.Predicate<T>`      |
| `function<R apply(value1: T, value2: U)>`     | `java.util.function.BiFunction<T,U,R>` |
| `function<void accept(value1: T, value2: U)>` | `java.util.function.BiConsumer<T,U>`   |

**Example with standard interface:**

```
// Meta-language
subscribe(callback: function<void onEvent(event: Event)>)
```

```java
// Java implementation using Consumer
public void subscribe(@NonNull final Consumer<Event> callback) {
    Objects.requireNonNull(callback, "callback must not be null");
    // ...
}
```

**Custom functional interface:**

When the function signature does not match any standard interface (e.g., more than two parameters or checked
exceptions),
define a custom `@FunctionalInterface`:

```
// Meta-language
execute(handler: function<bool onMessage(topic: string, message: bytes, timestamp: dateTime)>)
```

```java
// Java implementation with custom functional interface
@FunctionalInterface
public interface MessageHandler {
    boolean onMessage(@NonNull String topic, @NonNull byte[] message, @NonNull LocalDateTime timestamp);
}

public void execute(@NonNull final MessageHandler handler) {
    Objects.requireNonNull(handler, "handler must not be null");
    // ...
}
```

**Rules:**

1. Prefer standard `java.util.function` interfaces over custom ones
2. Always annotate custom functional interfaces with `@FunctionalInterface`
3. Apply `@NonNull`/`@Nullable` annotations to functional interface method parameters and return types
4. Function type parameters must not be `null` unless annotated with `@@nullable` in the meta-language

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
public record Person(@Nullable String name, @Nullable Integer age) {
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

Types annotated with `@@finalType` in the meta-language must be declared with the `final` keyword in Java (or as a
`record`, which is implicitly final). This prevents subclassing and ensures the type cannot be extended.

Next to that records should be used wherever possible.
If a non-abstract type in the meta-language only contains attributes annotated with `@@immutable`, the type must be
declared as a Java `record`.

### Generic Type Parameters

Generic type parameters in the meta-language use the `$$` prefix (e.g., `$$T`, `$$Product`). In Java, drop the `$$`
prefix and use standard Java generic syntax.

**Basic mapping:**

```
// Meta-language
abstraction Factory<$$Product> {
    $$Product create()
}
```

```java
// Java implementation
public interface Factory<Product> {
    @NonNull
    Product create();
}
```

**Bounded type parameters** using `extends` map directly:

```
// Meta-language
abstraction FruitFactory<$$Product extends Fruit> {
    $$Product create()
}
```

```java
// Java implementation
public interface FruitFactory<Product extends Fruit> {
    @NonNull
    Product create();
}
```

**Concrete extension** of a generic type:

```
// Meta-language
CarFactory extends Factory<Car> {
}
```

```java
// Java implementation
public final class CarFactory implements Factory<Car> {

    @Override
    @NonNull
    public Car create() {
        return new Car();
    }
}
```

**Rules:**

1. Drop the `$$` prefix — `$$T` becomes `T`, `$$Product` becomes `Product`
2. Keep descriptive names where the meta-language uses them (e.g., `Product` instead of shortening to `T`)
3. Apply `@NonNull`/`@Nullable` annotations to generic return types and parameters as usual

## Enumerations

Enumerations defined in the meta-language map directly to Java `enum` types. Enum values use `UPPER_SNAKE_CASE` as
defined in the meta-language naming conventions, which matches Java's standard enum naming.

**Simple enumeration:**

```
// Meta-language
enum TransactionStatus {
    PENDING
    COMPLETED
    FAILED
}
```

```java
// Java implementation
public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED
}
```

**Enumeration with immutable attributes:**

All attributes on enumerations must be `@@immutable` in the meta-language. In Java, enum fields are declared `final` and
set via the constructor.

```
// Meta-language
enum KeyAlgorithm {
    ED25519
    ECDSA_SECP256K1

    @@immutable keySize: int32
}
```

```java
// Java implementation
public enum KeyAlgorithm {

    ED25519(32),
    ECDSA_SECP256K1(33);

    private final int keySize;

    KeyAlgorithm(final int keySize) {
        this.keySize = keySize;
    }

    public int getKeySize() {
        return keySize;
    }
}
```

**Enumeration with methods:**

```
// Meta-language
enum TransactionStatus {
    PENDING
    COMPLETED
    FAILED

    bool isTerminal()
}
```

```java
// Java implementation
public enum TransactionStatus {

    PENDING,
    COMPLETED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
```

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
public record Example(@NonNull String name) {

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
public record Example(@NonNull String name) {

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
public void setName(Optional<String> name) { ...}

// WRONG: Do not use Optional as a field
private Optional<String> name;

// CORRECT: Use @Nullable for parameters that may be absent
public void setName(@Nullable final String name) { ...}

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

## Thread Safety (`@@threadSafe`)

The meta-language `@@threadSafe[(groupName)]` annotation indicates that a method or attribute accessor (getter and, if
mutable, setter) can be called concurrently by the SDK and must be implemented in a thread-safe manner. The optional
`groupName` groups methods and attributes whose accessors can be called concurrently with each other.

See the [API guideline](api-guideline.md) for the full semantics of `@@threadSafe`.

### The `@ThreadSafe` annotation

In Java, the meta-language `@@threadSafe` maps to a custom `@ThreadSafe` annotation provided by the SDK. The annotation
is defined in the `org.hiero.sdk.annotation` package (see [java-files/ThreadSafe.java](java-files/ThreadSafe.java) for
the full source).

The annotation serves two purposes:

- **On interface methods** — it documents a contract: any implementation of this method must be thread-safe.
- **On concrete methods and attribute accessors** — it indicates that the implementation is thread-safe.

The optional `group` parameter maps to the meta-language `groupName`:

```java
import org.hiero.sdk.annotation.ThreadSafe;

public interface DataCache {

    // @@threadSafe(cache) — grouped with other "cache" members
    @ThreadSafe(group = "cache")
    void updateCache(@NonNull byte[] data);

    // @@threadSafe(cache) — same group
    @ThreadSafe(group = "cache")
    @NonNull
    byte[] readCache();

    // @@threadSafe — no group, only safe for concurrent calls on its own
    @ThreadSafe
    void resetStats();
}
```

When `@@threadSafe` is applied to an attribute in the meta-language, annotate both the getter and (if mutable) the
setter with `@ThreadSafe`:

```java
public class ConnectionManager {

    private volatile boolean connected = true;

    @ThreadSafe
    public boolean isConnected() {
        return connected;
    }

    @ThreadSafe
    public void setConnected(final boolean connected) {
        this.connected = connected;
    }
}
```

All methods that implement a `@ThreadSafe`-annotated interface method must also carry the `@ThreadSafe` annotation on
the implementation.

### Implementation strategies

Always prefer **non-blocking** approaches over blocking ones. Blocking mechanisms (`synchronized`, `Lock`) suspend
threads, which reduces throughput, increases latency, and risks deadlocks. Non-blocking alternatives (atomics, volatile,
concurrent collections, immutable snapshots) allow threads to proceed without waiting and should be the first choice.
Only fall back to blocking synchronization when the operation genuinely requires mutual exclusion across multiple fields
or complex invariants that cannot be expressed with non-blocking primitives.

When blocking is unavoidable, prefer `java.util.concurrent.locks.Lock` (and its variants such as `ReadWriteLock`) over
`synchronized`. Locks offer finer-grained control: they can be acquired and released in different scopes, support
try-lock and timed-lock patterns, and allow read/write separation. `synchronized` should only be used in the simplest
cases where none of these advantages are needed.

The following table lists the common strategies **in order of preference** — try the topmost applicable strategy first:

| Priority | Strategy                          | When to use                                                                                     |
|----------|-----------------------------------|-------------------------------------------------------------------------------------------------|
| 1        | Immutable snapshots               | Return defensive copies so callers never see partial state — no synchronization needed at all   |
| 2        | `volatile` field                  | Simple flags or single references where only visibility is required                             |
| 3        | `java.util.concurrent.atomic.*`   | Single-field atomic updates (e.g., `AtomicReference`, `AtomicLong`)                             |
| 4        | Concurrent collections            | Thread-safe collection access (`ConcurrentHashMap`, `CopyOnWriteArrayList`)                     |
| 5        | `java.util.concurrent.locks.Lock` | Multi-field invariants requiring mutual exclusion; prefer `ReadWriteLock` for read-heavy groups |
| 6        | `synchronized` block / method     | Last resort for simple cases where a `Lock` provides no additional benefit                      |

### Example: Immutable snapshots

The preferred strategy is to avoid synchronization entirely by using immutable snapshots. When a getter returns a
defensive copy or an unmodifiable view, callers can read the data without any risk of seeing partial state — no locks,
no atomics, no contention. This works especially well for attributes whose value is replaced as a whole rather than
mutated in place.

```
// Meta-language
Configuration {
    @@threadSafe(config) properties: map<string, string>

    @@threadSafe(config)
    void updateProperties(properties: map<string, string>)
}
```

```java
// Java implementation using immutable snapshots — no blocking required

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hiero.sdk.annotation.ThreadSafe;

public final class Configuration {

    // ConcurrentHashMap handles concurrent reads and writes without blocking
    private final ConcurrentHashMap<String, String> properties = new ConcurrentHashMap<>();

    // @@threadSafe(config) attribute accessor — returns an immutable snapshot
    @ThreadSafe(group = "config")
    @NonNull
    public Map<String, String> getProperties() {
        // Map.copyOf creates an immutable copy; callers can read it freely without synchronization
        return Map.copyOf(properties);
    }

    // @@threadSafe(config) — replaces all properties atomically from the caller's perspective
    @ThreadSafe(group = "config")
    public void updateProperties(@NonNull final Map<String, String> properties) {
        Objects.requireNonNull(properties, "properties must not be null");
        this.properties.clear();
        this.properties.putAll(properties);
    }
}
```

The key insight is that the getter returns an independent, immutable copy. Even if another thread calls
`updateProperties` concurrently, callers that already hold a snapshot are unaffected. No thread ever blocks.

### Example: Volatile field

A `volatile` field guarantees that reads and writes are visible across threads without any locking. This is the right
choice when a single reference or primitive value is read frequently and written infrequently, and no compound
read-modify-write operation is needed.

```
// Meta-language
ConnectionManager {
    @@threadSafe connected: bool

    @@threadSafe
    void disconnect()
}
```

```java
// Java implementation using volatile — no blocking required

import org.hiero.sdk.annotation.ThreadSafe;

public final class ConnectionManager {

    // volatile ensures that any thread reading 'connected' sees the latest write
    private volatile boolean connected = true;

    // @@threadSafe attribute accessor — getter
    @ThreadSafe
    public boolean isConnected() {
        return connected;
    }

    // @@threadSafe attribute accessor — setter
    @ThreadSafe
    public void setConnected(final boolean connected) {
        this.connected = connected;
    }

    // @@threadSafe — uses the volatile field directly
    @ThreadSafe
    public void disconnect() {
        connected = false;
    }
}
```

`volatile` is sufficient here because each operation is a simple read or write of a single field. If the operation
required a check-then-act sequence (e.g., "disconnect only if currently connected"), an `AtomicBoolean` with
`compareAndSet` would be needed instead.

### Example without group

A method annotated with `@@threadSafe` (no group) must be safe for concurrent calls on its own:

```
// Meta-language
DataService {
    @@threadSafe
    void resetStats()
}
```

```java
// Java implementation

import java.util.concurrent.atomic.AtomicLong;

import org.hiero.sdk.annotation.ThreadSafe;

public final class DataService {

    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    @ThreadSafe
    public void resetStats() {
        requestCount.set(0);
        errorCount.set(0);
    }
}
```

### Example with group

Methods and attributes in the same group can be called concurrently with each other. The implementation must ensure that
concurrent access across all members of the group is safe:

```
// Meta-language
DataCache {
    @@threadSafe(cache) data: bytes

    @@threadSafe(cache)
    void updateCache(data: bytes)

    @@threadSafe(cache)
    bytes readCache()

    @@threadSafe
    void resetStats()
}
```

```java
// Java implementation using ReadWriteLock for the 'cache' group

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicLong;

import org.hiero.sdk.annotation.ThreadSafe;

public final class DataCache {

    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

    private byte[] data = new byte[0];

    private final AtomicLong statsCounter = new AtomicLong(0);

    // @@threadSafe(cache) attribute accessor — getter
    @ThreadSafe(group = "cache")
    @NonNull
    public byte[] getData() {
        cacheLock.readLock().lock();
        try {
            return data.clone();
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    // @@threadSafe(cache) attribute accessor — setter
    @ThreadSafe(group = "cache")
    public void setData(@NonNull final byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        cacheLock.writeLock().lock();
        try {
            this.data = data.clone();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    // @@threadSafe(cache) — same group, shares the cacheLock
    @ThreadSafe(group = "cache")
    public void updateCache(@NonNull final byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        cacheLock.writeLock().lock();
        try {
            this.data = data.clone();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    // @@threadSafe(cache) — same group, shares the cacheLock
    @ThreadSafe(group = "cache")
    @NonNull
    public byte[] readCache() {
        cacheLock.readLock().lock();
        try {
            return data.clone();
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    // @@threadSafe (no group) — independent, uses its own synchronization
    @ThreadSafe
    public void resetStats() {
        statsCounter.set(0);
    }
}
```

### Best practices

1. **Avoid blocking whenever possible** — Non-blocking techniques (atomics, volatile, concurrent collections, immutable
   snapshots) should always be the first choice. They do not suspend threads, avoid deadlocks by design, and scale
   better under contention. Only introduce blocking synchronization when the invariant genuinely requires it.
2. **Prefer `Lock` over `synchronized`** — When blocking is unavoidable, use `java.util.concurrent.locks.Lock` instead
   of `synchronized`. Locks provide finer-grained control: they can be acquired and released in different scopes,
   support try-lock and timed-lock patterns to avoid indefinite blocking, and allow read/write separation via
   `ReadWriteLock`. Use `synchronized` only in the simplest cases where none of these advantages are needed.
3. **Prefer `ReadWriteLock` for read-heavy groups** — When a group contains both read and write operations, use
   `ReentrantReadWriteLock` to allow concurrent reads while serializing writes.
4. **Scope the synchronization to the group** — Members of the same `@@threadSafe` group share a single lock or
   synchronization mechanism. Members without a group or in different groups use independent synchronization.
5. **Prefer atomics for simple state** — For single-field updates (counters, flags), `java.util.concurrent.atomic.*`
   types are simpler and more performant than any blocking approach.
6. **Avoid holding locks during callbacks** — Never hold a lock while invoking user-supplied callbacks or listeners.
   This can cause deadlocks if the callback attempts to re-enter the SDK.
7. **Document the synchronization strategy** — Add a brief comment or Javadoc noting which lock protects which group,
   so future maintainers understand the concurrency design.
8. **Do not over-synchronize** — Only add synchronization where `@@threadSafe` is declared. Adding `synchronized` to
   every method kills performance and is explicitly discouraged (see
   [Defensive Implementation](#defensive-implementation)).

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

    CompletionStage<Example> getExample(final String id);

    default Example getExampleSync(final String id, final long timeout, final TimeUnit unit) {
        return getExample(id).toCompletableFuture().get(timeout, unit);
    }

}
```

The sample uses `long timeout, TimeUnit unit` as parameters for the synchronous method.
That is the best practice in Java to ensure that the synchronous method can be called from multiple threads.
Instead of just defining a `long timeoutInMs` the usage of `TimeUnit` is recommended.

## Exception Handling (`@@throws`)

The meta-language `@@throws(error-type-a, error-type-b)` annotation declares which errors a method can produce.
In Java, these map to exception classes.

### Prefer standard Java exceptions

When a meta-language error identifier has a natural equivalent in the Java standard library, use that standard exception
instead of defining a custom one. Java developers already know and handle these exceptions, and frameworks and libraries
are built around them.

| Meta-Language Identifier | Java Exception                            | Rationale                             |
|--------------------------|-------------------------------------------|---------------------------------------|
| `timeout-error`          | `java.util.concurrent.TimeoutException`   | Standard for timeout scenarios        |
| `invalid-argument-error` | `java.lang.IllegalArgumentException`      | Standard for bad input                |
| `illegal-format`         | `java.lang.IllegalArgumentException`      | Standard for malformed input format   |
| `invalid-state-error`    | `java.lang.IllegalStateException`         | Standard for wrong object state       |
| `io-error`               | `java.io.IOException`                     | Standard for I/O failures             |
| `not-found-error`        | `java.util.NoSuchElementException`        | Standard for missing elements         |
| `unsupported-error`      | `java.lang.UnsupportedOperationException` | Standard for unimplemented operations |

Only define a custom exception class when no suitable standard exception exists — typically for SDK-specific error
conditions that have no Java equivalent (e.g., transaction-specific failures, network-specific consensus errors).

### Checked vs. unchecked exceptions

Whether an exception is checked (`extends Exception`) or unchecked (`extends RuntimeException`) is a deliberate design
decision per error type. Both have their place in the SDK:

- **Checked exceptions** are appropriate when the caller is expected to handle the error as part of normal control flow.
  They force the caller to acknowledge the error at compile time. Examples: a transaction that is rejected by the
  network, a query for an entity that may not exist.
- **Unchecked exceptions** are appropriate for programming errors, unexpected failures, or situations where recovery is
  unlikely. Examples: invalid arguments, internal SDK errors, configuration mistakes.

When deciding, consider:

- **Async methods** — For `@@async` methods returning `CompletionStage`, exceptions are delivered through the
  `CompletionStage` failure mechanism. Checked exceptions cannot be declared on the `CompletionStage` type itself, but
  they can still be thrown by the underlying implementation and wrapped by the `CompletionStage`. Callers handle them
  via `exceptionally`, `handle`, or `whenComplete`.
- **Lambda compatibility** — Checked exceptions cannot be thrown from standard functional interfaces (`Consumer`,
  `Function`, etc.) without wrapping. If an exception is commonly encountered in lambda contexts, unchecked may be more
  ergonomic.
- **Cross-SDK consistency** — Most other SDK languages (Go, Rust, Python, JavaScript, Swift) do not distinguish between
  checked and unchecked. The choice is Java-specific and should be documented clearly for each error type.

### Naming convention for custom exceptions

When a custom exception class is needed, the meta-language kebab-case identifier is converted to a Java class name:

- Convert kebab-case to PascalCase
- Drop the `-error` suffix and replace it with `Exception` (e.g., `not-found-error` becomes `NotFoundException`)

### Custom exception class structure

Custom exception classes should follow this pattern. Each custom exception must provide at least two constructors
(message only, and message with cause). Constructors without a message parameter are not allowed — every exception must
carry a descriptive message to support debugging and logging:

```java
// Custom exception for an SDK-specific error (e.g., 'transaction-rejected-error')
public final class TransactionRejectedException extends Exception {

    public TransactionRejectedException(@NonNull final String message) {
        super(Objects.requireNonNull(message, "message must not be null"));
    }

    public TransactionRejectedException(@NonNull final String message, @Nullable final Throwable cause) {
        super(Objects.requireNonNull(message, "message must not be null"), cause);
    }
}
```

### Usage in synchronous and asynchronous methods

```
// Meta-language
@@async
@@throws(not-found-error)
@@nullable TransactionDetails fetchDetails(apiKey: string)
```

```java
// Java implementation — using standard IOException for I/O failures,
// custom NotFoundException for SDK-specific lookup failure
public interface TransactionService {

    // Synchronous: checked exceptions declared in signature
    @Nullable
    TransactionDetails fetchDetailsSync(@NonNull String apiKey, long timeout, @NonNull TimeUnit unit)
            throws NotFoundException;

    // Asynchronous: exceptions delivered through CompletionStage failure
    @NonNull
    CompletionStage<@Nullable TransactionDetails> fetchDetails(@NonNull String apiKey);
}
```

For synchronous methods, checked exceptions are declared in the method signature and thrown directly. For asynchronous
methods, the exception is delivered as the cause of the failed `CompletionStage`. Callers handle it via `exceptionally`,
`handle`, or `whenComplete`:

```java
service.fetchDetails("key-123")
        .

thenAccept(details ->System.out.

println("Found: "+details))
        .

exceptionally(throwable ->{
        if(throwable.

getCause() instanceof NotFoundException){
        System.out.

println("Not found");
            }
                    return null;
                    });
```

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

### Loading a Service Provider

On the consumer side, use `java.util.ServiceLoader` to discover and load provider implementations at runtime. This works
both on the module path (JPMS) and the classpath (`META-INF/services`).

```java
import java.util.ServiceLoader;

public final class ExampleProviderLoader {

    @NonNull
    public static ExampleProvider load() {
        return ServiceLoader.load(ExampleProvider.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No ExampleProvider implementation found"));
    }
}
```

When a module uses `ServiceLoader` to load a service, it must declare this in its `module-info.java`:

```java
module com.example.consumer {
    requires com.example.api;
    uses com.example.api.ExampleProvider;
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
4. **Test edge cases** - null values, empty collections, boundary values
5. **Test error conditions** - verify exceptions are thrown when expected
6. **Independent tests** - tests should not depend on execution order
7. **Fast execution** - unit tests should run in milliseconds

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

## Streaming

The meta-language `@@streaming` annotation declares methods that return an asynchronous stream of items. In Java, the
SDK must provide two consumption modes:

1. **Pull-based** (primary) — The canonical implementation. Returns a `HieroStream<T>` that the consumer iterates over
   using a standard `for` loop or `Iterator`. All retry, reconnect, and domain logic lives here.
2. **Push-based** (convenience adapter) — Built on top of the pull implementation using `java.util.concurrent.Flow`. The
   push adapter contains zero domain logic and is derived automatically from the pull stream.

### The `HieroStream<T>` interface (pull-based)

The primary pull-based API is a custom `HieroStream<T>` interface that extends `Iterable<T>` and `AutoCloseable`. This
allows consumers to use try-with-resources for automatic cleanup and enhanced `for` loops for iteration.
See [java-files/HieroStream.java](java-files/HieroStream.java) for the full source with Javadoc.

```java
public interface HieroStream<T> extends Iterable<T>, AutoCloseable {

    @Override
    @NonNull
    Iterator<T> iterator();

    @Override
    void close();
}
```

Consumer usage:

```
try(HieroStream<StreamItem<TopicMessage>> stream = topicSubscription.subscribe(client)) {
        for(StreamItem<TopicMessage> item :stream) {
            switch(item) {
                case StreamItem.Success<TopicMessage> s -> process(s.value());
                case StreamItem.Error<TopicMessage> e ->log.warn("Bad message",e.error());
            }
        }
} // AutoCloseable cancels the stream and releases resources
```

The `Iterator` returned by `HieroStream` blocks on `next()` until the next item is available. On Java 21+, this is
efficient when consumed on a virtual thread — the virtual thread parks without blocking an OS thread. On older Java
versions, the blocking is real but acceptable for most use cases since the consumer typically dedicates a thread to
stream processing.

### The `StreamItem<T>` sealed interface (per-item Result type)

The meta-language `streamResult<TYPE>` maps to a sealed interface in Java. This type represents a single item in the
stream that is either a success value or an error. It allows per-item error handling without terminating the stream.
See [java-files/StreamItem.java](java-files/StreamItem.java) for the full source with Javadoc.

```java
public sealed interface StreamItem<T> permits StreamItem.Success, StreamItem.Error {

    record Success<T>(@NonNull T value) implements StreamItem<T> {

        public Success {
            Objects.requireNonNull(value, "value must not be null");
        }
    }

    record Error<T>(@NonNull Throwable error) implements StreamItem<T> {

        public Error {
            Objects.requireNonNull(error, "error must not be null");
        }
    }
}
```

Consumer code uses pattern matching (Java 17+) to handle each variant:

```
for(StreamItem<TopicMessage> item : stream) {
    switch(item){
        case StreamItem.Success<TopicMessage> s -> process(s.value());
        case StreamItem.Error<TopicMessage> e ->  log.warn("Item error: {}",e.error().getMessage());
    }
}
```

When a streaming method in the meta-language uses a plain return type (not `streamResult`), the Java stream yields `T`
directly and all errors are terminal — they surface as exceptions from `Iterator.next()` or `Iterator.hasNext()`.

### Push-based adapter using `java.util.concurrent.Flow`

The push-based API is a convenience adapter built on top of the pull-based `HieroStream<T>`. It implements
`java.util.concurrent.Flow.Publisher<T>` (Java 9+) so that consumers can use the standard reactive streams interface.
The adapter contains no domain logic — it drives the pull loop on a virtual thread and delivers items to the
`Flow.Subscriber`. See [java-files/HieroPublisher.java](java-files/HieroPublisher.java) and
[java-files/HieroSubscription.java](java-files/HieroSubscription.java) for the full source with Javadoc.

#### The `HieroPublisher<T>` adapter

```java
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class HieroPublisher<T> implements Flow.Publisher<T> {

    private final HieroStream<T> stream;

    public HieroPublisher(@NonNull final HieroStream<T> stream) {
        this.stream = Objects.requireNonNull(stream, "stream must not be null");
    }

    @Override
    public void subscribe(@NonNull final Flow.Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber must not be null");
        subscriber.onSubscribe(new HieroSubscription<>(stream, subscriber));
    }
}
```

#### The `HieroSubscription<T>` implementation

The subscription drives the pull-based `HieroStream` on a virtual thread and respects backpressure through the
`Flow.Subscription.request(long)` protocol:

```java
final class HieroSubscription<T> implements Flow.Subscription {

    private final HieroStream<T> stream;
    private final Flow.Subscriber<? super T> subscriber;
    private final AtomicLong requested = new AtomicLong(0);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    HieroSubscription(@NonNull final HieroStream<T> stream,
                      @NonNull final Flow.Subscriber<? super T> subscriber) {
        this.stream = stream;
        this.subscriber = subscriber;
        Thread.ofVirtual().start(this::drainLoop);
    }

    @Override
    public void request(final long n) {
        if (n <= 0) {
            cancel();
            subscriber.onError(new IllegalArgumentException("request count must be positive"));
            return;
        }
        requested.addAndGet(n);
    }

    @Override
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            stream.close();
        }
    }

    private void drainLoop() {
        try {
            for (T item : stream) {
                // Wait until demand is available
                while (requested.get() <= 0) {
                    if (cancelled.get()) {
                        return;
                    }
                    Thread.sleep(1); // virtual thread parks cheaply
                }
                if (cancelled.get()) {
                    return;
                }
                requested.decrementAndGet();
                subscriber.onNext(item);
            }
            if (!cancelled.get()) {
                subscriber.onComplete();
            }
        } catch (final Exception e) {
            if (!cancelled.get()) {
                subscriber.onError(e);
            }
        }
    }
}
```

#### Consumer usage with `Flow.Subscriber`

```
HieroStream<StreamItem<TopicMessage>> pullStream = topicSubscription.subscribe(client);
Flow.Publisher<StreamItem<TopicMessage>> publisher = new HieroPublisher<>(pullStream);

publisher.subscribe(new Flow.Subscriber<>() {

    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(@NonNull final Flow.Subscription subscription){
        this.subscription = subscription;
        subscription.request(1); // request first item
    }

    @Override
    public void onNext(@NonNull final StreamItem<TopicMessage> item){
        switch (item) {
            case StreamItem.Success<TopicMessage> s -> process(s.value());
            case StreamItem.Error<TopicMessage> e -> log.warn("Bad message", e.error());
        }
        subscription.request(1); // request next item
    }

    @Override
    public void onError(@NonNull final Throwable throwable){
        log.error("Stream failed", throwable);
    }

    @Override
    public void onComplete() {
        log.info("Stream completed");
    }
});
```

#### Integration with reactive libraries

Since `HieroPublisher<T>` implements the standard `java.util.concurrent.Flow.Publisher<T>` interface, it integrates
directly with any reactive library that supports the Reactive Streams specification:

```
// Project Reactor (via JdkFlowAdapter)

import reactor.adapter.JdkFlowAdapter;

Flux<StreamItem<TopicMessage>> flux = JdkFlowAdapter.flowPublisherToFlux(
        new HieroPublisher<>(topicSubscription.subscribe(client)));
flux.filter(item ->item instanceof StreamItem.Success)
        .map(item ->((StreamItem.Success<TopicMessage>)item).value())
        .subscribe(msg ->process(msg));
```

```java
// RxJava 3 (via jdk9-interop)

import hu.akarnokd.rxjava3.jdkinterop.FlowInterop;

Flowable<StreamItem<TopicMessage>> flowable = FlowInterop.fromFlowPublisher(
        new HieroPublisher<>(topicSubscription.subscribe(client))
);
```

### Why `Flow` over callbacks

The Java `Flow` API (JSR 166, `java.util.concurrent.Flow`) was chosen over a custom callback interface for the
push-based
adapter because:

1. **Standard library** — `Flow` is part of `java.base` since Java 9. No external dependency.
2. **Backpressure built in** — The `request(n)` protocol prevents the producer from overwhelming the consumer. A custom
   callback interface would need to reinvent this.
3. **Reactive Streams interop** — `Flow.Publisher` is the Java standard mapping of the Reactive Streams specification.
   It integrates directly with Project Reactor, RxJava, Vert.x, and any other reactive library via adapter utilities.
4. **Well-understood contract** — The `Flow` specification defines clear rules for `onSubscribe`, `onNext`, `onError`,
   and `onComplete` ordering, thread safety, and cancellation. A custom callback interface would need to document and
   enforce all of these from scratch.

### Architecture: pull is canonical, push is derived

The pull-based `HieroStream<T>` is the single source of truth for all streaming logic. The push-based
`HieroPublisher<T>`
is a stateless adapter that drives the pull loop. This means:

- All retry, reconnect, and node selection logic is implemented once in the pull stream.
- The push adapter contains zero domain logic — it only bridges pull to push.
- Bug fixes and improvements to streaming behavior apply to both consumption modes automatically.
- SDK implementors only need to implement `HieroStream<T>`. The push adapter is provided by the SDK framework.

```
Network (gRPC/WebSocket) → SDK internal retry/reconnect → HieroStream<T> (pull)
                                                              ↓
                                                         HieroPublisher<T> (push adapter)
                                                              ↓
                                                         Flow.Subscriber<T> (consumer)
```

### Providing both APIs from a single method

Streaming methods in the SDK should provide a pull-based return type as the primary API. The push-based `Publisher` can
be obtained by wrapping the pull stream. A convenience method may be provided:

```java
public interface TopicSubscription {

    // Primary: pull-based
    @NonNull
    HieroStream<StreamItem<TopicMessage>> subscribe(@NonNull Client client);

    // Convenience: push-based adapter
    @NonNull
    default Flow.Publisher<StreamItem<TopicMessage>> subscribePublisher(@NonNull final Client client) {
        return new HieroPublisher<>(subscribe(client));
    }
}
```

### Error handling summary

| Error level    | Pull-based (`HieroStream`)                     | Push-based (`Flow.Publisher`)             |
|----------------|------------------------------------------------|-------------------------------------------|
| Per-item error | `StreamItem.Error` variant in the iteration    | `StreamItem.Error` delivered via `onNext` |
| Stream-level   | Exception from `Iterator.next()` / `hasNext()` | Delivered via `onError`                   |
| Cancellation   | `close()` / try-with-resources                 | `Subscription.cancel()`                   |

## Questions & Comments

## Todos based on AI tests for generating Java code
