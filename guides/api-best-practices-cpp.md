# C++ API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete C++ patterns,
conventions, and ready‑to‑copy code templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

## Type Mapping

| Generic Type      | C++ Type                                      | Notes                                   |
|-------------------|-----------------------------------------------|-----------------------------------------|
| `string`          | `std::string`                                 | -                                       |
| `intX`            | `int8_t`, `int16_t`, `int32_t`, `int64_t`     | From `<cstdint>`                        |
| `uintX`           | `uint8_t`, `uint16_t`, `uint32_t`, `uint64_t` | From `<cstdint>`                        |
| `double`          | `double`                                      | -                                       |
| `decimal`         | Custom or third-party library                 | No standard decimal type                |
| `bool`            | `bool`                                        | -                                       |
| `bytes`           | `std::vector<std::byte>`                      | C++17+                                  |
| `list<TYPE>`      | `std::vector<TYPE>`                           | -                                       |
| `set<TYPE>`       | `std::set<TYPE>`                              | -                                       |
| `map<KEY, VALUE>` | `std::map<KEY, VALUE>`                        | Or `std::unordered_map` for performance |
| `date`            | `std::chrono::year_month_day`                 | C++20                                   |
| `time`            | `std::chrono::time_of_day`                    | C++20                                   |
| `dateTime`        | `std::chrono::system_clock::time_point`       | -                                       |
| `zonedDateTime`   | `std::chrono::zoned_time`                     | C++20                                   |

## Implementing Abstractions

### Contract types (`contract` keyword)

Implement as a class with only pure virtual methods (an abstract interface class). Generic type parameters (`$$T`)
map to C++ templates.

```cpp
// Meta-language:
//   contract Executable<$$Response> {
//       @@async $$Response execute(client: HieroClient)
//   }

template<typename Response>
class Executable {
public:
    virtual ~Executable() = default;
    virtual Response execute(const HieroClient& client) = 0;
    virtual std::future<Response> executeAsync(const HieroClient& client) = 0;
};
```

### Base types (`abstraction` keyword)

Implement as an abstract class with member variables and non-pure-virtual methods providing shared implementation.

```cpp
// Meta-language:
//   abstraction Request {
//       @@default(10) maxAttempts: int32
//       protected $$Result withRetry(...)
//   }

class Request {
protected:
    int maxAttempts_ = 10;
    std::chrono::milliseconds maxBackoff_{8000};
    std::chrono::milliseconds minBackoff_{250};
    std::optional<std::chrono::milliseconds> grpcDeadline_;
    std::optional<std::chrono::milliseconds> requestTimeout_;

    template<typename Node, typename Result>
    Result withRetry(
        Network<Node>& network,
        std::function<Result(const Node&)> action,
        std::function<bool(std::exception_ptr)> shouldRetry);
};
```

### Mixed inheritance (abstraction + contract)

Use single class inheritance for the abstraction and additional public inheritance for contracts. Since contract classes
have only pure virtual methods and a virtual destructor, this avoids diamond inheritance issues.

```cpp
// Meta-language:
//   abstraction Transaction extends ConsensusRequest, Executable<TransactionResponse> { ... }

template<typename SdkRequestType>
class Transaction
    : public ConsensusRequest                        // class inheritance (abstraction)
    , public Executable<TransactionResponse> {       // interface implementation (contract)
    // ...
};
```

### Protected methods

Map `protected` directly to the C++ `protected` access specifier.

### Callback parameters

Map callbacks to `std::function`:

| Meta-language | C++ |
|---|---|
| `void callback()` | `std::function<void()>` |
| `void callback(item: $$T)` | `std::function<void(const T&)>` |
| `bool callback(error: $$Error)` | `std::function<bool(std::exception_ptr)>` |
| `$$R callback(node: $$N)` | `std::function<R(const N&)>` |
