# Native Ethereum Transaction Data Construction and Signing

**Date Submitted:** 2026-05-28

## Summary

This proposal introduces **native construction and signing of Ethereum transaction data** to all Hiero SDKs. It
defines a first-class, signable data type for each of the four Ethereum transaction envelope formats so that developers
can build and sign an Ethereum transaction entirely within the SDK and submit the result through the existing
`EthereumTransaction` (HAPI `EthereumTransaction`).

The four envelope formats are:

| Envelope                         | Type prefix | Distinguishing fields                                    |
|----------------------------------|-------------|----------------------------------------------------------|
| Legacy (pre-[EIP-2718])          | none        | `gasPrice`, `v`                                          |
| [EIP-2930] (typed, access lists) | `0x01`      | `chainId`, `gasPrice`, `accessList`                      |
| [EIP-1559] (dynamic fee)         | `0x02`      | `chainId`, `maxPriorityGas`, `maxGas`, `accessList`      |
| [EIP-7702] (EOA code delegation) | `0x04`      | the EIP-1559 fields plus an `authorizationList`          |

Today, to submit an `EthereumTransaction` a developer must construct the raw RLP envelope, prepend the correct type
prefix, compute the secp256k1 recovery id, and sign the payload themselves — typically by pulling in an external
library (`ethers.js`, `web3j`, etc.). This is error-prone (RLP big-integer trimming, type-prefix placement,
recovery-id computation) and inconsistent across SDKs. This proposal makes the full Ethereum-envelope build-and-sign
flow self-contained in every SDK.

