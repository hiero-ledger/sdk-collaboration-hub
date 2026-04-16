# Hiero Proto API

This section defines the Hiero Proto API.

## Description

The communications protocol between Hiero SDKs and the network nodes is done based on GRPC and Protobuf.
The Protobuf messages of Hiero are used in each SDK to generate the GRPC stubs.
Those stubs are language dependent and language specific tools are used to generate the stubs.

```
namespace hieroProto

// Minimal placeholders to make external dependencies explicit in this draft.
// Concrete protobuf fields are intentionally omitted and will be defined from hedera-protobufs.
abstraction TransactionBody {}
abstraction TransactionResponse {}
abstraction TransactionReceipt {}
abstraction TransactionRecord {}
```

## Questions & Comments