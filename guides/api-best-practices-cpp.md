# C++ API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete C++ patterns,
conventions, and ready-to-copy templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

The C++ SDK baseline is C++17. This guide therefore treats C++17 mappings as the default contract.
Where C++20 offers a clearer standard type, that mapping is listed as an optional alternative.

## 1. Type Mapping Table

| Generic Type      | C++17 Mapping (Primary)                       | Optional C++20+ Mapping                           | Notes                                   |
|-------------------|-----------------------------------------------|---------------------------------------------------|-----------------------------------------|
| `string`          | `std::string`                                 | `std::string`                                     | -                                       |
| `intX`            | `int8_t`, `int16_t`, `int32_t`, `int64_t`     | `int8_t`, `int16_t`, `int32_t`, `int64_t`         | From `<cstdint>`                        |
| `uintX`           | `uint8_t`, `uint16_t`, `uint32_t`, `uint64_t` | `uint8_t`, `uint16_t`, `uint32_t`, `uint64_t`     | From `<cstdint>`                        |
| `double`          | `double`                                      | `double`                                          | -                                       |
| `decimal`         | Custom or third-party library                 | Custom or third-party library                     | No standard decimal type                |
| `bool`            | `bool`                                        | `bool`                                            | -                                       |
| `bytes`           | `std::vector<std::byte>`                      | `std::vector<std::byte>`                          | `std::byte` is available in C++17       |
| `list<TYPE>`      | `std::vector<TYPE>`                           | `std::vector<TYPE>`                               | -                                       |
| `set<TYPE>`       | `std::set<TYPE>`                              | `std::set<TYPE>`                                  | -                                       |
| `map<KEY, VALUE>` | `std::map<KEY, VALUE>`                        | `std::map<KEY, VALUE>`                            | Use `std::unordered_map` if needed      |
| `type`            | `std::type_index`                             | `std::type_index`                                 | From `<typeindex>`                      |
| `uuid`            | UUID library type                             | UUID library type                                 | No standard UUID until C++26            |
| `date`            | Custom date struct or library type            | `std::chrono::year_month_day`                     | `year_month_day` is C++20               |
| `time`            | Custom time struct or library type            | `std::chrono::hh_mm_ss<std::chrono::nanoseconds>` | `hh_mm_ss` is C++20                     |
| `dateTime`        | `std::chrono::system_clock::time_point`       | `std::chrono::system_clock::time_point`           | -                                       |
| `zonedDateTime`   | `time_point` plus explicit IANA zone string   | `std::chrono::zoned_time`                         | `zoned_time` is C++20                   |
| `function<...>`   | `std::function<...>`                          | `std::function<...>`                              | Prefer templates for hot paths          |

## 2. Error Model Mapping For @@throws

`@@throws(error-type-a[, ...])` maps to typed C++ error handling at the SDK boundary.

Recommended pattern:

- Public APIs should expose stable SDK-level error identifiers, not transport-specific status enums.
- Prefer a structured error object with `code` and `message` when avoiding exceptions.
- If exceptions are enabled in the SDK, use a base exception type and specific subclasses.
- Preserve lower-level causes in logs or nested error payloads.

Example exception pattern:

```cpp
#include <stdexcept>
#include <string>

class HieroError : public std::runtime_error {
public:
    explicit HieroError(std::string code, std::string message)
        : std::runtime_error(message), code_(std::move(code)) {}

    const std::string& code() const noexcept { return code_; }

private:
    std::string code_;
};

class NotFoundError : public HieroError {
public:
    NotFoundError() : HieroError("not-found-error", "Resource not found") {}
};
```

## 3. Async Model Mapping For @@async

`@@async` should map to non-blocking APIs that return futures or completion objects.

Recommended pattern:

- Use `std::future<T>` for simple async contracts.
- For richer cancellation or continuations, use a runtime-specific async abstraction.
- If both sync and async variants exist, keep shared execution logic in one internal implementation.
- Do not mix callback and future styles in a single public method signature.

Example:

```cpp
#include <future>
#include <vector>
#include <cstdint>

class Client {
public:
    std::future<std::vector<std::uint8_t>> submitAsync(std::vector<std::uint8_t> txBytes) const {
        return std::async(std::launch::async, [payload = std::move(txBytes)]() {
            return payload;
        });
    }
};
```

## 4. Factory And Static Method Conventions

Map `@@static` factory methods to named static constructors or free factory functions when ownership needs to be explicit.

Recommended pattern:

- Use `static Type fromBytes(...)` for value types.
- Use `static std::unique_ptr<Type> create(...)` for polymorphic ownership.
- Keep constructors simple; move parsing and validation into named factories.

