## Attendees

- Ivan Asenov
- Hendrik Ebbers
- Keith Kowal
- Angelina Ceppaluni
- Tayebwa Noah

## Minutes

- Hiero hooks
  - Ivan reviewed the SDK design doc for Hiero hooks (HIP-1195).
  - Hendrik provided good feedback, including:
    - Specifying when fields can be invalid/null.
    - Make `LambdaStorageUpdate` an abstract class, with `LambdaStorageSlot` and `LambdaMappingEntry` as derived classes.
    - Rename `LambdaSStoreTransaction` to something more user-friendly.
      - Keith mentioned the only users who would use this transaction would understand already what it means.
      - Ivan suggested `HookUpdateTransaction`, but will talk with Michael Tinker about it.
    - Specifying when returned lists should be mutable/immutable.
    - Shorten up `TransferTransaction` APIs (6 parameters down to 4).
- Keith mentioned more discussion of design docs should be done in future SDK community meetings.
- 