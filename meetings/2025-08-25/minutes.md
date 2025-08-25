## Attendees

- Robert Walworth
- Angelina Ceppaluni
- Ivan Asenov
- Keith Kowal
- Hendrik Ebbers
- Tayebwa Noah
- Anuj Saxena

## Minutes

- Hendrik Java SDK prototype demo
    - Presented problematic patterns in current SDKs
      - API allows for invalid operations
      - Errors caught at runtime, not compile-time
    - New API
      - Prevents users from experiencing runtime errors
      - Provides intuitive way for users to use SDK
    - Discussed transaction mutability
      - Does it make sense to have `freeze()`?
      - New model proposed for "frozen" transactions ("packed" transactions)
    - Next steps
      - Come up with more developer documentation
      - Start new discussion in SDK hub