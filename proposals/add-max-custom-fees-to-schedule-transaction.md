# Max Custom Fees in Schedulable Transactions

## Summary

This design implements a missed feature in HIP-991 that adds a `max_custom_fees` field to schedulable transactions. This allows users to specify the maximum amount they're willing to pay for a custom fee for a scheduled transaction.

## Internal Changes

`max_custom_fees` should already be in the SDKs as a field in `Transaction` as a part of the HIP-991 implementation. Internal processing of scheduled transactions differ between SDKs, so whatever function the `Transaction` is converted to a `SchedulableTransactionBody` protobuf (i.e. `_getScheduledTransactionBody()` in JS, `WrappedTransaction.toSchedulableProtobuf()` in C++, etc.), the `max_custom_fees` should be included in that protobuf as well.

## Test Plan

1. Given an account with an Hbar balance, a topic with a custom fixed fee that charges Hbar, and a message that is scheduled to submit to that topic and paid for by that account with a maximum custom fee amount bigger than topic custom fee amount, when the scheduled transaction executes, then the account is debited the custom fixed fee amount of Hbar.
2. Given an account with an Hbar balance, a topic with a custom fixed fee that charges Hbar, and a message that is scheduled to submit to that topic and paid for by that account without specifying a maximum custom fee, when the scheduled transaction executes, then the account is debited the custom fixed fee amount of Hbar.
3. Given a fungible token, an account with a balance of that token, a topic with a custom fixed fee that charges that token, and a message that is scheduled to submit to that topic and paid for by that account with a maximum custom fee amount bigger than topic custom fee amount, when the scheduled transaction executes, then the account is debited the custom fixed fee amount of the token.
4. Given a fungible token, an account with a balance of that token, a topic with a custom fixed fee that charges that token, and a message that is scheduled to submit to that topic and paid for by that account without specifying a maximum custom fee amount, when the scheduled transaction executes, then the account is debited the custom fixed fee amount of the token.
5. Given an account with an Hbar balance, a topic with a custom fixed fee that charges Hbar, and a message that is scheduled to submit to that topic and paid for by that account with a maximum custom fee amount smaller than topic custom fee amount, when the scheduled transaction is executed, then the transaction fails with `MAX_CUSTOM_FEE_LIMIT_EXCEEDED`.
6. Given a fungible token, an account with a balance of that token, a topic with a custom fixed fee that charges that token, and a message this is scheduled to submit to that topic and paid for by that account with a maximum custom fee amount smaller than topic custom fee amount, when the scheduled transaction executes, then the transaction fails with `MAX_CUSTOM_FEE_LIMIT_EXCEEDED`.
7. Given a fungible token, an account with a balance of that token, a topic with a custom fixed fee that charges that token, when a message is scheduled to be submitted to that topic and paid for by that account, specifying maximum custom fee with invalid tokenID, then the schedule transaction fails with `NO_VALID_MAX_CUSTOM_FEE`.
8. Given a fungible token, an account with a balance of that token, a topic with a custom fixed fee that charges that token, when a message is scheduled to be submitted to that topic and paid for by that account, specifying maximum custom fee list with duplicate denominations, then the schedule transaction fails with `DUPLICATE_DENOMINATION_IN_MAX_CUSTOM_FEE_LIST`.

## SDK Example

Edit the same example created with the original HIP-991 implementation. Add these steps.

1. Schedule a topic message submission with a custom maximum fee limit and a custom fee.
2. Verify the custom maximum fee limit is applied to the scheduled topic message submission _before_ it executes.
3. Wait for the topic message submission to execute and verify it executed properly.
4. Schedule another topic message submission with a custom maximum fee limit below the custom fee.
5. Wait for the topic message submission to execute and verify it failed.
