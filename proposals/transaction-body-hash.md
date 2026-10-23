# Whole-transaction body hash

## Summary

Add `getTransactionBodyHash()` to `Transaction` for comparing prepared transaction content and recording review attestations independently of transaction signature collection. It covers every node-specific body and every chunk in one 48-byte SHA-384 digest.

**Date Submitted:** 2026-10-23

**Status:** Discussion draft. The batch representation and its shared expected hashes must be agreed before this design is ready to implement.

Origin: [JavaScript SDK #4579](https://github.com/hiero-ledger/hiero-sdk-js/issues/4579). Keep that issue as the JS implementation task; coordinate the common contract here and link Java, Go, other SDK and TCK tasks as they are created. Implementations may ship in stages.

This identifies an ordered set of prepared bodies. Transaction IDs, node IDs and their order, fees, memo, chunk boundaries and chunk order affect the result. Equal high-level operations built independently are not guaranteed to hash equally: [Protobuf serialization is not canonical](https://protobuf.dev/programming-guides/serialization-not-canonical/).

### Gap in the original proposal: batch signatures

A batch body contains serialized `SignedTransaction` values for its children. Therefore, hashing the outer `bodyBytes` still includes the children's signature maps. Excluding only the outer signature map does not deliver full signature independence. See the current [JS batch encoding](https://github.com/hiero-ledger/hiero-sdk-js/blob/3f52c9073796c6c2a351d4d3b68986b6d466f19c/src/transaction/BatchTransaction.js#L138), [Java batch encoding](https://github.com/hiero-ledger/hiero-sdk-java/blob/b555d33d0f3581d05bf219683143cc5b9e4d78c1/sdk/src/main/java/com/hedera/hashgraph/sdk/BatchTransaction.java) and [Go batch encoding](https://github.com/hiero-ledger/hiero-sdk-go/blob/f449739e02fd90e0c0bc8f2c000efeec8cc8e623/sdk/batch_transaction.go).

**Recommended behavior:** exclude both outer and child transaction signature maps. Batch hashes must still commit to the outer body fields, every child body, and child order. This requires a separate hash-only representation for batches; never remove signatures from the transaction sent to the network.

**Decision required in this PR:** specify the exact batch representation, framing and byte-preservation rules, then publish shared expected hashes. A possible approach is replacing signed child envelopes with unsigned child-body representations in a temporary hash input. A generic decode/re-encode is insufficient without defining field ordering and unknown-field handling. This draft deliberately does not invent an interoperable encoding by implication.

If maintainers instead choose the smaller outer-signature-only contract, the API documentation and tests must explicitly say that changing child signatures changes the batch hash. Do not silently implement that behavior while promising full signature independence.

## New APIs

No new public types or standalone utilities.

## Updated APIs

### Transaction

```text
Transaction {
    @@throws(transaction-not-frozen-error, invalid-transaction-error)
    bytes getTransactionBodyHash()
}
```

Language bindings follow their existing hash-accessor conventions: JavaScript returns `Promise<Uint8Array>`, Java returns `byte[]`, and Go returns `([]byte, error)`. These are proposed bindings, not existing APIs.

- Require a frozen transaction with a complete prepared body set; reject missing or malformed bodies rather than returning a digest of an empty set.
- Return 48 raw bytes. Hex encoding is a presentation concern.
- Exclude transaction envelope signature maps, including child maps under the recommended batch contract. Signature or proof bytes that are actual transaction-body fields remain content; this is not a rule to remove every signature-shaped value.
- Repeated calls and signature additions/removals must preserve the digest while the prepared content is unchanged. Do not sign, submit, or invoke external signer callbacks just to compute this hash.
- Preserve body bytes, signatures and iteration position. The accessor may lock selected IDs and node membership consistently with existing hash accessors to prevent regeneration after review.

Existing network transaction hashes and per-node hash accessors keep their current behavior. The first-chunk limitation described in JS #4579 is a separate API issue; this proposal does not change the shape of its per-node map.

## Internal Changes

Reuse each SDK's prepared body collection. For ordinary and chunked transactions, use the original body bytes without decoding and re-encoding them. Traverse chunks first, then node IDs in their prepared order; use a single chunk for ordinary transactions.

```text
bodyHash = SHA384(concat(SHA384(bodyBytes[0]), ..., SHA384(bodyBytes[n-1])))
```

Each inner digest is exactly 48 bytes, giving unambiguous concatenation. Apply the outer hash even for one entry. Include every prepared entry; never hash only the first node or first chunk. This formula defines the ordinary/chunked path. The batch input to the same aggregation must be specified by the review decision above before implementation.

Construct unsigned bodies without triggering deferred signing. Any batch normalization operates on separate hash input and must leave the signed network bytes untouched. A shared aggregator with a batch-specific preparation step is sufficient in principle; no transaction hierarchy rewrite is required by this proposal.

### Response Codes

No new consensus-node response codes. Frozen-state and malformed-input failures are local SDK errors mapped through existing language conventions.

#### Transaction Retry

Hashing is local and performs no network retry. If the prepared body set changes, including regenerated transaction IDs or a different selected node set, the caller must review and hash that new content again.

## Test Plan

1. Given a fixed frozen single-entry transaction, when hashed, then its digest matches an independently computed double-SHA-384 expected value and has length 48 bytes.
2. Given multiple nodes and multiple file-append or topic-message chunks, when only a later node/chunk body changes, then the digest changes. Reordering nodes or chunks also changes it.
3. Given unchanged prepared bodies, when transaction signatures are added, removed or reordered, then the digest stays unchanged.
4. Given batches with identical outer fields and child bodies but different child signatures, when hashed, then their digests are equal under the recommended contract. Adding an outer signature also leaves the digest unchanged. This is a required regression test for the identified gap.
5. Given a batch, when a child body, child order, child count or outer body field changes, then the digest changes.
6. Given the same serialized fixtures imported by different SDKs, when hashed, then every SDK returns the same expected digest. Serialization roundtrips preserving the prepared body bytes retain the digest.
7. Given an unfrozen transaction, a missing body or malformed batch child bytes, when hashed, then a local error is returned and input state is preserved.
8. Given deferred signing, when the hash is requested repeatedly, then no signer callback is invoked, the iteration position is preserved and subsequent signing uses unchanged body bytes. Hash-only batch normalization must not remove or alter stored network signatures.

### TCK

Add an offline body-hash test specification and adapter operation in [hiero-sdk-tck](https://github.com/hiero-ledger/hiero-sdk-tck). The operation accepts the same serialized transaction fixtures and returns the raw digest encoded in one specified text format. Network execution is unnecessary.

Publish fixtures with explicit transaction IDs, node order, chunk size and expected hashes for the cases above, including batches with different child signature maps. SDK unit tests can reuse them before full TCK integration. The exact adapter request/response schema, batch encoding and golden vectors remain review deliverables; do not report cross-SDK compatibility from SDK-specific tests alone.

## SDK Example

Review and approve prepared content:

1. Freeze a transaction with its final transaction ID, node list and chunking, then serialize it for review.
2. Compute `getTransactionBodyHash()` and associate the digest with the application's review record.
3. Collect the network transaction signatures. Under the recommended contract, this includes child signature collection and rebuilding batches with unchanged outer fields and child bodies.
4. Import the transaction returned for approval and recompute the hash from its content. Compare it with the recorded digest; do not trust a supplied digest without recomputing it.
5. Approve only matching content. A matching body hash does not establish signature validity, authorization or successful network execution; those use the existing transaction workflow.
