# Language Guide Parity Template

This template defines the minimum required section set for all language best-practice guides.
Use it when creating or expanding any `guides/api-best-practices-*.md` file.

## 1. Type Mapping Table

Document how meta-language types from [API Guideline](../guides/api-guideline.md) map to native language types.

## 2. Error Model Mapping For @@throws

Explain how `@@throws(error-type-a[, ...])` maps to language-native error and exception patterns.
Include naming guidance for stable error identifiers.

## 3. Async Model Mapping For @@async

Explain the language-native async contract for methods annotated with `@@async`.
Document sync and async API pairing conventions if both are exposed.

## 4. Factory And Static Method Conventions

Document how factory constructors and static utility methods should be represented in this language.

## 5. Nullability And Collection Semantics

Document nullability behavior and collection semantics.
Include language-specific behavior for empty collections, optional values, and null or nil handling.

## 6. Immutability And Thread-Safety Guidance

Document immutable-by-default guidance and language-native thread-safety patterns.
Explain how this aligns with `@@immutable` and `@@threadSafe`.

## 7. Runnable Build-Sign-Send Example

Provide one runnable example that covers a build-sign-send flow end-to-end.
The example should be executable and follow the conventions defined in the guide.
