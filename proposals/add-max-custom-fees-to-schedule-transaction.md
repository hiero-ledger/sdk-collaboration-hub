# Max Custom Fees in Schedulable Transactions

## Summary

This design implements a missed feature in HIP-991 that adds a `max_custom_fees` field to schedulable transactions. This allows users to specify the maximum amount they're willing to pay for a custom fee for a scheduled transaction.

## Internal Changes

`max_custom_fees` should already be in the SDKs as a field in `Transaction` as a part of the HIP-991 implementation. Internal processing of scheduled transactions differ between SDKs, so whatever function the `Transaction` is converted to a `SchedulableTransactionBody` protobuf (i.e. `_getScheduledTransactionBody()` in JS, `WrappedTransaction.toSchedulableProtobuf()` in C++, etc.), the `max_custom_fees` should be included in that protobuf as well.

## Test Plan

1. Given a transaction with maximum custom fee limits, when it gets scheduled on the network, then the maximum custom fee limits should be a part of that scheduled transaction.
2. Given a transaction with maximum custom fee limits, when it gets scheduled and executed on the network and the fee is over the maximum custom fee limit, then the scheduled transaction fails.

## SDK Example

Edit the same example created with the original HIP-991 implementation. Add these steps.

1. Schedule a topic message submission with a custom maximum fee limit and a custom fee.
2. Verify the custom maximum fee limit is enforced on the scheduled topic message submission _before_ it executes.
3. Wait for the topic message submission to execute and verify it executed properly.
4. Schedule another topic message submission with a custom maximum fee limit below the custom fee.
5. Wait for the topic message submission to execute and verify it failed.
