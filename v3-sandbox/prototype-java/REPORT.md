# Implementation report — Java prototype of the V3 public API

This report documents every place where the language-agnostic API definitions in
[`v3-sandbox/prototype-api`](../prototype-api/) were ambiguous, contradictory, or
simply not implementable from the meta-language alone. It is meant as input for the
next iteration of the API design.

The report is organised by namespace and follows a consistent structure:
**Issue → What the meta-language says → How the Java prototype handled it →
Recommendation**.

---

## 0. Cross-cutting issues

### 0.1 Placeholder namespaces (`grpc`, `hiero-proto`)

- **What the meta-language says:** Both `grpc.md` and `hiero-proto.md` only declare
  the namespace name; no types or methods are defined. `transactions-spi.md`
  references `grpc.MethodDescriptor`, `hiero-proto.TransactionBody`,
  `hiero-proto.TransactionResponse`, `hiero-proto.TransactionReceipt` and
  `hiero-proto.TransactionRecord` even though those types do not exist.
- **Prototype handling:** The Java `TransactionSupport` SPI was reduced to its
  language-agnostic core (`getTransactionType()` only). The four `convert(...)` and
  `updateBody(...)` methods cannot be modeled because their parameter types are
  undefined.
- **Recommendation:** Either fully define a public proto/grpc abstraction in the
  meta-language (preferred — keeps the API portable across languages) **or**
  declare these namespaces SDK-language-specific and document that the SPI cannot
  be expressed in the meta-language.

### 0.2 `uint8`/`uint16`/`uint64` mapping

- **What the meta-language says:** All three are listed in the basic-data-type
  table; the Java best-practice guide maps them to `byte` / `short` / `int` /
  `long`. Java has no unsigned primitive types.
- **Prototype handling:** `Address.shard/realm/num` and `ConsensusNode.port` were
  mapped to `long`/`int` with explicit range checks (e.g. `port` in `[0, 0xFFFF]`,
  shard/realm/num must be non-negative). Negative inputs are rejected.
- **Recommendation:** Add an explicit "unsigned semantics" note to the Java best
  practice that callers may pass any value but the SDK must reject negative ones.
  Currently the rule is implicit and easy to forget.

### 0.3 The meta-language has no exception type system

- **What the meta-language says:** `@@throws(error-identifier)` only declares a
  stable identifier (e.g., `not-found-error`). The Java best-practice guide maps
  some standard identifiers to standard JDK exceptions but does not list all of
  them — `illegal-format` is used in `keys.md`, `common.md` and `transactions.md`
  but is not in the standard mapping table.
- **Prototype handling:** `illegal-format` was mapped to
  `java.lang.IllegalArgumentException`. `not-found-error` was mapped to
  `java.util.NoSuchElementException`.
- **Recommendation:** Extend the standard mapping table in api-best-practices-java
  to include `illegal-format → IllegalArgumentException` and
  `not-found-error → NoSuchElementException`.

### 0.4 JPMS module split

- **What the api guideline says:** "Each module must have a `module-info.java`",
  and namespaces should map to one JPMS module each.
- **Prototype handling:** The prototype is a single Maven module with **one**
  `module-info.java` that exports all public namespaces from the same JPMS module.
  Splitting one Maven artifact into multiple JPMS modules requires non-trivial
  build configuration that was out of scope for the prototype.
- **Recommendation:** When the prototype is promoted to an MVP, it must be split
  into one Maven module per namespace. The current package layout already mirrors
  the namespace boundaries, so the split should be mechanical.

### 0.5 Hyphenated namespace names (`transactions-accounts`, `transactions-spi`)

- **What the meta-language says:** Hyphens are used in two namespace names. The
  api guideline only specifies "lowerCamelCase" namespace names but the existing
  files use kebab-case for sub-namespaces.
- **Prototype handling:** Mapped to dotted Java sub-packages
  `org.hiero.transactions.accounts` and `org.hiero.transactions.spi`. There is no
  rule in the api-best-practices guide describing this — guess based on intuition.
- **Recommendation:** Add a clear rule to the api guideline: either disallow
  hyphens entirely (`transactionsAccounts`?) or document that hyphens denote
  sub-namespaces and map to dotted packages in language-specific guidelines.

### 0.6 Constructor / static-factory ergonomics for type "namespace functions"

- **What the meta-language says:** Several namespaces define functions that have
  no obvious owning type (e.g., `registerNetworkSetting` and
  `getNetworkSetting` in `config`, `getTransactionSupport` and
  `getAllTransactionSupports` in `transactions-spi`). The api-best-practice rule
  "factories should be implemented as static methods on the type that is created"
  doesn't apply because the returned types are interfaces with no obvious holder.
