# GRPC API

This section defines the GRPC API.

## Description

The communications protocol between Hiero SDKs and the network nodes is done based on gRPC and Protobuf.

`MethodDescriptor` identifies a gRPC method by its fully-qualified service name and method name. It is
defined here as a portable value type with no dependency on any language's gRPC runtime library. The
binding to the language-specific gRPC channel happens inside the SDK transport layer, not in the public API.

This means PoC authors must NOT use `io.grpc.MethodDescriptor` (Java), `grpc.ServiceRpc` (Python), or
any equivalent runtime type in the public SPI signature. They must accept and return this custom type,
then map it to the runtime type internally.

```
namespace grpc

// Cross-language value type identifying a gRPC method.
// Does not depend on any language's gRPC runtime — the runtime binding is an internal detail.
abstraction MethodDescriptor {
    @@immutable serviceName: string   // fully-qualified gRPC service name (e.g. "proto.CryptoService")
    @@immutable methodName: string    // gRPC method name within the service (e.g. "createAccount")
}
```

## Questions & Comments

- [@hendrikebbers](https://github.com/hendrikebbers): Is `MethodDescriptor` specific for Java or is it the same for all languages?
  Can we provide all information needed for `MethodDescriptor` in a custom complex type and by doing so remove the dependency to `grpc` in the public API?

  **Decision:** `MethodDescriptor` is cross-language. The type above is the complete public representation —
  `serviceName` and `methodName` strings carry all information needed to construct the language-specific
  gRPC descriptor internally. No SDK should expose its gRPC runtime type in the public SPI signature.