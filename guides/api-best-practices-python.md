# Python API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete Python patterns,
conventions, and ready-to-copy templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.
Examples mirror the current synchronous SDK patterns.

## 1. Type Mapping Table

| Generic Type      | Python Type                       | Notes                                             |
|-------------------|-----------------------------------|---------------------------------------------------|
| `string`          | `str`                             | -                                                 |
| `intX`            | `int`                             | Python `int` is arbitrary precision               |
| `uintX`           | `int`                             | Use validation for non-negative values            |
| `double`          | `float`                           | -                                                 |
| `decimal`         | `decimal.Decimal`                 | Use for money and exact decimal arithmetic        |
| `bool`            | `bool`                            | -                                                 |
| `bytes`           | `bytes` or `bytearray`            | Prefer `bytes` in public immutable APIs           |
| `list<TYPE>`      | `list[TYPE]`                      | Use built-in generics (Python 3.10+)              |
| `set<TYPE>`       | `set[TYPE]`                       | Use `frozenset` for immutable values              |
| `map<KEY, VALUE>` | `dict[KEY, VALUE]`                | Use `Mapping` in type hints for read-only inputs  |
| `type`            | `type[Any]`                       | Use for runtime type identity                     |
| `uuid`            | `uuid.UUID`                       | -                                                 |
| `date`            | `datetime.date`                   | -                                                 |
| `time`            | `datetime.time`                   | -                                                 |
| `dateTime`        | `datetime.datetime`               | Prefer timezone-aware UTC values when possible    |
| `zonedDateTime`   | `datetime.datetime` with `tzinfo` | Prefer `zoneinfo.ZoneInfo` (concrete `tzinfo`)    |
| `function<...>`   | `collections.abc.Callable[...]`   | Prefer named `Protocol` for complex callbacks     |

Python is dynamically typed. Enforce types at API boundaries with input validation and
conversion helpers, and document expectations with type hints and docstrings.

Type enforcement flow (typical):

| Meta type         | Input type                          | Internal type                 | Return type                   | Validation                                 |
|-------------------|-------------------------------------|-------------------------------|-------------------------------|--------------------------------------------|
| `string`          | `str`                               | `str`                         | `str`                         | length, pattern, non-empty where required  |
| `intX`            | `int` or numeric `str`              | `int`                         | `int`                         | range checks, reject `bool`                |
| `uintX`           | `int` or numeric `str`              | `int`                         | `int`                         | range checks, `>= 0`                       |
| `bytes`           | `bytes`, `bytearray`, hex `str`     | `bytes`                       | `bytes`                       | length checks, hex decode if needed        |
| `list<T>`         | `Iterable[T]`                       | `list[T]`                     | `list[T]`                     | per-item validation                        |
| `map<K, V>`       | `Mapping[K, V]`                     | `dict[K, V]`                  | `dict[K, V]`                  | key/value validation                       |
| `dateTime`        | `datetime.datetime`                 | `datetime.datetime` (UTC)     | `datetime.datetime`           | require `tzinfo`, normalize to UTC         |
| `zonedDateTime`   | `datetime.datetime`                 | `datetime.datetime` (ZoneInfo)| `datetime.datetime`           | require `tzinfo` is `ZoneInfo`             |
| `function<...>`   | `collections.abc.Callable[...]`     | `collections.abc.Callable[...]`| `collections.abc.Callable[...]`| `callable(...)` checks                      |

## 2. Error Model Mapping For @@throws

`@@throws(error-type-a[, ...])` maps to Python exceptions. Keep transport and protocol details out of the public
exception hierarchy and expose stable SDK-level error identifiers. In the current SDK, precheck and receipt failures
use `PrecheckError` and `ReceiptStatusError`; preserve those patterns for parity.

Recommended pattern:

- Use a single root exception type, for example `HieroSdkError`.
- Add stable subclasses per error identifier, for example `NotFoundError`, `InvalidSignatureError`.
- Store the stable identifier on the exception, for example `error_id = "not-found-error"`.
- Chain low-level exceptions with `raise ... from ...` so debugging context is preserved.

Example:

