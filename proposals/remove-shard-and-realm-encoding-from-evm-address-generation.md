# Remove Shard and Realm Encoding from EVM Address Generation

**Date Submitted:** 2025-06-06

---

## Summary

The SDKs currently encode shard and realm values when generating an EVM address from an entity ID. The entity ID can be an account, token, topic, contract, delegate contract or file. This applies to all entity types. This proposal recommends removing shard and realm encoding entirely, and eliminating any assumptions that default these values to zero.

**Justification for decision to move forward without EVM address encoding:**
The decision to proceed by abandoning bit-packing the shard and realm information was driven by the conclusion that cross-chain transactions would still ultimately require explicit system contract calls regardless of address encoding. So the sacrifice of reduction in address space simply was without value.

**Why explicitly declaring the addresses and passing them in protobuf messages?**
- Long zeros can't necessarily be differentiated from ETH addresses due to the potential for zeroes at the leading bits of ETH addresses.
- Cross chain calls will demand explicit system contract calls regardless of the addresses being packed with shard and realm values, therefore the ultimate benefit of incorporating those values is not ultimately realizable.
- Triple ordering resulting in local networks always identifying as 0.0 values additionally contribute to the irrelevance of incorporating any shard and realm values packed within addresses.
- CAIP2/ChainID's per realm adoption across multiple networks simplified.
- We don't have to make Consensus nodes do extra work parsing addresses when they can simply be inferred.
- Minimal difference between present method of provisioning accounts on our path to cross ledger communications(user impacts basically non-existent).

## New APIs

### `<EntityId>.fromEvmAddress(int64 shard, int64 realm, string evmAddress)`

This method already exists in some SDKs as a replacement for `.fromSolidityAddress()`. This proposal recommends standardizing on this API across all SDKs. 

**Behavior:**

- Accepts `shard`, `realm`, and `evmAddress` as parameters.
- Assumes the `evmAddress` encodes only the entity number (i.e., the last 8 bytes of a 20-byte address).
- Constructs and returns the corresponding `EntityId` using the provided `shard`, `realm`, and extracted entity number.

**Clarification:**  
If any SDK currently assumes default values of `0` for shard and realm (e.g., ignoring the passed-in values), those assumptions must be removed.

The intent was for AccountId.fromEvmAddress(1, 1, "00000000000000000000000000000000000004d2") to return 1.1.1234. However, the current implementation returns 1.1.00000000000000000000000000000000000004d2. We would prefer not to introduce a breaking change by altering the return type.


**Example:**

```js
const id = AccountId.fromEvmAddress(
  1,
  1,
  "00000000000000000000000000000000000004d2"
);
// Should return 1.1.00000000000000000000000000000000000004d2

const id = AccountId.fromEvmAddress(
  1,
  1,
  "742d35Cc6634C0532925a3b844Bc454e4438f44e"
);
// Should return 1.1.742d35Cc6634C0532925a3b844Bc454e4438f44e

```

**Note:**  
If `<EntityId>.fromEvmAddress(shard, realm, evmAddress)` is not yet implemented in a specific SDK language, it should be implemented as part of this proposal to ensure consistent support across all Hiero SDKs.

### `<EntityId>.toEvmAddress()`

This method provides the EVM-compatible address representation of an `EntityId`. It complements `.fromEvmAddress()` by standardizing how entity identifiers are represented in EVM-compatible environments.

**Behavior:**

- If the `EntityId` has an alias-style EVM address (e.g., a 20-byte Ethereum address like `0x742d…`), that address is returned directly.
- If the entity uses a numerical ID (i.e., has a non-zero `entityNum` and no alias), the returned address is a 20-byte hex string where:
  - The **first 12 bytes are zero**
  - The **last 8 bytes** encode the `entityNum` in **big-endian** format
- The `shard` and `realm` values are **not** encoded in the EVM address.

**Clarification:**

- This method ensures that `toEvmAddress()` and `fromEvmAddress(shard, realm, evmAddress)` are consistent inverses of each other when used together.
- The output is identical for all entities with the same `entityNum`, regardless of `shard` or `realm`.

