# Reference Hiero SDK Sample Code Guide

This guide defines the standard structure and conventions for writing Hedera SDK code examples across SDK repositories, blogs, developer documentation, and the Hedera Portal Playground.

It is intended for developers contributing to the SDK or creating sample code for docs and Playground environments.

Each example must use ECDSA (w EVM Alias from Public Key) as the default key type, support both .env and hardcoded credentials, follow a consistent step structure, include debug logging and HashScan links, and close the client gracefully.


## Quick Reference Standards

| **Element**                | **Standard**                                |
|----------------------------|---------------------------------------------|
| Default key type           | Always ECDSA w/ EVM Address from Public key |
| Operator setup             | `.env` or hardcoded fallback                |
| Environment variable names | `HEDERA_OPERATOR_ID`, `HEDERA_OPERATOR_KEY` |
| Client setup               | `Client.forTestnet().setOperator()`         |
| Transaction flow           | Use `// Step 1`, `// Step 2`, … convention  |
| Error handling             | Inline `try`/`catch` with clear output      |
| Shutdown                   | Always `await client.close()`               |

## Logging Conventions

| Type       | Example                                                          |
|------------|------------------------------------------------------------------|
| Connection | `console.log("Connected to Hedera Testnet");`                    |
| Progress   | `console.log("Transaction submitted, awaiting receipt...");`     |
| Result     | `console.log("New Account ID: ", receipt.accountId.toString());` |
| Explorer   | `console.log("HashScan: ", url);`                                |
| Error      | `console.error("Error: ", err.message);`                         |
| Shutdown   | `console.log("Client closed successfully");`                     |

## `.env` Example
```sh
# .env example
OPERATOR_ID=0.0.1234
OPERATOR_KEY=302e020100300506032b657004220420...
```

## JS Code Example: Token Creation
```javascript
/**
 * Example: Create a fungible token (ECDSA default)
 * Network: Testnet
 *
 * Best practices demonstrated:
 * - ECDSA keys by default (fromStringECDSA / generateECDSA)
 * - .env-first operator config, with optional hardcoded fallback for Playground
 * - Minimal client init (Client.forTestnet().setOperator(...))
 * - Full explicit transaction lifecycle: build → execute → getReceipt → log
 * - Debug guidance via clear error logging (no helpers, no abstraction)
 * - Never print private keys; only public identifiers
 * - Graceful client shutdown in finally { await client.close() }
 */
import {
  Client,
  AccountId,
  PrivateKey,
  TokenCreateTransaction,
  TokenType
} from "@hashgraph/sdk";
import dotenv from "dotenv";
dotenv.config();

async function main() {
    let client;
    
    try {
        // Step 1 – Operator configuration (.env first, fallback for Playground)
        // Use ECDSA for wallet/EVM compatibility. Never log private keys.
        const operatorIdRaw  = process.env.OPERATOR_ID  || "0.0.xxxx";
        const operatorKeyRaw = process.env.OPERATOR_KEY || "302e02...replace_me...";
    
        const operatorId  = AccountId.fromString(operatorIdRaw);
        const operatorKey = PrivateKey.fromStringECDSA(operatorKeyRaw);
    
        // Guard against placeholders — fail fast to avoid misleading network errors.
        if (operatorId.toString() === "0.0.xxxx" || operatorKeyRaw.includes("replace_me")) {
          throw new Error("Set OPERATOR_ID/OPERATOR_KEY in .env or hardcoded fallback before running this example.");
        }
    
        // Step 2 – Client initialization
        // Always start with a clean, minimal client setup.
        client = Client.forTestnet().setOperator(operatorId, operatorKey);
        console.log("Connected to Hedera Testnet");
        console.log(`Operator: ${operatorId}`);
    
        // Step 3 – Build transaction
        // Minimal reproducible example; always specify TokenType explicitly.
        const tx = new TokenCreateTransaction()
          .setTokenName("Demo Token")
          .setTokenSymbol("DMT")
          .setTokenType(TokenType.FungibleCommon)
          .setTreasuryAccountId(operatorId)
          .setInitialSupply(1000);
    
        console.log("Transaction built; submitting to network...");
        // Note: No freeze/sign needed here since only the operator auto-signs.
        // Use .freezeWith() and .sign() in multi-signature examples.
    
        // Step 4 – Execute transaction
        // The execute() call sends the transaction to the network and returns a TransactionResponse.
        const txResponse = await tx.execute(client);
        console.log("Transaction submitted. Awaiting receipt...");
    
        // Step 5 – Get transaction receipt
        // Fetch the receipt to confirm consensus from the network.
        const receipt = await txResponse.getReceipt(client);
        // Note: Hedera transactions require two steps — execute() submits to the network,
        // getReceipt() waits for final consensus confirmation.
    
        // Step 6 – Log results
        console.log("Transaction status:", receipt.status.toString());
        console.log("Token ID:", receipt.tokenId.toString());
    
        // Always show a HashScan link for easy verification.
        const txId = txResponse.transactionId.toString();
        console.log("HashScan:", `https://hashscan.io/testnet/tx/${txId}`);
    
    } catch (err) {
        // Step 7 – Error handling and debug
        console.error("Error:", err?.name || "Unknown", "-", err?.message || err);
        if (err?.status) console.error("Status:", err.status.toString());
        if (err?.transactionId) console.error("TxID:", err.transactionId.toString());
        if (err?.cause?.message) console.error("Cause:", err.cause.message);
        
            // Optional quick hints for common cases
            const status = err?.status?.toString();
            if (status === "INSUFFICIENT_PAYER_BALANCE") {
              console.error("Hint: Fund the operator account or reduce transaction cost.");
            } else if (status === "INVALID_SIGNATURE") {
              console.error("Hint: Check operator key or required signatures.");
            } else if (status === "TRANSACTION_EXPIRED") {
              console.error("Hint: Rebuild and resubmit with a fresh transaction ID.");
            }
        
        } finally {
        // Step 8 – Graceful shutdown
        if (client) await client.close();
        console.log("Client closed successfully");
    }
}

main();
```

## Example Output

```
Connected to Hedera Testnet
Operator: 0.0.1234
Transaction built; submitting to network...
Transaction submitted. Awaiting receipt...
Transaction status: SUCCESS
Token ID: 0.0.5678
HashScan: https://hashscan.io/testnet/tx/0.0.1234@1700000000.111111111
Client closed successfully
```