- **Prototype handling:** Created two dedicated static-helper classes,
  `org.hiero.config.NetworkSettings` and
  `org.hiero.transactions.spi.TransactionSupportRegistry`, that group the related
  namespace-level functions. Both also expose the namespace constants
  (`HEDERA_MAINNET_IDENTIFIER`, …).
- **Recommendation:** Make the api-best-practices guide explicit that
  namespace-level functions/constants without an obvious owning type should live in
  a `<NamespaceName>s` (plural) static helper.

---

## 1. `common` namespace (`common.md`)

### 1.1 `HbarUnit` carries both attributes and a `static` method

- **What the meta-language says:**
  ```
  enum HbarUnit {
      TINYBAR ...
      @@immutable symbol: String
      @@immutable tinybars: int64
      static list<HbarUnit> values()
  }
  ```
- **Issue:** The api-guideline.md grammar for enums (lines 207-234) doesn't declare
  `static` as a valid modifier and doesn't show how a `static` method on an enum
  is supposed to be invoked or what its semantics are. `String` is also written in
  PascalCase (Java-style) instead of the meta-language `string`.
- **Prototype handling:** Mapped `static list<HbarUnit> values()` to a Java
  `valuesAsList()` static factory because Java enums already have a built-in
  `values()` method that returns an array. The clash with the auto-generated
  `values()` makes the meta-language declaration impossible to translate
  one-to-one.
