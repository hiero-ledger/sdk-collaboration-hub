# Python API Implementation Guideline

This document translates the [language-agnostic API meta-definition](api-guideline.md) into concrete Python patterns,
conventions, and ready-to-copy templates.
It aims to keep SDK APIs consistent, ergonomic, and maintainable across modules.

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
| `list<TYPE>`      | `list[TYPE]`                      | Use `typing.List` for older Python versions       |
| `set<TYPE>`       | `set[TYPE]`                       | Use `frozenset` for immutable values              |
| `map<KEY, VALUE>` | `dict[KEY, VALUE]`                | Use `Mapping` in type hints for read-only inputs  |
| `type`            | `type[Any]`                       | Use for runtime type identity                     |
| `uuid`            | `uuid.UUID`                       | -                                                 |
| `date`            | `datetime.date`                   | -                                                 |
| `time`            | `datetime.time`                   | -                                                 |
| `dateTime`        | `datetime.datetime`               | Prefer timezone-aware UTC values when possible    |
| `zonedDateTime`   | `datetime.datetime` with `tzinfo` | Use `zoneinfo.ZoneInfo` (Python 3.9+)             |
| `function<...>`   | `typing.Callable[...]`            | Prefer named `Protocol` for complex callbacks     |

## 2. Error Model Mapping For @@throws

`@@throws(error-type-a[, ...])` maps to Python exceptions. Keep transport and protocol details out of the public
exception hierarchy and expose stable SDK-level error identifiers.

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

`@@async` methods should be represented as `async def` and return awaited values, not callback-based APIs.

Recommended pattern:

- Public async APIs are `async def` methods.
- If synchronous convenience methods are provided, implement them as thin wrappers around async methods.
- Do not combine callback parameters with coroutine return values in one method.

Example:

```python
class Client:
    async def submit(self, tx_bytes: bytes) -> bytes:
        ...

    def submit_sync(self, tx_bytes: bytes) -> bytes:
        import asyncio

        return asyncio.run(self.submit(tx_bytes))
```

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


@dataclass(frozen=True)
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

Map `@@nullable` to `Optional[T]` and explicit `None` handling.
Collections should never be nullable in public APIs; return empty collections instead.

Recommended pattern:

- Use `Optional[T]` only where absence has domain meaning.
- Never return `None` for collection return types (`list`, `set`, `dict`).
- Prefer immutable defaults in dataclasses (`tuple`, `frozenset`) and `default_factory` for mutable internals.

Example:

```python
from dataclasses import dataclass, field
from typing import Optional


@dataclass(frozen=True)
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

- Use `@dataclass(frozen=True)` for immutable value objects.
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

The following example is a runnable end-to-end flow showing build, sign, and send with async submission.

```python
import asyncio
import hashlib
from dataclasses import dataclass


@dataclass(frozen=True)
class SignedTransaction:
    body: bytes
    signature: bytes


class TransferTransactionBuilder:
    def __init__(self) -> None:
        self._from_account = ""
        self._to_account = ""
        self._amount_tinybar = 0

    def from_account(self, value: str) -> "TransferTransactionBuilder":
        self._from_account = value
        return self

    def to_account(self, value: str) -> "TransferTransactionBuilder":
        self._to_account = value
        return self

    def amount_tinybar(self, value: int) -> "TransferTransactionBuilder":
        self._amount_tinybar = value
        return self

    def build(self) -> bytes:
        payload = f"{self._from_account}|{self._to_account}|{self._amount_tinybar}"
        return payload.encode("utf-8")


class PrivateKey:
    def __init__(self, seed: bytes) -> None:
        self._seed = seed

    def sign(self, message: bytes) -> bytes:
        return hashlib.sha256(self._seed + message).digest()


class Client:
    async def send(self, tx: SignedTransaction) -> str:
        await asyncio.sleep(0.01)
        digest = hashlib.sha256(tx.body + tx.signature).hexdigest()
        return f"ok:{digest[:16]}"


async def main() -> None:
    tx_body = (
        TransferTransactionBuilder()
        .from_account("0.0.1001")
        .to_account("0.0.1002")
        .amount_tinybar(100)
        .build()
    )

    key = PrivateKey(seed=b"demo-key")
    signed_tx = SignedTransaction(body=tx_body, signature=key.sign(tx_body))

    client = Client()
    receipt = await client.send(signed_tx)
    print("receipt", receipt)


if __name__ == "__main__":
    asyncio.run(main())
```
