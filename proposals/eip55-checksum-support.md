# Design Proposal: EIP-55 Checksum Support and Address String Representation

## This proposal does not attempt to define a canonical display representation for EVM addresses across the Hedera ecosystem; it only proposes additive checksum utilities. Specific Mention: (AI)In this proposal making process Claude is used for research and some idea implementation part.

**Status:** Proposed  
**Author:** @iron-prog  
**Date:** May 2026  
**Affected SDKs:** All (Java, JavaScript/TypeScript, Go, Rust, Python, etc.)  
**Discussion:** [hiero-sdk-python#2233](https://github.com/hiero-ledger/hiero-sdk-python/pull/2233)

---

## Problem Statement

Currently, Hiero SDKs do not expose standardized EIP-55 checksum formatting or validation utilities for EVM addresses. While this is purely a presentation-layer concern, the lack of a coordinated API shape creates potential cross-SDK inconsistency and makes Hiero EVM addresses feel inconsistent with common Ethereum tooling conventions.

1. **Developer ergonomics**: Developers working with EVM-compatible addresses lack client-side typo detection
2. **Cross-SDK inconsistency**: Different SDKs may produce different string representations of the same address
3. **Ethereum compatibility**: Missing `0x` prefix and checksum support makes Hiero SDK addresses look inconsistent with standard Ethereum tooling
4. **Validator availability**: No standard way to validate EIP-55 checksums across SDKs


- All existing serialization/deserialization code unaffected

---

## Proposed Solution

Add **additive, non-breaking** EIP-55 utilities to standardize address string handling across all Hiero SDKs while preserving backward compatibility.

### Design Principles

1. **Preserve existing `to_string()` semantics across all SDKs**: Existing APIs (`to_string()`, `from_string()`, `__eq__`, serialization) never change
2. **Make checksum formatting explicitly opt-in**: Checksum formatting is available only when explicitly requested
3. **Maintain cross-SDK API consistency**: All SDKs expose the same API shape, even if implementations differ
4. **Align optional checksum formatting with Ethereum conventions**: Include `0x` prefix where appropriate to align with Ethereum tooling standards

---

## Proposed API Surface

### 1. Lowercase String Representation (Existing, Unchanged)

```python
# Returns: 20-byte hex address, lowercase, NO 0x prefix
address.to_string() → str
address.__str__()  → str
# Example: "fb69de3d6c4aeba17649f716e4e3d2b5ba7f0b3c"
```

**Rationale:**
- Preserves existing behavior for all SDKs
- No breaking changes to serialization, logging, or string comparisons
- This proposal intentionally preserves existing `to_string()` behavior for backward compatibility.

---

### 2. Checksummed String (New, Optional)

```python
# Returns: EIP-55 mixed-case hex address with 0x prefix
address.to_checksum_string() → str
# Example: "0xfb69De3d6c4aEba17649f716E4e3d2b5ba7F0b3c"
```

**Rationale:**
- Opt-in method for users who explicitly want checksummed output
- Includes `0x` prefix (aligns with Ethereum conventions)
- Does not affect default string representation
- Language idiom: `to_checksum_hex()` (Go), `toChecksumString()` (Java), `toChecksumString()` (JS), etc.

---

### 3. Validation Helper (New, Optional)

```python
# Static/class method: validates EIP-55 checksum
# Accepts addresses with or without 0x prefix
# Returns: True if valid checksum, False otherwise
EvmAddress.is_checksum_valid("0xfb69De3d6c4aEba17649f716E4e3d2b5ba7F0b3c") → bool
EvmAddress.is_checksum_valid("fb69De3d6c4aEba17649f716E4e3d2b5ba7F0b3c") → bool
EvmAddress.is_checksum_valid("fb69de3d6c4aeba17649f716e4e3d2b5ba7f0b3c") → bool  # 
# Lowercase or uppercase addresses are treated as syntactically valid,
# but are not considered checksummed representations.
# Raises ValueError on invalid address format (not 20 bytes)
```

**Rationale:**
- Helpful for applications receiving user input that might be checksummed
- Static method so no address object needed
- Validates format and checksum simultaneously
- Tolerates addresses without `0x` prefix (common in various contexts)

---

### 4. Alternative Constructor (Optional, SDK-Dependent)

Some SDKs may optionally offer a constructor that accepts checksummed input:

```python
# Python example (optional)
address = EvmAddress.from_string("0xfb69De3d6c4aEba17649f716E4e3d2b5ba7F0b3c")
# from_string() accepts both checksummed and unchecksummed
# Does NOT validate checksum (preserves backward compatibility)
```

**Rationale:**
- Makes it ergonomic to paste addresses from explorers (which often emit checksummed)
- Does NOT reject non-checksummed valid addresses
- Does NOT validate checksum on construction (that's what `is_checksum_valid()` is for)

**Breaking-change risk:** LOW

---

## Implementation Checklist

All SDKs should implement checksum derivation according to the EIP-55 specification using the keccak256 hash of the lowercase hexadecimal address.

### Testing Requirements
Each SDK implementation should include:
- Standard EIP-55 test vectors
- Round-trip checksum generation/validation tests
- Backward compatibility verification for existing string behavior
- Edge-case coverage for invalid lengths, malformed hex, and optional `0x` prefixes

---

## Breaking-Change Risk Analysis

| API Change | Breaking? | Rationale |
|---|---|---|
| Add `to_checksum_string()` | ❌ No | Pure addition, no existing API changes |
| Add `is_checksum_valid()` | ❌ No | New static method, no existing API changes |
| Change `to_string()` to return mixed-case | ✅ **YES** | Breaks string comparisons, logs, hashes, tests, serialized data |
| Change `from_string()` to reject invalid checksum | ✅ **YES** | Breaks callers passing uppercase addresses |
| Add `0x` prefix to `to_string()` | ✅ **YES** | Same impact as case change |
| Modify `__eq__` behavior | ✅ **YES** | Currently byte-based, would break if made case-sensitive |

This proposal intentionally preserves existing `to_string()` and `__eq__` semantics for backward compatibility across SDKs.

---

## Language-Specific Implementation Notes

### Python

```
## Language-Specific API Shape

| SDK | Proposed API |
|---|---|
| Python | `to_checksum_string()`, `is_checksum_valid()` |
| Java | `toChecksumString()`, `isChecksumValid()` |
| JavaScript/TypeScript | `toChecksumString()`, `isChecksumValid()` |
| Go | `ToChecksumHex()`, `IsChecksumValid()` |
| Rust | `to_checksum_string()`, `is_checksum_valid()` |

Exact naming may follow language-specific idioms while preserving equivalent functionality.
```

---

## Cross-SDK Coordination

### Phase 1: Design Alignment (Now)
- [ ] Review this proposal in sdk-collaboration-hub
- [ ] Confirm API shape across Java, JavaScript, Go, Rust, Python maintainers
- [ ] Agree on test vectors and acceptance criteria

### Phase 2: Implementation
- [ ] Implement in all SDKs using agreed API shape
- [ ] Submit PRs with shared test vectors
- [ ] Ensure `to_string()` guarantees are met

### Phase 3: Release
- [ ] Aim for reasonably aligned rollout across SDKs where feasible.
- [ ] Update documentation
- [ ] Share examples and best practices

---

## Alternatives Considered

### Alternative 1: Do Nothing
- ✅ Avoids any API changes
- ❌ Loses developer ergonomics advantage
- ❌ Leaves inconsistency with Ethereum tooling
- ❌ No cross-SDK position on 0x prefix

### Alternative 2: Make to_string() Return Checksummed by Default
- ✅ Great ergonomics
- ❌ **Breaking change** (disqualifying)
- ❌ Breaks all existing string comparisons and tests
- ❌ Violates non-breaking promise

### Alternative 3: Python-Only Implementation
- ✅ Fast to implement
- ❌ **Creates cross-SDK inconsistency** (disqualifying)
- ❌ Same address renders differently depending on SDK
- ❌ Causes subtle bugs downstream

### Selected: Additive Approach (This Proposal)
- Non-breaking
- Cross-SDK aligned
- Explicit opt-in
- Improved ergonomics where needed
- Ethereum-compatible conventions

---

## Open Questions & Future Scope

1. **Address input parsing**: Should `from_string()` accept `0x` prefix?
2. **EIP-55-only mode**: Should an application be able to require checksummed addresses? (Scope: future, if needed)
3. **Error messages**: What level of detail for checksum validation failures? (SDK discretion)
4. **Documentation**: Should SDKs document when/why to use checksummed addresses? (Recommended: yes)

---

## Acceptance Criteria

SDKs expose equivalent additive checksum APIs 
All SDKs use keccak256-based EIP-55 algorithm  
All SDKs pass shared test vectors  
All SDKs explicitly document that `to_string()` is unchanged  
SDKs should aim for API parity across releases where feasible.

---

## References

- [EIP-55: Mixed-case checksum address encoding](https://eips.ethereum.org/EIPS/eip-55)
- [hiero-sdk-python#2233](https://github.com/hiero-ledger/hiero-sdk-python/pull/2233) (original proposal)
- [Ethereum address checksumming](https://ethereum.org/en/developers/docs/standards/ether-address-checksum/)

---

## Feedback & Discussion

Please comment on:
1. **API shape**: Is `to_checksum_string()` + `is_checksum_valid()` the right surface?
2. **0x prefix**: Is including it in checksummed output the right choice?
3. **Scope**: Should SDKs optionally support strict checksum enforcement modes?
4. **Timeline**: What level of rollout coordination is appropriate across SDKs?

---

**Next Steps:**
- [ ] Share for feedback in SDK Community Call
- [ ] Refine based on maintainer feedback
- [ ] Proceed with coordinated implementation across SDKs