**Example:**

```js
// Pass entity ID 0.0.1234
const evmAddress1 = new AccountId(0, 0, 1234).toEvmAddress();
console.log("To EVM address: " + evmAddress1);
// Result: 00000000000000000000000000000000000004d2

// Pass entity ID 1.1.1234
const evmAddress2 = new AccountId(1, 1, 1234).toEvmAddress();
console.log("To EVM address: " + evmAddress2);
// Result: 00000000000000000000000000000000000004d2

// Pass entity ID 1.1.742d35Cc6634C0532925a3b844Bc454e4438f44e
const evmAddress3 = new AccountId(
  1,
  1,
  "742d35Cc6634C0532925a3b844Bc454e4438f44e"
).toEvmAddress();
console.log("To EVM address: " + evmAddress3);
// Result: 742d35Cc6634C0532925a3b844Bc454e4438f44e

```


---

## Updated APIs

### `<EntityId>.fromSolidityAddress(string address)`

- **Status:** Deprecated    
- **Replacement:** `<EntityId>.fromEvmAddress(shard, realm, evmAddress)` 

### `<EntityId>.toSolidityAddress()`
- **Status:** Deprecated    
- **Replacement:** `<EntityId>.toEvmAddress()` 


## Test Plan

1. **Given** an entity ID with non-zero shard and realm,  
   **When** `toEvmAddress()` is called,  
   **Then** the resulting EVM address encodes only the entity number and does **not** encode shard or realm values.

2. **Given** an entity ID with shard and realm both set to `0`,  
   **When** `toEvmAddress()` is called before and after the update,  
   **Then** the resulting EVM address remains unchanged, ensuring backward compatibility.

3. **Given** a call to `fromEvmAddress(1, 1, longZeroAddress)`,  
   **When** the EVM address contains only the entity number (e.g., `"00000000000000000000000000000000000004d2"`),  
   **Then** the resulting entity ID is correctly returned as `1.1.00000000000000000000000000000000000004d2`.

4. **Given** a call to `fromEvmAddress(0, 0, evmAddress)`,  
   **When** the EVM address contains only the entity number,  
   **Then** the resulting entity ID is correctly returned as `0.0.<number>`.

5. **Given** a call to `fromEvmAddress(shard, realm, evmAddress)` in an SDK where shard and realm default to zero,  
   **When** non-zero shard and realm values are passed,  
   **Then** the method must respect those values and return the correct entity ID, rather than defaulting to `0.0`.

6. **Given** an entity ID with non-zero shard and realm with an evm address (0.0.742d35Cc6634C0532925a3b844Bc454e4438f44e),  
   **When** `toEvmAddress()` is called,  
   **Then** the resulting EVM address is the evm address(742d35Cc6634C0532925a3b844Bc454e4438f44e).
---

## SDK Example

```js
// 0.0.1234 → EVM address: 00000000000000000000000000000000000004d2
const accountId = new AccountId(0, 0, 1234);
console.log(accountId.toEvmAddress()); 
// Expected output: 00000000000000000000000000000000000004d2

// 1.1.1234 → EVM address should still be: 00000000000000000000000000000000000004d2
const accountIdNonZero = new AccountId(1, 1, 1234);
console.log(accountIdNonZero.toEvmAddress()); 
// Expected output: 00000000000000000000000000000000000004d2

// Create AccountId from evm address and explicit shard/realm
const accountIdFromEvm = AccountId.fromEvmAddress(
  1,
  1,
  "00000000000000000000000000000000000004d2"
);
console.log(accountIdFromEvm.toString()); 
// Expected output: 1.1.1234

```

---

## Compatibility
Proposal is expected to be backwards compatible. Any deprecated APIs will not be removed until a future major version update.

---

## Conclusion

This proposal removes the encoding for shard and realm values in EVM addresses as well as any assumptions for zero shard and realm values.

It was proposed to keep `toSolidityAddress` and update its logic to not encode the shard and realm, but was rejected for for maintaining a consistent experience in the SDKs with `fromEvmAddress`.