- **Recommendation:** Either drop the `static values()` declaration entirely
  (Java's built-in `values()` already covers it) or move it to a namespace-level
  function. Also fix the typo `String` → `string`.

### 1.2 `HBarExchangeRate` has an inconsistent name

- **Issue:** `HBarExchangeRate` mixes uppercase "HB" (used nowhere else in the
  spec) with PascalCase. Other types use `Hbar` (single uppercase H) consistently.
- **Prototype handling:** Renamed to `HbarExchangeRate` in Java per the
  PascalCase / single-uppercase-acronym convention.
- **Recommendation:** Rename in the meta-language to `HbarExchangeRate` for
  consistency.

### 1.3 `Ledger.id : bytes` plus immutability

- **Issue:** The meta-language defines `@@immutable id: bytes`, but Java arrays
  are mutable references and a Java `record` would expose the internal `byte[]`
  reference unmodifiably. There is no guidance in the best-practices guide on how
  to model immutable byte arrays.
- **Prototype handling:** Implemented `Ledger` as a non-record `final class` with
  a defensive copy on construction and on every getter call. Custom `equals` /
  `hashCode` / `toString` are required.
- **Recommendation:** Add a section to api-best-practices-java about
  "Immutable byte arrays" and what pattern to use (defensive copy class vs.
  `ByteBuffer.asReadOnlyBuffer()` vs. a custom `ImmutableBytes` wrapper).

### 1.4 `Address` is declared as `abstraction` but the only subtype provided is
`AccountId`

- **Issue:** `AccountId extends Address` provides no additional fields or methods
  — the abstraction has no observable refinement. It is unclear whether this is
  meant as a marker pattern or whether other subclasses (e.g., `TopicId`,
  `FileId`) are coming.
- **Prototype handling:** Modeled as a Java `interface` with `AccountId record`
  implementing it.
- **Recommendation:** Either remove the `Address` abstraction (the API consumer
  never sees it as a polymorphic type yet) or list which sub-types are planned.

### 1.5 `AccountId.fromString` is a "namespace function" but is grouped with the
type

- **Issue:** The block of free-standing factories at the bottom of `common.md`
  contains `fromString(accountId: string)` whose obvious owner is `AccountId`. The
  spec says they "should be added to the namespace in the best language dependent
  way" — that is too open.
- **Prototype handling:** Mapped to `AccountId.fromString(...)` per the
  best-practice "factories on the type that is created" rule.
- **Recommendation:** State explicitly in the meta-language that
  free-standing factories are static methods on the type they return.

### 1.6 `validateChecksum(ledger)` algorithm is undefined

- **Issue:** The actual checksum algorithm is not defined anywhere — only the
  contract that validation is ledger-specific.
- **Prototype handling:** Throws `UnsupportedOperationException`. The current V2
  Java SDK has a working algorithm; porting it is straightforward but the
  algorithm needs to be in the spec, not in code.
- **Recommendation:** Document the algorithm (and any per-ledger parameters)
  either in `common.md` or in a dedicated cross-cutting "checksum algorithm" doc.

### 1.7 `int64` overflow on `Hbar.amount * unit.tinybars`

- **Issue:** The meta-language has no native overflow semantics. `100_000_000_000
  Hbar * GIGABAR` overflows `int64` silently.
- **Prototype handling:** Constructor performs an explicit `BigInteger`
  multiplication and rejects values whose result has more than 63 bits.
- **Recommendation:** Either define the overflow semantics in the meta-language
  ("undefined behaviour" / "throw `out-of-range-error`") or restrict
  `amount` to a smaller integer type.

---

## 2. `config` namespace (`config.md`)

### 2.1 The `NetworkSetting` Java type for "immutable getter return value"

- **What the meta-language says:**
  ```
  @@immutable Set<common.ConsensusNode> getConsensusNodes()
  ```
  using `@@immutable` on a method declaration.
- **Issue:** The api guideline only documents `@@immutable` as an **attribute**
  annotation. Its meaning on a method declaration is not formally defined. The
  intent is clearly "the returned set is immutable", but `@@immutable` was not
  designed for that.
- **Prototype handling:** Returned `Set.copyOf(...)` snapshots from the
  in-memory implementation; the interface contract documents the immutability.
- **Recommendation:** Either drop `@@immutable` from method return types and rely
  on the type system (always return immutable collections per the api-guideline
  rule), or formally extend `@@immutable` to also be a method-return-type
  annotation.

### 2.2 Open question carried over from `config.md`

- The author notes "Do we want to have mirror node information in the
  configuration at all or should v3 do a concrete split between mirror node and
  consensus node?". This is a structural decision that affects whether
  `NetworkSetting` should be one type or two. The Java prototype assumed one
  combined type because that is what is currently in the meta-language.

---

## 3. `keys` namespace (`keys.md`)

### 3.1 Two `toBytes` / `toString` overloads with different parameter types

- **What the meta-language says:** Inside `abstraction Key`:
  ```
  @@throws(illegal-format) bytes toBytes(container: keys.io.EncodedKeyContainer)
  @@throws(illegal-format) string toString(container: keys.io.EncodedKeyContainer)
  ...
  @@throws(illegal-format) bytes toBytes(container: EncodedKeyContainer)
  @@throws(illegal-format) string toString(container: EncodedKeyContainer)
  ```
- **Issue:** Two pairs of methods with effectively the same signature, one using
  the qualified name `keys.io.EncodedKeyContainer` and one using the unqualified
  `EncodedKeyContainer`. Neither name is defined as a type in the namespace —
  `KeyFormat` is the closest match.
- **Prototype handling:** Collapsed both pairs to a single
  `toBytes(KeyFormat)` / `toString(KeyFormat)` method.
- **Recommendation:** Remove one of the duplicated declarations and rename
  `EncodedKeyContainer` consistently to `KeyFormat`. The current text reads as a
  mid-rename artifact.

### 3.2 `RawFormate` typo

- **What the meta-language says:** `enum RawFormate`
- **Issue:** Almost certainly a typo for `RawFormat`.
- **Prototype handling:** Used the corrected name `RawFormat` in Java.
- **Recommendation:** Fix the typo.

### 3.3 `byte[] decode(...)` syntax in enum methods

- **What the meta-language says:** `byte[] decode(keyType : keys.KeyType, value : string)`
- **Issue:** `byte[]` is Java syntax, not the meta-language `bytes`. Spaces around
  the colon (`keyType : keys.KeyType`) also do not match the rest of the file
  (`name: string`). The `boolean` return type elsewhere is also Java syntax —
  the meta-language type is `bool`.
- **Prototype handling:** Translated to the corrected types, but the source spec
  needs to be cleaned up.
- **Recommendation:** Run a consistency pass over `keys.md` to use only
  meta-language types (`bytes`, `bool`, `string`) and consistent
  `name: type` formatting.

### 3.4 The `KeyFormat` enum has methods that other enums don't

- **What the meta-language says:** `KeyFormat` declares
  `boolean supportsType(...)` and `decode(...)`. The api-guideline allows methods
  on enums, but it is not clear whether such methods *must* be `static`,
  *instance*, or either.
- **Prototype handling:** Modeled as instance methods (the existing
  `prototype-keys-java` did the same).
- **Recommendation:** Make method semantics explicit for enums in the
  api-guideline.

### 3.5 Open question still in keys.md

- The "Questions & Comments" section makes clear that the convenience overload
  `createPrivateKey(value: string)` / `createPublicKey(value: string)` is not yet
  decided. The Java prototype mirrors the existing `prototype-keys-java` choice
  (PKCS#8/SPKI PEM), but this should be confirmed.

### 3.6 `KeyPair` field order vs. existing prototype

- **Issue:** `keys.md` declares `KeyPair` as `(publicKey, privateKey)` while the
  existing Java `prototype-keys-java` test classes construct it as
  `(privateKey, publicKey)` in places — a mismatch the api-best-practices guide
  also reproduces in its example.
- **Prototype handling:** Used the meta-language order `(publicKey, privateKey)`
  in this prototype to match the spec.
- **Recommendation:** Update the api-best-practices Java guide example so it
  matches the meta-language order.

---

## 4. `client` namespace (`client.md`)

### 4.1 `HieroClient` is mostly a `TO_BE_DEFINED_IN_FUTURE_VERSIONS` placeholder

- **Issue:** The meta-language type only exposes `operatorAccount` and `ledger`.
  All real client functionality (gRPC submission, async receipt/record polling,
  retry policy, …) is deferred. The Java prototype `HieroClient` interface
  therefore cannot offer anything beyond two getters.
- **Prototype handling:** Implemented the two getters and a static factory; the
  `transactions` layer reaches into a private method on `HieroClientImpl` to
  obtain the wired-in `NetworkSetting` because the public interface does not
  expose it.
- **Recommendation:** Define what the client must expose. At minimum:
  - access to the consensus node selection strategy (round-robin, sticky, …)
  - access to the network setting (already implicit but currently private)
  - default execution config (max attempts, backoff, …)

### 4.2 Open question: mutability of `operatorAccount`

- The reviewer note `Should the operatorAccount of HieroClient be immutable?`
  is unresolved. The prototype assumed immutable per the meta-language
  `@@immutable` annotation; if it should be mutable, a setter must be added.

---

## 5. `transactions` namespace (`transactions.md`)

### 5.1 `TransactionBuilder` generic naming inconsistency

- **What the meta-language says:**
  ```
  abstraction TransactionBuilder<$$Transaction extends TransactionBuilder, $$Response extends Response>
  ```
- **Issue:** The first generic parameter is named `$$Transaction` but is
  constrained to `extends TransactionBuilder` — i.e., it is the **builder** type,
  not the **transaction** type. Reading the declaration is confusing. The bound
  also has no generic arguments, leading to a raw-type-style usage.
- **Prototype handling:** The Java `TransactionBuilder` interface uses the
  parameter names `SELF` (CRTP), `RECEIPT`, `RECORD`, `RESP` so the intent is
  obvious.
- **Recommendation:** Either rename `$$Transaction → $$Self` (or `$$Builder`)
  and add the missing generic args to the bound, or document that the
  meta-language is intentionally raw at this point.

### 5.2 `TransactionBuilder` references `Receipt`/`Record` indirectly

- **Issue:** The builder declares `$$Response extends Response` but `Response`
  itself is `Response<$$Receipt extends Receipt, $$Record extends Record>` — a
  generic two-parameter type. Without naming the receipt/record explicitly in the
  builder declaration, there is no way to write a fluent
  `buildAndExecute(...)` that returns the right concrete `Response` subclass.
- **Prototype handling:** The Java type adds the two extra type parameters
  (`RECEIPT`, `RECORD`) to make the chain fully type-safe.
- **Recommendation:** Update the meta-language declaration to carry the receipt
  and record type parameters explicitly.

### 5.3 `Transaction` is `@@finalType` but the meta-language defines instance
methods that need polymorphism (e.g., `unbuild()`)

- **Issue:** `unbuild()` returns `TransactionBuilder` (the abstraction), but in
  practice it must return the **concrete** builder subclass that originally
  produced the transaction. With a raw return type, callers receive a useless
  base reference and must downcast — defeating the purpose of CRTP.
- **Prototype handling:** Returns `TransactionBuilder<?, ?, ?, ?>` and throws
  `UnsupportedOperationException`. A real implementation would need to ask the
  SPI registry (using the body's stored builder type) and downcast.
- **Recommendation:** Either parametrise `Transaction` itself with the original
  builder type, or add a `Transaction unbuild(type: $$Builder)` overload.

### 5.4 `BasicTransactionStatus` is incomplete

- **What the meta-language says:**
  ```
  enum BasicTransactionStatus extends TransactionStatus {
      OK
      INVALID_TRANSACTION
      PAYER_ACCOUNT_NOT_FOUND
      ...
      GRPC_WEB_PROXY_NOT_SUPPORTED
  }
  ```
- **Issue:** The literal `...` is in the source — the full set of status codes
  is missing. Concrete numeric `code:int32` values are also missing for the
  listed names; the meta-language only declares the field exists.
- **Prototype handling:** Implemented only the four named values with placeholder
  numeric codes (`0..3`).
- **Recommendation:** Either enumerate the complete list with the canonical
  `code` values from the consensus node, or remove `BasicTransactionStatus`
  entirely and require services to register their own statuses via the SPI.

### 5.5 `enum extends abstraction` is unusual

- **Issue:** `BasicTransactionStatus extends TransactionStatus` mixes "enum"
  (closed set) with "abstraction" (open extension). This is the only place in the
  meta-language where it happens. In Java, enums cannot extend interfaces with
  default fields without becoming brittle.
- **Prototype handling:** Implemented `TransactionStatus` as an interface and the
  enum implements it. Custom non-enum implementations are also possible.
- **Recommendation:** Document the "enum implements abstraction" pattern in the
  api guideline so other namespaces can use it consistently.

### 5.6 `nodeAccountIds` lacks an `@@nullable` decision

- **Issue:** All other body fields on `TransactionBuilder` are explicitly
  `@@nullable`. `nodeAccountIds` is a list and is not annotated. The api guideline
  says collections should never be nullable, but the api-best-practices guide
  says "do not treat null and an empty collection as the same value" — leaving
  unclear whether an unset list is "empty" or "default to network".
- **Prototype handling:** Treated empty list as "unset" and lets `build(client)`
  populate it from the client's `NetworkSetting`.
- **Recommendation:** Document the semantic of an empty `nodeAccountIds` list.

### 5.7 Async-only `execute(...)` and `queryReceipt`/`queryRecord`

- **Issue:** The meta-language only specifies an `@@async` variant. The Java
  best-practice says "an API should provide a synchronous method as alternative".
- **Prototype handling:** Only the async variant exists in the prototype because
  the meta-language doesn't declare a sync variant. A real implementation should
  add a sync `executeSync(client, timeout, unit)`.
- **Recommendation:** Either always provide a synchronous variant in the
  meta-language or document that the synchronous variant is a Java-only
  ergonomic add-on.

### 5.8 Open questions still in `transactions.md`

- Mutability of `HieroClient.operatorAccount` (see §4.2).
- Whether `maxTransactionFee` and `validDuration` should have default values.

---

## 6. `transactions-accounts` namespace (`transactions-accounts.md`)

### 6.1 `AccountCreateTransactionBuilder` extends a generic with one parameter
only

- **What the meta-language says:**
  ```
  AccountCreateTransactionBuilder extends transactions.TransactionBuilder<AccountCreateTransactionBuilder>
  ```
- **Issue:** `TransactionBuilder` has **two** generic parameters in
  `transactions.md` but is invoked here with **one**. The Java compiler would
  reject this.
- **Prototype handling:** The Java prototype passes all four parameter types
  explicitly:
  ```java
  AccountCreateTransactionBuilder extends AbstractTransactionBuilder<
          AccountCreateTransactionBuilder,
          AccountCreateReceipt,
          AccountCreateRecord,
          AccountCreateResponse>
  ```
- **Recommendation:** Fix `transactions-accounts.md` to provide both
  parameters (and ideally the receipt + record per §5.2).

### 6.2 `@@default(0) initialBalance:common.Hbar`

- **Issue:** `Hbar` is a complex type, not a primitive numeric. The literal `0`
  must be implicitly converted to `Hbar(0, TINYBAR)` (?) or `Hbar(0, HBAR)` (?).
  The api guideline doesn't define how literal defaults bind to complex types.
- **Prototype handling:** Defaulted to `new Hbar(0L, HbarUnit.HBAR)`. The choice
  of unit is arbitrary and should be specified.
- **Recommendation:** Either restrict `@@default(value)` to primitive types or
  add a syntax for complex defaults (e.g., `@@default(Hbar.ZERO)`).

### 6.3 `key:keys.PublicKey` is required at build time but no annotation enforces
that

- **Issue:** The field is not `@@nullable` and not `@@default(...)`, so by the
  api-guideline rule "at least one must be set by the constructor to not end in an
  invalid state". But the builder pattern in `transactions.md` does not use a
  constructor — fields are set after construction via fluent setters. There is no
  declared way to express "required by `build()` but not by the constructor".
- **Prototype handling:** Stored as `@Nullable` internally and threw
  `IllegalStateException` from `build()` if still unset.
- **Recommendation:** Add a `@@requiredAtBuild` annotation or document the
  builder semantics explicitly.

### 6.4 The receipt/record/response types provide little extra over the generics

- **Observation:** `AccountCreateReceipt` only adds `accountId`. The same for
  `AccountCreateRecord`. This is fine, but the spec should make explicit that
  domain-specific subclasses are expected to add domain-specific fields.

---

## 7. `transactions-spi` namespace (`transactions-spi.md`)

### 7.1 The whole SPI depends on `grpc` and `hiero-proto` placeholders

- See §0.1 for the impact. The Java prototype reduced `TransactionSupport` to its
  core `getTransactionType()` method only. The `convert(...)` and `updateBody(...)`
  methods cannot be implemented until the proto namespace is defined.

### 7.2 `MethodDescriptor` is a Java/gRPC concept

- The author already raised this question in the file: "Is `MethodDescriptor`
  specific for Java or is it the same for all languages?" Other languages
  (e.g., Rust `tonic`, Python `grpcio`) have similar but not identical concepts.
  The cleanest fix is to define an SDK-internal `RpcEndpoint` complex type that
  carries `(serviceName, methodName, requestType, responseType)` — see the
  author's third question in `transactions-spi.md`.

### 7.3 SPI factory functions are not on a type

- See §0.6. Implemented as static methods on
  `TransactionSupportRegistry`.

---

## 8. Issues with the api-best-practices-java guide itself

### 8.1 The guide refers to `EncodedKeyContainer` while the meta-language uses
`KeyFormat`

- The Java guide example uses `EncodedKeyContainer.SPKI_WITH_PEM` whereas
  `keys.md` defines `KeyFormat`. The two should be reconciled.

### 8.2 The async return type rule conflicts with the practical SDK V2 code

- The guide says `@@async` → `CompletionStage<T>`. The existing
  `prototype/sdk/.../HieroClient.java` returns `CompletableFuture<T>` directly. If
  V2 → V3 migration is a goal, the guide should mention the migration path or
  loosen the rule for the prototype.

### 8.3 The `@ThreadSafe` annotation is referenced but the meta-language doesn't
use it consistently

- The meta-language `@@threadSafe` annotation is only listed in the syntax
  section, not used on any actual API types in `prototype-api/`. The Java
  prototype uses `@ThreadSafe` on the static registry methods because the
  api-best-practice rule expects them, even though the meta-language doesn't
  declare it.

---

## 9. Items intentionally not implemented in the prototype

| Item                                     | Reason                                                                  |
|------------------------------------------|-------------------------------------------------------------------------|
| Real `Transaction.toBytes()`             | Wire format requires the `hiero-proto` namespace which is a placeholder |
| Real `Transaction.fromBytes()`           | Same                                                                    |
| `Transaction.execute(client)` over gRPC  | `grpc` namespace is a placeholder                                       |
| `Receipt`/`Record` polling               | Same                                                                    |
| `unbuild()` round-tripping               | Requires SPI lookup of the stored builder type — see §5.3               |
| `validateChecksum(ledger)`               | Algorithm is not defined in the spec — see §1.6                         |
| One JPMS module per namespace            | Maven multi-module split is out of scope for the prototype — see §0.4   |
| Sync variants of async methods           | Not in the meta-language — see §5.7                                     |
| Full enumeration of `BasicTransactionStatus` | Spec is incomplete — see §5.4                                       |

These items are all technically implementable; they need either spec clarifications
(§§1.6, 5.4) or proper definitions of the placeholder namespaces (§§0.1, 5.3).

---

## 10. Things that worked well

- The meta-language enum/record/abstraction split mapped cleanly to Java enums,
  records and interfaces. Where the prototype did not need to invent any
  ergonomics (e.g., the simple data types in `common`), the translation was
  mechanical.
- The CRTP pattern documented in `transactions.md` translates one-to-one to a
  Java self-bounded generic.
- The api-best-practice rule about not exposing concrete collection
  implementations is easy to follow with `Set.copyOf` / `List.copyOf`.
- The defensive null-check rule (`Objects.requireNonNull(x, "x must not be null")`)
  is unambiguous and made the implementation pleasantly mechanical.
- Reusing the existing `prototype-keys-java` impl for the keys namespace was
  smooth — the only changes were package renames and adding `@NonNull` /
  `@Nullable` annotations consistently.