Several SDKs already expose *some* Ethereum transaction data types, but coverage and capabilities are uneven (see
[Current SDK support](#current-sdk-support)): the JavaScript SDK has all four envelopes, the Java SDK has Legacy and
EIP-1559 (it lacks EIP-2930, and its EIP-7702 support is in progress), and **no SDK currently signs natively**.
This proposal closes those gaps by (a) requiring all four envelope formats in every SDK, (b) adding a uniform `sign`
capability to each, and (c) introducing a structured `AccessListItem` type shared by the typed envelopes. The
[EIP-7702] data type and the `Authorization` tuple were first specified for SDKs in
[hip-1340.md](./hips/hip-1340.md) (EOA Code Delegation); this proposal builds directly on them.

All changes are net-new, client-side, additive functionality. They do not change any consensus behavior, wire format,
or response codes — the signed bytes produced are submitted through the existing `EthereumTransaction.setEthereumData`
entry point and validated by the network exactly as before.

### Relationship to the Hiero JSON-RPC Relay

The [Hiero JSON-RPC Relay](https://github.com/hiero-ledger/hiero-json-rpc-relay) is the primary path for Ethereum
tooling (MetaMask, ethers.js, Hardhat), and this proposal does not replace it. The relay only submits an
*already-signed* raw transaction — the client still builds and signs it. This proposal moves that build-and-sign step
into the SDKs, so the signed bytes can go through either HAPI's `EthereumTransaction` or the relay's
`eth_sendRawTransaction`.

Here "native" means *native to the SDK* — the developer no longer adds an Ethereum library to their own project (any
such dependency, e.g. RLP, stays internal to the SDK) — not native EVM execution; the payload is still wrapped in a
HAPI `EthereumTransaction`.

Related references:

- [EIP-2718] — Typed Transaction Envelope
- [EIP-2930] — Optional Access Lists
- [EIP-1559] — Fee Market Change
- [EIP-7702] — Set EOA Account Code
- [HIP-1340](https://hips.hedera.com/hip/hip-1340) — EOA Code Delegation
- [Hiero JSON-RPC Relay](https://github.com/hiero-ledger/hiero-json-rpc-relay) — Ethereum JSON-RPC API for Hiero

[EIP-2718]: https://eips.ethereum.org/EIPS/eip-2718
[EIP-2930]: https://eips.ethereum.org/EIPS/eip-2930
[EIP-1559]: https://eips.ethereum.org/EIPS/eip-1559
[EIP-7702]: https://eips.ethereum.org/EIPS/eip-7702

---

## New APIs

### AccessListItem

A single entry in an [EIP-2930] access list. Each entry names an Ethereum address and the storage keys under that
address that the transaction intends to access. Used by the EIP-2930, EIP-1559, and EIP-7702 envelopes.

```
@@finalType
AccessListItem {
    @@nullable address: bytes
    storageKeys: list<bytes>

    AccessListItem addStorageKey(storageKey: bytes)
}
```

> `address` is a 20-byte Ethereum address. Each storage key is a 32-byte value. SDKs should accept and return raw
> `bytes` here and may additionally accept the SDK's existing `EvmAddress` type for `address` per language best
> practice.
>
> **Current representation divergence.** The Go SDK models this as a struct (`AccessListItem`); the JS SDK
> represents it as a positional tuple typedef `[address, storageKeys[]]`; the Java SDK currently stores the entire
> access list as a single opaque `byte[]` RLP blob. This proposal adopts the named, structured type as canonical so
> that all SDKs expose the access list in an inspectable, type-safe form.

### Authorization

A single authorization tuple in an [EIP-7702] authorization list, authorizing the delegation of an EOA's code to a
contract address. This type was introduced for SDKs in [HIP-1340](./hips/hip-1340.md) and is reused here unchanged.
Note that its `chainId` is `bytes`, which is intentionally inconsistent with the typed-numeric model adopted for the
envelope fields below (where `chainId` is `uint64`): to avoid re-opening an already-specified HIP-1340 type,
`Authorization` is left exactly as HIP-1340 defined it, and aligning its fields to the typed model is deferred to a
future revision.

> **Current representation divergence.** [HIP-1340](./hips/hip-1340.md) specifies this as a named `Authorization`
> type (whose address field is named `address`). The JS SDK currently represents it as a positional tuple typedef
> `AuthorizationItem` (`[chainId, contractAddress, nonce, yParity, r, s]`, with the address field named
> `contractAddress`); the Go SDK names the type `AuthorizationTuple`; the Java SDK's EIP-7702 support is in
> progress. This proposal adopts the named `Authorization` type as canonical.

```
@@finalType
Authorization {
    @@immutable chainId: bytes
    @@immutable address: bytes
    @@immutable nonce: uint64
    @@immutable yParity: uint32
    @@immutable r: bytes
    @@immutable s: bytes
}
```

### EthereumTransactionData

The abstract base type for all Ethereum transaction envelopes. It declares the **behavior** shared by every variant:
RLP serialization (with the correct type prefix), ECDSA signing, and deserialization. It deliberately declares no
fields — see the note on common-field placement below.

`sign` ECDSA-signs the unsigned RLP payload (the type-prefixed message for typed envelopes), writes the resulting
`r`, `s`, and recovery component (`recoveryId` for typed envelopes, `v` for legacy) back onto the instance, and
returns the signed RLP bytes. The returned bytes are what is passed to `EthereumTransaction.setEthereumData`. `sign`
throws if the supplied key is not an ECDSA (secp256k1) key. Detection is via the recovery-id computation rather than a
simple key-type check (an Ed25519 signature is also 64 bytes, so a length check alone is insufficient): if a valid
recovery id cannot be derived, `sign` fails.

```
abstraction EthereumTransactionData {
    @@throws(invalid-ecdsa-key-error)
    bytes sign(key: PrivateKey)

    @@throws(rlp-encoding-error)
    bytes toBytes()

    @@static
    @@throws(parse-error)
    EthereumTransactionData fromBytes(payload: bytes)

    string toString()
}
```

> **Common-field placement is language-specific.** Several fields are common to all four envelopes — `nonce`,
> `gasLimit`, `to`, `value`, `callData`, `r`, and `s`. For clarity, each envelope below lists its full field set
> rather than relying on inheritance. Whether an SDK physically hoists these common fields onto the base type or
> repeats them on each concrete variant is a **language-specific realization detail**, not part of the contract. The
> JS and Java SDKs, where `EthereumTransactionData` is a base class, place `callData` on the base and inherit it; the
> Go SDK, where the shared type is an interface (which cannot hold fields), keeps every field on the concrete structs.
> The only requirement is that each concrete envelope expose the fields listed for it.
>
> **Numeric field representation.** Across all four envelopes below, `chainId`, `nonce`, and `gasLimit` are modeled as
> `uint64`; the monetary/gas-price fields (`gasPrice`, `maxPriorityGas`, `maxGas`, `value`) are modeled as `uint256`
> because Ethereum permits 256-bit values. `to` is a 20-byte address (`bytes`, `@@nullable` to allow contract-creation
> calls). These canonical types describe the *typed* view of each field; how that view is exposed alongside each SDK's
> existing representation is covered in [Field accessor conventions](#field-accessor-conventions) below.

#### Field accessor conventions

The four envelope types are **already shipped** in several SDKs, each with its own set of public fields and field types
(see [Current SDK support](#current-sdk-support)). Changing the type of an existing public field — for example flipping
the Go `value` accessor from `[]byte` to `*big.Int`, or the JS fields from `Uint8Array` to a big-integer type — would be
a breaking change for those SDKs' users. This proposal therefore does **not** require any SDK to change an existing
field's type. Instead, the typed/bytes flexibility is delivered **additively**, by layering convenience accessors around
whatever representation an SDK already stores:

- **Typed and raw-bytes accessors are paired.** Every field should be reachable both as its natural typed form (the
  canonical type in the definitions below — `uint64`, a big integer, or a 20-byte address) and as raw `bytes`. An SDK
  that stores a field as bytes today adds the typed accessor; one that stores it typed adds the bytes accessor. Neither
  removes what already exists, so no current signature breaks.
- **Naming.** Languages with method overloading (Java, C++, Swift, and via union parameter types TypeScript) reuse the
  base name for both forms — `getValue()` returns the typed form and `getValueBytes()` the raw form, `setValue(...)`
  accepts either. **Go has no overloading**, so it uses an explicit suffix: `GetValue`/`GetValueBytes` and
  `SetValue`/`SetValueBytes`. This is the reason for Go's `…Bytes` accessors and is a deliberate convention, not an
  ad-hoc divergence.
- **String/hex variants are bounded.** A hex-`string` accessor (e.g. `setToString`) is worthwhile only where it is
  genuinely ergonomic — addresses (`to`, `contractAddress`) and raw byte blobs (`callData`) — and should require an
  explicit, documented format (e.g. `0x`-prefixed). Large integers (`value`, `gasPrice`, `maxGas`) should go through the
  native big-integer type and raw bytes rather than strings, to avoid divergent string-parsing rules across SDKs.

This keeps each SDK's existing public field set stable while still giving developers the representation they find most
convenient.

#### EthereumTransactionDataLegacy

A pre-[EIP-2718] legacy Ethereum transaction. Carries no type prefix and uses a single `gasPrice` and the legacy `v`
signature component.

```
@@finalType
EthereumTransactionDataLegacy extends EthereumTransactionData {
    @@default(0) nonce: uint64
    @@default(0) gasPrice: uint256
    @@default(0) gasLimit: uint64
    @@nullable to: bytes
    @@default(0) value: uint256
    callData: bytes
    @@default(0) v: uint64
    r: bytes
    s: bytes
}
```

#### EthereumTransactionDataEip2930

An [EIP-2930] typed transaction (prefix `0x01`). Adds a `chainId` and an access list to the legacy fee model.

```
@@finalType
EthereumTransactionDataEip2930 extends EthereumTransactionData {
    @@default(0) chainId: uint64
    @@default(0) nonce: uint64
    @@default(0) gasPrice: uint256
    @@default(0) gasLimit: uint64
    @@nullable to: bytes
    @@default(0) value: uint256
    callData: bytes
    accessList: list<AccessListItem>
    @@default(0) recoveryId: uint32
    r: bytes
    s: bytes

    EthereumTransactionDataEip2930 addAccessListItem(item: AccessListItem)
}
```

#### EthereumTransactionDataEip1559

An [EIP-1559] dynamic-fee transaction (prefix `0x02`). Replaces the single `gasPrice` with the `maxPriorityGas` /
`maxGas` fee market fields.

```
@@finalType
EthereumTransactionDataEip1559 extends EthereumTransactionData {
    @@default(0) chainId: uint64
    @@default(0) nonce: uint64
    @@default(0) maxPriorityGas: uint256
    @@default(0) maxGas: uint256
    @@default(0) gasLimit: uint64
    @@nullable to: bytes
    @@default(0) value: uint256
    callData: bytes
    accessList: list<AccessListItem>
    @@default(0) recoveryId: uint32
    r: bytes
    s: bytes

    EthereumTransactionDataEip1559 addAccessListItem(item: AccessListItem)
}
```

#### EthereumTransactionDataEip7702

An [EIP-7702] transaction (prefix `0x04`). Extends the EIP-1559 fee model with an `authorizationList` carrying the
EOA-to-contract code-delegation authorizations described in [HIP-1340](./hips/hip-1340.md).

```
@@finalType
EthereumTransactionDataEip7702 extends EthereumTransactionData {
    @@default(0) chainId: uint64
    @@default(0) nonce: uint64
    @@default(0) maxPriorityGas: uint256
    @@default(0) maxGas: uint256
    @@default(0) gasLimit: uint64
    @@nullable to: bytes
    @@default(0) value: uint256
    callData: bytes
    accessList: list<AccessListItem>
    authorizationList: list<Authorization>
    @@default(0) recoveryId: uint32
    r: bytes
    s: bytes

    EthereumTransactionDataEip7702 addAccessListItem(item: AccessListItem)
    EthereumTransactionDataEip7702 addAuthorization(authorization: Authorization)
}
```

---

## Updated APIs

### EthereumTransaction

`EthereumTransaction` already accepts the raw signed RLP bytes via `setEthereumData(bytes)`. The signed output of
`EthereumTransactionData.sign` (or `toBytes` for already-signed data) is passed to this existing method, so no change
to the wire path is required.

As an ergonomic addition, SDKs **should** also provide an overload that accepts an `EthereumTransactionData` instance
directly, so a developer does not have to manually call `toBytes`/`sign` and thread the resulting byte array:

```
EthereumTransaction {
    EthereumTransaction setEthereumData(data: EthereumTransactionData)
}
```

When passed an `EthereumTransactionData` that has not yet been signed (no `r`/`s` set), the SDK must either serialize
the already-signed envelope or surface a clear error; it must not silently submit an unsigned envelope. The exact
behavior of this overload is left to each SDK, but it must not change the semantics of the existing
`setEthereumData(bytes)` method.

---

## Internal Changes

This feature is entirely **client-side**: it constructs and signs Ethereum transaction data locally before submission.
It introduces no new consensus interaction.

### Current SDK support

The starting point differs by SDK. This proposal brings them to parity on the contract above.

| Capability                          | JavaScript                              | Java                                   | Go                                     |
|-------------------------------------|-----------------------------------------|----------------------------------------|----------------------------------------|
| Abstract base type                  | `EthereumTransactionData`               | `EthereumTransactionData` (abstract)   | `EthereumTransactionBody` (interface)  |
| Legacy envelope                     | yes                                     | yes                                    | yes                                    |
| EIP-2930 envelope                   | yes                                     | **missing**                            | yes                                    |
| EIP-1559 envelope                   | yes                                     | yes                                    | yes                                    |
| EIP-7702 envelope                   | yes                                     | in progress                            | yes                                    |
| Native `sign`                       | **missing**                             | **missing**                            | **missing**                            |
| Field representation                | bytes (`Uint8Array`)                    | bytes (`byte[]`)                       | typed (`uint64`/`big.Int`) + bytes     |
| Access list representation          | tuple list `[address, keys[]]`          | single opaque `byte[]` RLP blob        | `[]AccessListItem` struct              |

The headline gap is universal: **no SDK signs natively today**. On envelope coverage, **Java still lacks EIP-2930**;
its EIP-7702 support is *in progress* rather than absent (see note below). Both are addressed by adopting the contract
in this document across all SDKs.

> **EIP-7702 / HIP-1340 status.** EIP-7702 support corresponds to [HIP-1340](./hips/hip-1340.md) (EOA Code Delegation),
> a recent addition still being rolled out across the SDKs. It is already merged in some SDKs and in progress (open PR)
> in others; the table marks the latter as *in progress* rather than *missing*.

### Shared signing and encoding logic

Each SDK needs the following primitives, most of which already exist for the current `EthereumTransaction` support:

- **RLP encoding/decoding** of the envelope field list, with Ethereum's minimal big-endian integer encoding
  (leading zero bytes trimmed; zero encoded as empty bytes).
- **Type-prefix handling** — typed envelopes (`0x01`/`0x02`/`0x04`) prepend the prefix to both the message that is
  signed and the final serialized bytes; legacy envelopes do not.
- **secp256k1 ECDSA signing with recovery-id computation** — `sign` produces a 64-byte `r`/`s` pair and computes the
  recovery id over the (prefixed) message. Signing with a non-ECDSA (e.g. Ed25519) key must fail with a clear error.

SDKs should factor this shared signing and encoding logic into common helpers reused by the three typed variants,
rather than duplicating it per variant.

### Cross-SDK naming map

SDKs are expected to name these concepts according to their own language conventions — divergence here is normal and
not a problem to be fixed. This table maps each meta-language concept to the name each SDK currently uses, so readers
can cross-reference one SDK against another:

| Concept (meta-language)         | JavaScript                       | Java                             | Go                  |
|---------------------------------|----------------------------------|----------------------------------|--------------------------------|
| EIP-1559 variant                | `EthereumTransactionDataEip1559` | `EthereumTransactionDataEip1559` | `EthereumEIP1559Transaction`   |
| EIP-2930 variant                | `EthereumTransactionDataEip2930` | (missing)                        | `EthereumEIP2930Transaction`   |
| EIP-7702 variant                | `EthereumTransactionDataEip7702` | (in progress)                    | `EthereumEIP7702Transaction`   |
| Legacy variant                  | `EthereumTransactionDataLegacy`  | `EthereumTransactionDataLegacy`  | `EthereumLegacyTransaction`    |
| Recovery component (typed)      | `recId`                          | `recoveryId`                     | `recoveryId`                   |
| EIP-7702 authorization tuple    | `Authorization`                  | (in progress)                    | `AuthorizationTuple`           |

> **Note — `EthereumTransactionData` role (a semantic collision, not a naming style).** Unlike the rows above, this one
> is not merely per-language style: the *same* identifier means different things. In JS and Java,
> `EthereumTransactionData` is the abstract base the variants extend (the model this proposal uses); in Go it is a
> concrete union wrapper holding a pointer to one of the four variants (`NewEthereumTransactionData`, `GetTransaction`),
> with the base behavior on the `EthereumTransactionBody` interface. Because one name carries two different meanings,
> the recommended resolution is for Go to rename its wrapper so `EthereumTransactionData` consistently denotes the
> abstract base across SDKs. This is a naming change only — the field and behavior contract is unaffected, so
> implementers need not block on it.

### Response Codes

No new consensus node response codes are introduced. Construction and signing happen entirely client-side; the signed
Ethereum data is validated by the network through the existing `EthereumTransaction` path, and the existing response
codes for that transaction (e.g. `INVALID_ETHEREUM_TRANSACTION`, `WRONG_CHAIN_ID`, `INVALID_ACCOUNT_ID`) continue to
apply unchanged.

#### Transaction Retry

No new retry behavior. Building and signing are local, deterministic operations with no network round-trip. Submission
retry is governed entirely by the existing `EthereumTransaction` execution path and is unaffected by this proposal.

## Test Plan

1. Given a valid ECDSA key and a fully populated `EthereumTransactionDataLegacy`, when `sign` is called, then `v`,
   `r`, and `s` are populated and `toBytes` returns RLP with no type prefix.

2. Given a valid ECDSA key and a fully populated `EthereumTransactionDataEip2930`, when `sign` is called, then
   `recoveryId`, `r`, and `s` are populated and `toBytes` returns RLP prefixed with `0x01`.

3. Given a valid ECDSA key and a fully populated `EthereumTransactionDataEip1559`, when `sign` is called, then
   `recoveryId`, `r`, and `s` are populated and `toBytes` returns RLP prefixed with `0x02`.

4. Given a valid ECDSA key and a fully populated `EthereumTransactionDataEip7702` with one or more `Authorization`
   entries, when `sign` is called, then `recoveryId`, `r`, and `s` are populated and `toBytes` returns RLP prefixed
   with `0x04`.

5. Given any of the four envelopes serialized via `toBytes`, when the bytes are passed to `fromBytes`, then the
   reconstructed instance is field-for-field equal to the original (round-trip).

6. Given an `EthereumTransactionDataEip1559` with an `accessList` of multiple `AccessListItem` entries (each with one
   or more storage keys), when the data is signed and round-tripped, then the access list is preserved exactly.

7. Given an `EthereumTransactionDataEip1559` with an empty `accessList`, when the data is signed and round-tripped,
   then the access list round-trips as empty (not null).

8. Given an `EthereumTransactionDataEip7702` with an empty `authorizationList`, when the data is signed, then signing
   succeeds and the authorization list round-trips as empty.

9. Given an envelope whose `value` and `maxGas` are large 256-bit integers (high bytes set), when the data is signed
   and round-tripped, then the values are preserved and encoded with minimal big-endian form (no leading zero bytes).

10. Given an envelope whose `value` and `gasLimit` are zero, when the data is signed and round-tripped, then the
    zero values are encoded as empty bytes per Ethereum RLP rules and round-trip back to zero.

11. Given a non-ECDSA key (e.g. an Ed25519 key), when `sign` is called on any envelope, then it fails with an
    `invalid-ecdsa-key-error`.

12. Given a signed `EthereumTransactionDataEip1559` for a contract call, when its signed bytes are passed to
    `EthereumTransaction.setEthereumData` and the transaction is executed against the network, then the transaction
    succeeds and the receipt reflects the executed call.

13. Given a signed `EthereumTransactionDataEip7702` that delegates an EOA to a contract, when submitted via
    `EthereumTransaction` and executed, then the delegation takes effect (consistent with [HIP-1340](./hips/hip-1340.md)).

14. Given an unsigned `EthereumTransactionData` instance, when it is passed to the `setEthereumData(data)` overload,
    then the SDK either signs/serializes it as specified or fails with a clear error — it never submits an unsigned
    envelope.

### TCK

The tests defined above should also be defined in the [TCK repository](https://github.com/hiero-ledger/hiero-sdk-tck).
Because construction and signing are client-side, most of these are SDK-local unit tests; the end-to-end cases
(12, 13) belong in the TCK as `EthereumTransaction` flows. Issues should be created in the TCK repository based on this
content to document the end-to-end tests and linked as reference in this design doc.

## SDK Example

### Example: Build, sign, and submit an EIP-1559 Ethereum transaction

This example builds an [EIP-1559] contract call natively in the SDK, signs it with an ECDSA key, and submits it through
`EthereumTransaction`.

1. Generate or load an ECDSA (secp256k1) `PrivateKey`.
2. Build an `EthereumTransactionDataEip1559`, setting `chainId`, `nonce`, `maxPriorityGas`, `maxGas`, `gasLimit`, the
   `to` contract address, `value`, and `callData`.
3. Optionally add `AccessListItem` entries via `addAccessListItem`.
4. Call `sign(privateKey)` to populate `recoveryId`, `r`, and `s` and obtain the signed RLP bytes (prefixed with `0x02`).
5. Construct an `EthereumTransaction` and pass the signed bytes to `setEthereumData`.
6. Execute the transaction and fetch the receipt.
7. Verify the receipt status is `SUCCESS`.

```typescript
import {
  Client,
  PrivateKey,
  EthereumTransaction,
  EthereumTransactionDataEip1559,
  AccessListItem,
} from "@hiero/sdk";

const client = Client.forTestnet();

const ecdsaKey = PrivateKey.generateECDSA();

const data = new EthereumTransactionDataEip1559()
  .setChainId(296) // Hedera testnet chain id
  .setNonce(0)
  .setMaxPriorityGas(2_000_000_000n)
  .setMaxGas(20_000_000_000n)
  .setGasLimit(100_000)
  .setTo(contractEvmAddressBytes)
  .setValue(0n)
  .setCallData(encodedFunctionCall)
  .addAccessListItem(new AccessListItem().setAddress(contractEvmAddressBytes));

// Signs in place (sets recoveryId/r/s) and returns the 0x02-prefixed RLP bytes.
const signedBytes = data.sign(ecdsaKey);

const response = await new EthereumTransaction()
  .setEthereumData(signedBytes)
  .execute(client);

const receipt = await response.getReceipt(client);
console.log(`Status: ${receipt.status.toString()}`);
```

The same flow applies to the other envelopes by substituting the corresponding data type
(`EthereumTransactionDataLegacy`, `EthereumTransactionDataEip2930`, or `EthereumTransactionDataEip7702`). For
[EIP-7702], add one or more `Authorization` entries with `addAuthorization` before signing.

> Method and accessor naming should follow each language's best-practice guideline. For example, Go exposes
> `SetMaxGas`/`GetMaxGas` (with `*big.Int`) alongside `SetMaxGasBytes`/`GetMaxGasBytes`; Rust uses snake_case
> (`set_max_gas`); TypeScript/JavaScript use property access and `setMaxGas`.
