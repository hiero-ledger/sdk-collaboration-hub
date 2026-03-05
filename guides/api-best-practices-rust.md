# Rust API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete Rust patterns,
conventions, and ready‑to‑copy code templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

## Type Mapping

| Generic Type      | Rust Type                                       | Notes                                       |
|-------------------|-------------------------------------------------|---------------------------------------------|
| `string`          | `String` or `&str`                              | Use `String` for owned, `&str` for borrowed |
| `intX`            | `i8`, `i16`, `i32`, `i64`, `i128`               | -                                           |
| `uintX`           | `u8`, `u16`, `u32`, `u64`, `u128`               | -                                           |
| `double`          | `f64`                                           | -                                           |
| `decimal`         | Third-party crate (e.g., `rust_decimal`)        | -                                           |
| `bool`            | `bool`                                          | -                                           |
| `bytes`           | `Vec<u8>` or `&[u8]`                            | -                                           |
| `list<TYPE>`      | `Vec<TYPE>`                                     | -                                           |
| `set<TYPE>`       | `HashSet<TYPE>` or `BTreeSet<TYPE>`             | -                                           |
| `map<KEY, VALUE>` | `HashMap<KEY, VALUE>` or `BTreeMap<KEY, VALUE>` | -                                           |
| `date`            | `chrono::NaiveDate`                             | Using `chrono` crate                        |
| `time`            | `chrono::NaiveTime`                             | Using `chrono` crate                        |
| `dateTime`        | `chrono::NaiveDateTime`                         | Using `chrono` crate                        |
| `zonedDateTime`   | `chrono::DateTime<Tz>`                          | Using `chrono` crate                        |

## Implementing Abstractions

Rust has no class inheritance. The meta-language `contract` and `abstraction` keywords map to either a Rust `trait` or
a generic struct depending on which keyword is used.

### Contract types (`contract` keyword)

Implement as a Rust `trait`. Contracts define only method signatures. Generic type parameters (`$$T`) map to either
trait generics or associated types.

```rust
// Meta-language:
//   contract Executable<$$Response> {
//       @@async $$Response execute(client: HieroClient)
//   }

pub trait Executable {
    type Response;
    fn execute(&self, client: &HieroClient) -> Result<Self::Response>;
}
```

Use associated types when each implementor has exactly one natural response type. Use generics when the same
implementor might satisfy the trait for multiple type arguments.

### Base types (`abstraction` keyword)

Implement as a struct containing the shared fields, composed into subtypes via a field (not inheritance). Subtypes
hold an instance of the base struct and delegate shared behavior to it.

```rust
// Meta-language:
//   abstraction Request {
//       @@default(10) maxAttempts: int32
//       protected $$Result withRetry(...)
//   }

pub struct RequestConfig {
    pub max_attempts: u32,
    pub max_backoff: Duration,
    pub min_backoff: Duration,
    pub grpc_deadline: Option<Duration>,
    pub request_timeout: Option<Duration>,
}

impl RequestConfig {
    pub(crate) fn with_retry<N, R>(
        &self,
        network: &dyn Network<N>,
        action: impl Fn(&N) -> Result<R>,
        should_retry: impl Fn(&Error) -> bool,
    ) -> Result<R> {
        // shared retry loop implementation
    }
}
```

### Mixed inheritance (base + contracts)

Use the wrapper struct pattern: a generic struct holds the base config and a data type parameter. The struct
implements the contract trait by delegating to the data type's specific methods.

```rust
// Meta-language:
//   abstraction Transaction extends ConsensusRequest, Executable<TransactionResponse> { ... }

pub struct Transaction<D: TransactionExecute> {
    pub(crate) config: RequestConfig,
    pub(crate) node_account_ids: Option<Vec<AccountId>>,
    pub(crate) data: D,
}

impl<D: TransactionExecute> Executable for Transaction<D> {
    type Response = TransactionResponse;

    fn execute(&self, client: &HieroClient) -> Result<Self::Response> {
        self.config.with_retry(
            client.consensus_network(),
            |node| { /* build, send, map */ },
            |err| self.data.should_retry(err),
        )
    }
}
```

### SPI / data traits

For each base abstraction, define a companion trait that concrete types implement to customize behavior (the SPI
pattern). This replaces the virtual method override pattern from class-based languages.

```rust
pub(crate) trait TransactionExecute: Send + Sync {
    fn execute(&self, channel: Channel, request: services::Transaction)
        -> BoxGrpcFuture<services::TransactionResponse>;
    fn should_retry(&self, error: &Error) -> bool;
}
```

### Protected / internal methods

Use `pub(crate)` visibility. This restricts access to the crate (package) while allowing all internal types to call
the method.

### Callback parameters

Map callbacks to Rust function traits:

| Meta-language | Rust |
|---|---|
| `void callback()` | `impl Fn() + Send` or `Box<dyn Fn() + Send>` |
| `void callback(item: $$T)` | `impl Fn(T) + Send` or `Box<dyn Fn(T) + Send>` |
| `bool callback(error: $$Error)` | `impl Fn(&Error) -> bool` |
| `$$R callback(node: $$N)` | `impl Fn(&N) -> Result<R>` |

Use `impl Fn` for static dispatch (when the callback type is known at compile time) and `Box<dyn Fn>` for dynamic
dispatch (when callbacks are stored as fields).