# Transaction Size Calculation Methods

## Summary

This proposal introduces new getter methods to the Hiero SDKs (JavaScript, Java, Go, etc.) to allow **calculating transaction sizes before submission**. These methods are essential for:

- Determining transaction fees more accurately before execution
- Validating transactions against network size limits
- Supporting advanced transaction batching scenarios

The proposed methods are:

- `size`: Returns the total transaction size in bytes after protobuf encoding
- `bodySize`: Returns just the transaction body size in bytes after encoding
- `bodySizeAllChunks`: Returns an array of body sizes for transactions with multiple chunks (like FileAppendTransaction)

## New API

### `Transaction.size`

**Type**: `Promise<number>`  
**Description**: Returns the total size in bytes of the transaction after protobuf encoding. This includes signatures, transaction body, and all other transaction components.

Since the actual wire format can only be known after encoding, this method returns a Promise.

**Implementation**:

```javascript
get size() {
    return this._makeRequestAsync().then(
        (request) =>
            HieroProto.proto.Transaction.encode(request).finish().length,
    );
}
```

### `Transaction.bodySize`

**Type**: `number`  
**Description**: Returns the size in bytes of just the transaction body after protobuf encoding. This is useful for understanding the core transaction size without signatures. Implementation uses Node Account Id `0.0.0` as the node account id doesn't matter for the transaction body size.

**Implementation**:

```javascript
get bodySize() {
    const body = this._makeTransactionBody(AccountId.fromString("0.0.0"));
    return HieroProto.proto.TransactionBody.encode(body).finish().length;
}
```

### `FileAppendTransaction.bodySizeAllChunks`

**Type**: `number[]`  
**Description**: Returns an array of body sizes for all chunks in a FileAppendTransaction. This is particularly useful for transactions that need to be split into multiple chunks, such as large file appends.

**Implementation**:

```javascript
/**
 * Get the body sizes for all chunks in a FileAppendTransaction.
 * For transactions with multiple chunks (like large file appends),
 * this returns an array containing the size of each chunk's transaction body.
 * The size is calculated by encoding the transaction body to protobuf format.
 *
 * @returns {number[]} An array of body sizes, where each element represents
 * the size in bytes of a chunk's transaction body
 *
 */
get bodySizeAllChunks() {
    const bodySizes = [];

    // Store sizes for each chunk
    for (let i = 0; i < this.getRequiredChunks(); i++) {
        // Set index directly
        this._transactionIds.index = i;
        // Use super.bodySize to access the base class implementation
        bodySizes.push(super.bodySize);
    }
    // Restore to initial index
    this._transactionIds.index = 0;
    return bodySizes;
}
```

## Internal Changes

- All of the new methods leverage existing internal transaction serialization methods
- The `size` getter uses `_makeRequestAsync()` to get the full transaction with signatures
- The `bodySize` getter uses `_makeTransactionBody()` with a placeholder account ID
- The `bodySizeAllChunks` method iterates through all chunks in a multi-chunk transaction, calculating the body size for each
- No changes to core transaction handling are needed

## Use Cases

1. **Size Validation**: Applications can check if transactions exceed network size limits before submission
2. **Transaction Batching**: Services that batch operations can optimize how many operations fit within size limits
3. **Network Analysis**: Tools can analyze transaction composition by comparing body size to total size
4. **Multi-chunk Planning**: When working with large files, developers can plan for the size of each chunk

## JavaScript Example

```javascript
// Creating a transaction
const tx = await new TransferTransaction()
  .addHbarTransfer(senderId, Hbar.fromTinybars(-100))
  .addHbarTransfer(receiverId, Hbar.fromTinybars(100))
  .freezeWith(client);

// Get transaction sizes
const totalSize = await tx.size;
const bodySize = tx.bodySize;

console.log(`Transaction total size: ${totalSize} bytes`);
console.log(`Transaction body size: ${bodySize} bytes`);
console.log(`Overhead (signatures, etc.): ${totalSize - bodySize} bytes`);

// Use sizes for fee calculation or validation
if (totalSize > MAX_TRANSACTION_SIZE) {
  console.error("Transaction exceeds maximum allowed size");
}

// For file append operations, we can examine all chunk sizes
const fileAppendTx = new FileAppendTransaction()
  .setFileId(fileId)
  .setContents(largeFileContents)
  .freezeWith(client);

const chunkSizes = fileAppendTx.bodySizeAllChunks;
console.log(`File will be uploaded in ${chunkSizes.length} chunks`);
console.log(`Chunk sizes in bytes: ${chunkSizes.join(", ")}`);
```

## Compatibility

- Fully backward compatible - adds new getters without modifying existing behavior
- No changes to transaction execution or serialization
- Works with all transaction types
- Special handling for multi-chunk transactions via FileAppendTransaction

## Implementation Notes

- The implementations should use the SDK's existing protobuf encoding methods
- The `size` getter is asynchronous as it depends on request preparation
- The `bodySize` getter is synchronous and uses a placeholder account ID for body creation
- transaction needs to be frozen before calculating the transaction size
- The `bodySizeAllChunks` method temporarily modifies the transaction index for size calculation but restores it afterward

## Conclusion

Adding these size calculation methods provides developers with important tools for transaction management. They enable more accurate fee calculation, better transaction validation, and support advanced usage patterns like batching and multi-chunk file operations. These methods should be consistent across all Hiero SDKs.
