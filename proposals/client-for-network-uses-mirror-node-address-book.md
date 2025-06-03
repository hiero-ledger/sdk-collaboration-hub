# Client Initialization for Static Networks Uses Address Book Mirror Node Query

**Date Submitted:** 2025-05-21

## Summary

This proposal describes a change in client network initialization for static networks (e.g. Hedera mainnet). Currently, the Hiero SDKs read a static file (or hardcoded values) that describe the IPs, ports, and account IDs of the nodes running the network. The SDKs will then wait roughly ten seconds, then query the network mirror node to grab the most up-to-date address book. This can be problematic for a couple of reasons:

- Network updates require an updated file from which the SDKs read.
- The first ten seconds of operation could use an outdated network, resulting in execution errors.

This proposal suggests removing this method of static network initialization and replacing it with a mirror node query. This will prevent needing to manually update the SDKs with new networks, as well as prevent invalid node errors if attempting execution within the first ten seconds of operation.

## Internal Changes

This proposal doesn't add or update any APIs, it just changes how the Client network is initialized for static networks. Instead of reading from a static file or using hardcoded values, the SDKs should instead query the mirror node for the most up-to-date address book immediately.

```c++
Client Client::forMainnet() {
    Client client;
    client.mImpl->setMirrorNetwork(internal::MirrorNetwork::forMainnet());
    client.mImpl->mNetwork =
        std::make_shared<internal::Network>(
            internal::Network::forNetwork(
                internal::Network::getNetworkFromAddressBook(
                    AddressBookQuery().execute(client),
                    internal::BaseNodeAddress::PORT_NODE_PLAIN)));
    return client;    
}
```

The implementation will still require some sort way to read the mirror node network. However, the SDKs already do this so nothing needs to change there. The SDKs should use the established mirror node network to retrieve the address book for the consensus node network, and use the address book to initialize its consensus node network.

With this update, the ten second scheduled update for Hiero SDK networks can be removed. Since the SDK networks will be initialized from the start with the most up-to-date address book information, there is no longer a good reason to do another one-time update ten seconds later.

---

## Test Plan

1. **Given** an uninitialized Client,  
   **When** its initialized with a static network,  
   **Then** the network correctly matches the network address book.

---

## Compatibility

- Fully backward compatible.
- No changes to existing APIs or behavior.
- Works seamlessly with all static network Client initializations.

## Conclusion

This enhancement allows users to always have the correct network when using a static network, without having to worry about updating SDK versions or having to wait ten seconds to get the correct node addresses. This will provide a more seamless, less error-prone experience for Hiero developers.
