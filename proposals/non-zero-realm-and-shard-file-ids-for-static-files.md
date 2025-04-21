# Non-Zero Realm and Shard File IDs for Static Files
**Date Submitted**: 2025-04-16

## Summary

This proposal introduces new APIs to allow a user to retrieve the file IDs of static files on a Hedera network. Currently, the Hiero SDKs provide hardcoded `FileId` values for the address book, fee schedule, and exchange rate files on a Hedera network, however these are hardcoded with values of zero for the realm and shard. Running a network that is not on the zero realm and shard will render these hardcoded values useless and may bring about confusing and unwanted behavior for users. A user may create their own `FileId` from the hardcoded values and update the realm/shard manually, but this makes for a bad user experience and the SDKs should provide this functionality. Attempting to change the hardcoded values will also break any user already using them, so this is not an option.

In addition to this, the `Client.forMirrorNetwork()` API assumes a zero realm and shard as well in its under-the-hood `AddressBookQuery`. Attempting to establish a network with `Client.forMirrorNetwork` with a non-zero realm and shard will cause the `Client` to not be initialized properly, and as such no communication between the `Client` and network can take place.

This proposal aims to fix this issue by providing new APIs to allow a user to set which realm and shard from which to retrieve a static file, as well as fixing the `AddressBookQuery` used by `Client.forMirrorNetwork()`.

## New APIs

### `FileId FileId.getAddressBookFileIdFor(int64 realm, int64 shard)`
**Description**: Get the `FileId` of the Hedera address book for the given realm and shard. This should be a _static_ function that lives within `FileId`.

- `int64 realm`: The realm from which to get the address book file ID.
- `int64 shard`: The shard from which to get the address book file ID.

### `FileId FileId.getFeeScheduleFileIdFor(int64 realm, int64 shard)`
**Description**: Get the `FileId` of the Hedera fee schedule for the given realm and shard. This should be a _static_ function that lives within `FileId`.

- `int64 realm`: The realm from which to get the fee schedule file ID.
- `int64 shard`: The shard from which to get the fee schedule file ID.

### `FileId FileId.getExchangeRatesFileIdFor(int64 realm, int64 shard)`
**Description**: Get the `FileId` of the Hedera exchange rates for the given realm and shard. This should be a _static_ function that lives within `FileId`.

- `int64 realm`: The realm from which to get the exchange rates file ID.
- `int64 shard`: The shard from which to get the exchange rates file ID.

---

## Updated APIs

### `Client Client.forMirrorNetwork(List<string>)`
**Description**: `forMirrorNetwork` uses a list of mirror node URLs to query and get the address book for a network and uses that address book to initialize the `Client` network. Additions need to be made to this function signature to allow realm and shard values to be passed, so the correct address book can be queried.

Two new arguments need to be added to this function:
- `int64 realm`: The realm from which to get the address book file ID.
- `int64 shard`: The shard from which to get the address book file ID.

To prevent breaking changes, these arguments should be added in one of two ways (depending on language capabilities):
1. The original function should contain the `realm` and `shard` arguments with default values of `0`.
2. A new overload of `forMirrorNetwork` should be created with the `realm` and `shard` inputs.

Both of these updates will allow previous uses of `forMirrorNetwork` to continue to work properly, either using default `0` values for realm and shard (option #1), or using the first overload of `forMirrorNetwork` (option #2). Option #1 should be opted for if the language allows to prevent code bloat.

The new function signature would look like either:
1. `Client Client.forMirrorNetwork(List<string>, int64 realm = 0, int64 shard = 0)`
2. `Client Client.forMirrorNetwork(List<string>, int64 realm, int64 shard)`

This updated API will allow users to continue to establish their `Client` network and communications using a mirror node address book, even if they are deployed on a non-zero realm and/or shard network.

---

## Test Plan

1. **Given** a network with a non-zero realm and/or shard address book, **When** `getAddressBookFileIdFor` is called with the corresponding realm and shard, **Then** the returned `FileId` is valid and is the ID of the corresponding address book.
2. **Given** a network with a non-zero realm and/or shard fee schedule, **When** `getFeeScheduleFileIdFor` is called with the corresponding realm and shard, **Then** the returned `FileId` is valid and is the ID of the corresponding fee schedule.
3. **Given** a network with a non-zero realm and/or shard exchange rates, **When** `getExchangeRatesFileIdFor` is called with the corresponding realm and shard, **Then** the returned `FileId` is valid and is the ID of the corresponding exchange rates.
4. **Given** a `Client` and a network with a non-zero realm and/or shard address book, **When** `forMirrorNetwork` is called by the `Client` with the corresponding realm and shard, **Then** the `Client` has the non-zero realm/shard nodes as a part of its network and can successfully submit transactions to them.
5. **Given** a `Client` and a network with a non-zero realm and/or shard address book, **When** `forMirrorNetwork` is called by the `Client` with no realm and shard, **Then** the `Client` has no nodes as a part of its network and cannot successfully submit transactions.

### TCK

Define TCK tests to verify:
- `get<STATIC_FILE>FileIdFor` functions properly construct `FileId` given the input `realm` and `shard` values.
- `forMirrorNetwork` properly initializes the `Client` with the contents of address book for the input `realm` and `shard`.
- `forMirrorNetwork` does not initialize the `Client` properly if no address book exists for the given `realm` and `shard`.
- `forMirrorNetwork` errors if input negative `realm` and `shard` values.

## SDK Example

An example may not be able to provided until a more established and public non-zero realm and/or shard network is provided for use. However, steps could be added to `ConstuctClientExample`:

```c++
// Read in the realm and shard of the address book to use to initialize the Client.
const int64_t realm = std::getenv("realm");
const int64_t shard = std::getenv("shard");

// Get the file ID of the address book.
const FileId addressBookFileId = FileId::getAddressBookFileIdFor(realm, shard);
std::cout << "Address book file ID: " << addressBookFileId.toString() << std::endl;

// Initialize the client with the mirror node URL and the input realm and shard.
const std::string mirrorNodeUrl = std::getenv("mirrorNodeUrl");
const Client client = Client::forMirrorNetwork({ mirrorNodeUrl }, realm, shard);

// Print the client network to prove the address book query was successful.
for (const auto& [urlAndPort, accountId]: client.getNetwork())
{
    std::cout << "URL and port: " << urlAndPort << ", AccountId: " << accountId.toString() << std::endl;
}
```

## Compatibility

- Fully backward compatible.
- Changes to `forMirrorNetwork` are not breaking, as the two proposed options allow for continued use of its previous functionality without the need for updating.
- Values for `realm` and `shard` of `0` for `getFileIdFor` functions should equal the hardcoded values the SDKs already provide.
- Users only attempting to use a non-zero realm/shard network are affected by these changes.

## Conclusion

These enhancements allow for users to now get the proper static file `FileId`s for commonly used files on a Hedera network. SDK functionality should work seamlessly for any value of `realm` or `shard` and these enhancements provide that robustness. 