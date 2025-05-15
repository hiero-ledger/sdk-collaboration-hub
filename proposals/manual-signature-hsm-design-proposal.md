# Manual Signature Injection Support for HSM-based Transaction Signing

**Date Submitted:** 2025-04-16

## Summary

This proposal introduces a new capability in the Hiero SDKs (JavaScript, Java, Go, etc.) to allow **external signing of transactions via HSM (Hardware Security Modules)**. In many enterprise use cases, private keys must remain in secure hardware and cannot be exposed to the application. Developers need the ability to:

- Extract the canonical `bodyBytes` that require signing.
- Sign externally (e.g., with an HSM).
- Inject the signature back into the transaction using supported SDK methods.

Currently, in the JavaScript SDK, `PrivateKey.sign(transaction.toBytes())` does not produce a network-valid signature because it signs the entire transaction, not the canonical `bodyBytes`.

This proposal introduces a new structured way to expose these canonical bytes clearly associated with their node account IDs.

## New API

### `SignableNodeTransactionBodyBytes`

**Type**: `Class`  
**Description**: Represents a transaction body ready for external signing, explicitly associated with a node account ID and transaction ID.

**Implementation**:

```javascript
export class SignableNodeTransactionBodyBytes {
  constructor(nodeAccountId, transactionId, signableTransactionBodyBytes) {
    this.nodeAccountId = nodeAccountId;
    this.transactionId = transactionId;
    this.signableTransactionBodyBytes = signableTransactionBodyBytes;
  }
}
```

### `Transaction.signableNodeBodyBytesList`

**Type**: `SignableNodeTransactionBodyBytes[]`  
**Description**: Returns an array of `SignableNodeTransactionBodyBytes` containing the canonical `bodyBytes` paired explicitly with their respective `nodeAccountId` and `transactionId` for signing.

**POC Implementation**:

```javascript
    get signableNodeBodyBytesList() {
        this._requireFrozen();

        return this._signedTransactions.list.map((signedTransaction) => {
            if (!signedTransaction.bodyBytes) {
                throw new Error("Missing bodyBytes in signed transaction.");
            }

            const body = HieroProto.proto.TransactionBody.decode(
                signedTransaction.bodyBytes,
            );

            if (!body.nodeAccountID) {
                throw new Error("Missing nodeAccountID in transaction body.");
            }

            const nodeAccountId = AccountId._fromProtobuf(body.nodeAccountID);
            if (!body.transactionID) {
                throw new Error("Missing transactionID in transaction body.");
            }

            const transactionId = TransactionId._fromProtobuf(
                body.transactionID,
            );

            return new SignableNodeTransactionBodyBytes(
                nodeAccountId,
                transactionId,
                signedTransaction.bodyBytes,
            );
        });
    }
```

**JavaScript Example**:

```javascript
const signableTransactionNodeBodyBytesList = transaction.signableNodeBodyBytesList;
// send each signableTransactionNodeBodyBytesList[i] to HSM for signing
```

---

## Test Plan

1. **Given** a transaction with one node,  
   **When** its `bodyBytes` is signed externally and injected,  
   **Then** the transaction executes successfully.

2. **Given** a transaction with multiple nodes,  
   **When** all node-specific signatures are correctly applied,  
   **Then** the transaction executes successfully with retries allowed.

3. **Given** a chunked transaction with one node,  
   **When** all its chunk-specific `bodyBytes` are signed externally and injected,  
   **Then** the transaction executes successfully.

4. **Given** a chunked transaction with multiple nodes,  
   **When** all chunk-specific and node-specific signatures are correctly applied,  
   **Then** the transaction executes successfully with retries allowed.

5. **Given** a transaction with an invalid or mismatched signature applied via `signableNodeBodyBytesList`,  
   **Then** the transaction fails with `INVALID_SIGNATURE`.

6. **Given** a call to `signableNodeBodyBytesList` on a frozen transaction,  
   **Then** the returned array length equals the number of node IDs.

7. **Given** a call to `signableNodeBodyBytesList` before freezing,  
   **Then** an error is thrown.

---

## SDK Example

**Use case**: Signing a transaction externally via HSM.

**JavaScript Example**:

```javascript
        const tx = new TransferTransaction()
            .addHbarTransfer(senderId, Hbar.fromTinybars(-100))
            .addHbarTransfer(receiverId, Hbar.fromTinybars(100))
            .setTransactionId(TransactionId.generate(senderId))
            .freezeWith(client);

        const signableTransactionNodeBodyBytesList =
            tx.signableNodeBodyBytesList;

        // hsmSign is not part of this SDK.
        // It is a placeholder function for an external signing service (e.g., a Hardware Security Module or KMS)
        // that generates a digital signature for the transaction body bytes.
        const signatureMap = new SignatureMap();

        for (const {
            nodeAccountId,
            transactionId,
            signableTransactionBodyBytes,
        } of signableTransactionNodeBodyBytesList) {
            const signature = await hsmSign(signableTransactionBodyBytes);

            signatureMap.addSignature(
                nodeAccountId,
                transactionId,
                senderPublicKey,
                signature,
            );
        }

        tx.addSignature(senderPublicKey, signatureMap);

        const response = await tx.execute(client);
        const receipt = await response.getReceipt(client);
        console.log("Transaction status:", receipt.status.toString());
```

---

## Compatibility

- Fully backward compatible.
- No changes to existing APIs or behavior.
- Works seamlessly with multi-node and single-node transactions.
- Clearly associates each signable body with the corresponding node ID for improved clarity and type safety.
- Optional for users—only needed for external HSM signing workflows.

## Conclusion

This enhancement provides a simple and effective way for enterprise developers to securely sign Hedera transactions using external key infrastructure. By exposing the correct `signableTransactionBodyBytes` through `SignableNodeTransactionBodyBytes` class, the SDK becomes safer, more transparent, and more robust for production use cases requiring external signing.