```python
class HieroSdkError(Exception):
    error_id = "sdk-error"


class NotFoundError(HieroSdkError):
    error_id = "not-found-error"


class InvalidSignatureError(HieroSdkError):
    error_id = "invalid-signature-error"
```

## 3. Async Model Mapping For @@async

The current Python SDK is synchronous, so prefer blocking methods and avoid `async def` in public APIs.
If async support is introduced in the future, keep it separate from callback-based APIs and return awaitable values.

## 4. Factory And Static Method Conventions

Map `@@static` factory-style methods to Python `@classmethod` when the result is an instance of the declaring type.
Use `@staticmethod` for utility operations that do not need class state.

Recommended pattern:

- Deserialization and alternate constructors: `@classmethod from_bytes(...)`.
- Parsing helpers that do not depend on class state: `@staticmethod parse_*`.
- Prefer descriptive names such as `from_string`, `from_bytes`, `new_for_transfer`.

Example:

```python
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class AccountId:
    shard: int
    realm: int
    num: int

    @classmethod
    def from_string(cls, value: str) -> "AccountId":
        shard, realm, num = (int(part) for part in value.split("."))
        return cls(shard=shard, realm=realm, num=num)

    @staticmethod
    def is_valid(value: str) -> bool:
        parts = value.split(".")
        return len(parts) == 3 and all(part.isdigit() for part in parts)
```

## 5. Nullability And Collection Semantics

Map `@@nullable` to `T | None` and explicit `None` handling.
Collections should never be nullable in public APIs; return empty collections instead.

Recommended pattern:

- Use `T | None` only where absence has domain meaning.
- Never return `None` for collection return types (`list`, `set`, `dict`).
- Prefer immutable defaults in dataclasses (`tuple`, `frozenset`) and `default_factory` for mutable internals.

Example:

```python
from dataclasses import dataclass, field


@dataclass(frozen=True, slots=True)
class TransactionMemo:
    text: str | None


@dataclass
class ValidationResult:
    warnings: list[str] = field(default_factory=list)
```

## 6. Immutability And Thread-Safety Guidance

When a type and all of its fields are defined as `@@immutable`, prefer Python immutable patterns.
When `@@threadSafe` is required, document and enforce synchronization boundaries explicitly.

Recommended pattern:

- Use `@dataclass(frozen=True, slots=True)` for immutable value objects.
- Use immutable containers in public models (`tuple`, `frozenset`).
- For mutable shared state, protect critical sections with `threading.Lock`.
- Keep thread-safe classes small and focused; avoid exposing mutable internals.

Example:

```python
from dataclasses import dataclass
from threading import Lock


@dataclass(frozen=True, slots=True)
class Receipt:
    transaction_id: str
    status: str


class NonceStore:
    def __init__(self) -> None:
        self._value = 0
        self._lock = Lock()

    def next(self) -> int:
        with self._lock:
            self._value += 1
            return self._value
```

## 7. Runnable Build-Sign-Send Example

The following example is a runnable end-to-end flow showing build, sign, and send using the current synchronous SDK API.

```python
import os

from hiero_sdk_python import (
    AccountId,
    Client,
    Hbar,
    Network,
    PrivateKey,
    ResponseCode,
    TransferTransaction,
)


def main() -> None:
    client = Client(Network("testnet"))
    operator_id = AccountId.from_string(os.environ["OPERATOR_ID"])
    operator_key = PrivateKey.from_string(os.environ["OPERATOR_KEY"])
    client.set_operator(operator_id, operator_key)

    recipient_id = AccountId.from_string(os.environ.get("RECIPIENT_ID", "0.0.1234"))

    receipt = (
        TransferTransaction()
        .add_hbar_transfer(operator_id, Hbar.from_tinybars(-1_000))
        .add_hbar_transfer(recipient_id, Hbar.from_tinybars(1_000))
        .freeze_with(client)
        .sign(operator_key)
        .execute(client)
    )

    if receipt.status != ResponseCode.SUCCESS:
        raise RuntimeError(f"Transfer failed: {receipt.status}")

    print("Transfer OK:", receipt.transaction_id)


if __name__ == "__main__":
    main()
```
