# Manual Signature Injection Support for HSM-based Transaction Signing

**Date Submitted:** 2025-04-16

## Summary

This proposal introduces a new capability in the Hiero SDKs (JavaScript, Java, Go, etc.) to allow **external signing of transactions via HSM (Hardware Security Modules)**. In many enterprise use cases, private keys must remain in secure hardware and cannot be exposed to the application. Developers need the ability to:

- Extract the canonical `bodyBytes` that require signing.
- Sign externally (e.g., with an HSM).
- Inject the signature back into the transaction using supported SDK methods.

Currently, in the JavaScript SDK, `PrivateKey.sign(transaction.toBytes())` does not produce a network-valid signature because it signs the entire transaction, not the canonical `bodyBytes`.

This proposal introduces a single new public getter to expose these canonical bytes: `signableBodyBytesList`.

## New API

### `Transaction.signableBodyBytesList`

**Type**: `Uint8Array[]`  
**Description**: Returns an array of `bodyBytes` for each node that the transaction targets. These are the exact serialized protobuf `TransactionBody` values used to compute the transaction hash and must be signed externally.

The transaction **must be frozen** before calling this method. If not frozen, the SDK will throw an error to ensure the returned bytes are stable.

This API is intended for secure signing workflows via HSMs, KMS, or offline signing mechanisms.

- Expose `bodyBytes` through the new `signableBodyBytesList` getter.
- Ensure this getter throws if the transaction is not frozen (`_requireFrozen()`).
- Ensure ordering of returned `bodyBytes` aligns with the order of `nodeAccountIds`.

**POC Implementation**:

```javascript
    get signableBodyBytesList() {
        this._requireFrozen();

        const result = [];
        const nodeAccountIds = this._nodeAccountIds.list;

        for (let i = 0; i < nodeAccountIds.length; i++) {
            const nodeId = nodeAccountIds[i];
            const signedTransaction = this._signedTransactions.get(i);

            if (!signedTransaction?.bodyBytes) {
                throw new Error(
                    'Missing bodyBytes for node ' + nodeId.toString()
                );
            }

            result.push(signedTransaction.bodyBytes);
        }

        return result;
    }
```

**JavaScript Example**:

    ```javascript
    const transactionSignableBodyBytesList = transaction.signableBodyBytesList;
    // send each transactionSignableBodyBytesList[i] to HSM for signing
    ```

---

## Test Plan

1. **Given** a transaction with one node,  
   **When** its `bodyBytes` is signed externally and injected,  
   **Then** the transaction executes successfully.

2. **Given** a transaction with multiple nodes,  
   **When** all node-specific signatures are correctly applied,  
   **Then** the transaction executes successfully with retries allowed.

3. **Given** a transaction with an invalid or mismatched signature applied via `signableBodyBytesList`,  
   **Then** the transaction fails with `INVALID_SIGNATURE`.

4. **Given** a call to `signableBodyBytesList` on a frozen transaction,  
   **Then** the returned array length equals the number of node IDs.

5. **Given** a call to `signableBodyBytesList` before freezing,  
   **Then** an error is thrown.

---

## TCK

Define TCK tests to verify:

- Returned `bodyBytes` match canonical encoding per protobuf.
- Valid externally signed transactions are accepted by the network.
- Multi-node signing succeeds only when all `bodyBytes` are signed.
- Single-node behavior remains unchanged.
- Invalid signature injection results in `INVALID_SIGNATURE`.

---

## SDK Example

**Use case**: Signing a transaction externally via HSM.

**JavaScript Example**:

```javascript
const tx = await new TransferTransaction()
  .addHbarTransfer(senderId, Hbar.fromTinybars(-100))
  .addHbarTransfer(receiverId, Hbar.fromTinybars(100))
  .setTransactionId(TransactionId.generate(senderId))
  .setNodeAccountIds([node1, node2])
  .freezeWith(client);

const bodyBytesList = tx.signableBodyBytesList;

// hsmSign is not part of this SDK.
// It is a placeholder function for an external signing service (e.g., a Hardware Security Module or KMS)
// that generates a digital signature for the transaction body bytes.
const signature1 = hsmSign(bodyBytesList[0]);
const signature2 = hsmSign(bodyBytesList[1]);

// Add the transaction HSM signatures for all the nodes to a SignatureMap
// Note: Each signature must be created using the bodyBytes of the specific nodeId
// where the signature is being attached. Signatures must exactly correspond
// to the node they're assigned to, or the transaction will fail.
const signatureMap = new SignatureMap();

signatureMap.addSignature(
  node1,
  transaction.transactionId,
  publicKey,
  signature1
);

signatureMap.addSignature(
  node2,
  transaction.transactionId,
  publicKey,
  signature2
);

// Add the populated signature map to the transaction
tx.addSignature(publicKey, signatureMap);

const response = await tx.execute(client);
const receipt = await response.getReceipt(client);
console.log("Transaction status:", receipt.status.toString());
```

## Compatibility

- Fully backward compatible.
- No changes to existing APIs or behavior.
- Works seamlessly with multi-node and single-node transactions.
- Optional for users—only needed for HSM/external signing flows.

## Conclusion

This enhancement provides a simple and effective way for enterprise developers to securely sign Hedera transactions using external key infrastructure. By exposing the correct `bodyBytes` through `signableBodyBytesList`, the SDK becomes safer, more transparent, and more robust for production use cases requiring external signing.