Example:

```cpp
#include <cstdint>
#include <string>
#include <vector>

class AccountId {
public:
    static AccountId fromString(const std::string& value);
    static bool isValid(const std::string& value);

private:
    std::uint64_t shard_{};
    std::uint64_t realm_{};
    std::uint64_t num_{};
};
```

## 5. Nullability And Collection Semantics

Map `@@nullable` to explicit optional values and avoid nullable collections.

Recommended pattern:

- Use `std::optional<T>` for nullable single values.
- Never use nullable pointers for collections in public APIs.
- Return empty vectors/maps/sets instead of null-like values.
- Prefer `const T&` for required inputs and `std::optional<T>` for optional inputs.

Example:

```cpp
#include <optional>
#include <string>
#include <vector>

struct TransactionMemo {
    std::optional<std::string> text;
};

struct ValidationResult {
    std::vector<std::string> warnings;
};
```

## 6. Immutability And Thread-Safety Guidance

For `@@immutable`, design value-oriented types with const-correctness and no mutating setters.
For `@@threadSafe`, define clear synchronization and ownership boundaries.

Recommended pattern:

- Prefer value semantics and `const` accessors for immutable models.
- Use `std::unique_ptr` to express ownership transfer.
- Use `std::shared_ptr<const T>` for shared immutable objects.
- Protect shared mutable state with `std::mutex` and scoped locking.
- Avoid exposing references to internal mutable containers.

Example:

```cpp
#include <mutex>
#include <string>

class NonceStore {
public:
    std::uint64_t next() {
        std::lock_guard<std::mutex> guard(mutex_);
        return ++value_;
    }

private:
    std::uint64_t value_ = 0;
    std::mutex mutex_;
};

class Receipt {
public:
    Receipt(std::string transactionId, std::string status)
        : transactionId_(std::move(transactionId)), status_(std::move(status)) {}

    const std::string& transactionId() const noexcept { return transactionId_; }
    const std::string& status() const noexcept { return status_; }

private:
    std::string transactionId_;
    std::string status_;
};
```

## 7. Runnable Build-Sign-Send Example

The following example is a self-contained build-sign-send flow.
It compiles as standard C++17 and demonstrates builder, signing, and submission flow.

```cpp
#include <cstdint>
#include <future>
#include <iostream>
#include <numeric>
#include <string>
#include <vector>

struct SignedTransaction {
    std::vector<std::uint8_t> body;
    std::vector<std::uint8_t> signature;
};

class TransferTransactionBuilder {
public:
    TransferTransactionBuilder& fromAccount(std::string value) {
        from_ = std::move(value);
        return *this;
    }

    TransferTransactionBuilder& toAccount(std::string value) {
        to_ = std::move(value);
        return *this;
    }

    TransferTransactionBuilder& amountTinybar(std::int64_t value) {
        amount_ = value;
        return *this;
    }

    std::vector<std::uint8_t> build() const {
        std::string payload = from_ + "|" + to_ + "|" + std::to_string(amount_);
        return std::vector<std::uint8_t>(payload.begin(), payload.end());
    }

private:
    std::string from_;
    std::string to_;
    std::int64_t amount_ = 0;
};

class PrivateKey {
public:
    explicit PrivateKey(std::vector<std::uint8_t> seed) : seed_(std::move(seed)) {}

    std::vector<std::uint8_t> sign(const std::vector<std::uint8_t>& message) const {
        std::vector<std::uint8_t> out = message;
        out.insert(out.end(), seed_.begin(), seed_.end());
        return out;
    }

private:
    std::vector<std::uint8_t> seed_;
};

class Client {
public:
    std::future<std::string> sendAsync(SignedTransaction tx) const {
        return std::async(std::launch::async, [tx = std::move(tx)]() {
            std::uint64_t checksum = std::accumulate(
                tx.body.begin(),
                tx.body.end(),
                static_cast<std::uint64_t>(0));
            checksum += std::accumulate(
                tx.signature.begin(),
                tx.signature.end(),
                static_cast<std::uint64_t>(0));
            return std::string("ok:") + std::to_string(checksum);
        });
    }
};

int main() {
    auto body = TransferTransactionBuilder()
                    .fromAccount("0.0.1001")
                    .toAccount("0.0.1002")
                    .amountTinybar(100)
                    .build();

    PrivateKey key({1, 2, 3, 4});
    SignedTransaction signedTx{body, key.sign(body)};

    Client client;
    auto receiptFuture = client.sendAsync(std::move(signedTx));
    std::cout << "receipt " << receiptFuture.get() << std::endl;
    return 0;
}
```
