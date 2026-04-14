# GRPC API

This section defines the GRPC API.

## Description

The communications protocol between Hiero SDKs and the network nodes is done based on GRPC and Protobuf.

```
namespace grpc

// Minimal placeholder to express SPI dependency.
// Concrete transport-layer details are language and runtime specific.
abstraction MethodDescriptor {
    @@immutable serviceName: string
    @@immutable methodName: string
}
```

## Questions & Comments