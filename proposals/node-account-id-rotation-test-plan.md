# Node Account ID Rotation Test Plan

## Summary

This test plan validates the functionality for updating the `account_id` field in `NodeUpdateTransactionBody` as part of the Dynamic Address Book (DAB) enhancement. The feature allows node operators to change the account associated with their node.

## Test Plan

1. Given a node with an existing account_id, when a NodeUpdateTransaction is submitted to change the account_id to a new valid account with signatures from both the node admin key and the account id key, then the transaction succeeds.

2. Given a node with an existing account_id, when a NodeUpdateTransaction is submitted to change the account_id to a new valid account with only the node admin key signature (missing account id signature), then the transaction fails with `INVALID_SIGNATURE`.

3. Given a node with an existing account_id, when a NodeUpdateTransaction is submitted to change the account_id to a new valid account with only the account id key signature (missing node admin), then the transaction fails with `INVALID_SIGNATURE`.

4. Given a node with an existing account_id, when a NodeUpdateTransaction is submitted to change the account_id to the same existing account_id with proper signatures, then the transaction succeeds.

5. Given a node with an existing account_id, when a NodeUpdateTransaction is submitted to remove the account_id (set to 0.0.0) with the node admin key signature, then the transaction succeeds.

6. Given a node with an existing account_id, when a NodeUpdateTransaction is submitted to remove the account_id (set to 0.0.0) without admin key signature, then the transaction fails with `INVALID_SIGNATURE`.

7. Given a node whose account_id has been updated, when the SDK queries the node information and submits a transaction to that node, then the SDK uses the new account_id and the transaction is processed successfully.

8. Given a node with an existing account_id, when a NodeUpdateTransaction is submitted to change the account_id to a non-existent account with proper signatures, then the transaction fails with `INVALID_ACCOUNT_ID`.

9. Given a node with an existing account_id, when a NodeUpdateTransaction is submitted to change the account_id to an deleted account with proper signatures, then the transaction fails with `INVALID_ACCOUNT_ID`.

10. Given a node update transaction that attempts to set account_id to the treasury account (0.0.2), the transaction fails.

11. Given multiple nodes with different existing account_ids, when NodeUpdateTransactions are submitted to change all of their account_ids to the same shared account with proper signatures, then all transactions succeed and the nodes share the same account_id after roster activation.

12. Given a node with an existing account_id where a transaction is successfully submitted, when the node's account_id is updated to a new account and a subsequent transaction is submitted using `setNodeAccountIDs` with the old account_id, then the transaction fails with `INVALID_NODE_ACCOUNT`.

13. Given a scheduled transaction is created and submitted to a node with the original account_id, when the node's account_id is updated to a new account and a ScheduleSignTransaction is subsequently submitted to sign the scheduled transaction, then the scheduled transaction executes successfully once all required signatures are collected.

## SDK Example

TODO