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
| `date`            | `date::year_month_day`                        | `std::chrono::year_month_day`                     | C++17: Howard Hinnant date library      |
| `time`            | Custom time struct or library type            | `std::chrono::hh_mm_ss<std::chrono::nanoseconds>` | `hh_mm_ss` is C++20                     |
| `dateTime`        | `std::chrono::system_clock::time_point`       | `std::chrono::system_clock::time_point`           | -                                       |
| `zonedDateTime`   | `date::zoned_time<std::chrono::system_clock::duration>` | `std::chrono::zoned_time`                | C++17: Howard Hinnant date library      |
| `function<...>`   | `std::function<...>`                          | `std::function<...>`                              | Use templates for one-shot invocation; use `std::function` when callables are stored |

## 2. Error Model Mapping For @@throws

`@@throws(error-type-a[, ...])` maps to typed C++ error handling at the SDK boundary.

Recommended V3 pattern: use a discriminated result type as the primary model.

- Return `HieroResult<T>` for fallible methods instead of throwing by default.
- Use one stable `HieroError` shape (for example, `code` plus optional message/details).
- Multiple `@@throws(...)` entries still map to the same `HieroResult<T>`; the error code distinguishes cases.
- For methods without a value return, use a status result (for example, `HieroStatus`) that carries success/failure.

Mapping sketch:

```cpp
// Meta-language:
// @@throws(not-found-error, invalid-argument-error)
// AccountBalance getBalance(id: AccountId)

// C++17 translation:
HieroResult<AccountBalance> getBalance(const AccountId& id);

auto result = client.getBalance(id);
if (!result) {
    log(result.error().message);
    return;
}
auto& balance = *result;
```

Recommended pattern:

- Public APIs should expose stable SDK-level error identifiers, not transport-specific status enums.
- Result types are the recommended V3 default in C++17.
- The current V2 SDK uses an exception hierarchy; document this for contributor context.
- Exception hierarchies are still a valid fallback when a project explicitly relies on exceptions.
- Preserve lower-level causes in logs or nested error payloads.

Example exception fallback pattern:

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

Recommended V3 pattern: an explicit client-owned executor or thread pool.

Recommended pattern:

- Submit work items to an explicit thread pool or executor owned by the client/runtime.
- Return a future-like handle from the pooled submission.
- If both sync and async variants exist, keep shared execution logic in one internal implementation.
- Do not mix callback and future styles in a single public method signature.

`std::async(std::launch::async, ...)` note:

- V2-style `std::async` is acceptable for simple, infrequent one-off operations.
- At SDK scale, prefer pooled executors because `std::async` can create one OS thread per call,
  futures are not composable in C++17, and future destruction may block if `.get()` is never called.

Example executor-oriented sketch:

```cpp
#include <future>
#include <memory>
#include <utility>

class Executor {
public:
    template <typename Fn>
    auto submit(Fn&& fn) -> std::future<decltype(fn())>;
};

class Client {
public:
    explicit Client(std::shared_ptr<Executor> executor) : executor_(std::move(executor)) {}

    std::future<int> submitAsync(int request) const {
        return executor_->submit([req = request]() { return req + 1; });
    }

private:
    std::shared_ptr<Executor> executor_;
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
- Use `std::unique_lock<std::mutex>` with `std::condition_variable` when waiting on conditions.
- Use `std::atomic<T>` for simple counters and flags.
- Avoid complex lock-free coordination patterns unless profiling proves they are required.
- Avoid exposing references to internal mutable containers.

Example: mutex and lock_guard for simple mutation

```cpp
#include <cstdint>
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

Example: condition_variable requires unique_lock

```cpp
#include <condition_variable>
#include <mutex>

class BackgroundSignal {
public:
    void notify() {
        {
            std::lock_guard<std::mutex> guard(mutex_);
            ready_ = true;
        }
        condition_.notify_one();
    }

    void waitUntilReady() {
        std::unique_lock<std::mutex> lock(mutex_);
        condition_.wait(lock, [this] { return ready_; });
    }

private:
    bool ready_ = false;
    std::mutex mutex_;
    std::condition_variable condition_;
};
```

Example: atomics for counters and flags

```cpp
#include <atomic>
#include <cstdint>

class RuntimeState {
public:
    void requestStop() noexcept { stopRequested_.store(true, std::memory_order_relaxed); }
    bool isStopRequested() const noexcept { return stopRequested_.load(std::memory_order_relaxed); }
    void incrementInFlight() noexcept { inFlight_.fetch_add(1, std::memory_order_relaxed); }

private:
    std::atomic<bool> stopRequested_{false};
    std::atomic<std::int64_t> inFlight_{0};
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
