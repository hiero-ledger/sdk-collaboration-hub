# Client JSON Configuration Example

This document provides example JSON configurations a user can input into `Client.fromConfig()`.

## Example: Named Network (Cross-SDK Friendly)

```json
{
  "network": "testnet",
  "mirrorNetwork": ["testnet.mirrornode.hedera.com:443"],
  "operator": {
    "accountId": "0.0.1001",
    "privateKey": "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10"
  }
}
```

## Example: Custom Network Object (JavaScript/TypeScript and Go)

For JS/TS and Go SDKs, the object format is:

`"host:port": "accountId"`

```json
{
  "network": {
    "35.237.200.180:50211": "0.0.3",
    "35.186.191.247:50211": "0.0.4"
  },
  "mirrorNetwork": ["testnet.mirrornode.hedera.com:443"],
  "operator": {
    "accountId": "0.0.1001",
    "privateKey": "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10"
  }
}
```

## Example: Custom Network Object (Java)

For Java SDK, the object format is:

`"accountId": "host:port"`

```json
{
  "network": {
    "0.0.3": "0.testnet.hedera.com:50211",
    "0.0.4": "1.testnet.hedera.com:50211"
  },
  "networkName": "testnet",
  "mirrorNetwork": ["testnet.mirrornode.hedera.com:443"],
  "operator": {
    "accountId": "0.0.1001",
    "privateKey": "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10"
  },
  "shard": 0,
  "realm": 0
}
```

## Parameter Reference

| Parameter | Type | Description |
| --- | --- | --- |
| `network` | `string` or `object` | Network name (`mainnet`, `testnet`, `previewnet`) or custom network object. Required in Java and Go SDKs. |
| `mirrorNetwork` | `string` or `string[]` | Mirror network name or mirror endpoint list. |
| `operator` | `object` | Optional operator configuration object. |
| `operator.accountId` | `string` | Operator account ID (for example, `0.0.1001`). |
| `operator.privateKey` | `string` | Operator private key string in a supported SDK format. |
| `shard` | `number` | Optional shard value for non-zero shard networks. |
| `realm` | `number` | Optional realm value for non-zero realm networks. |
| `networkName` | `string` | Optional Java SDK field used with object network config (`mainnet`, `testnet`, `previewnet`). |
| `scheduleNetworkUpdate` | `boolean` | Optional JS/TS field to disable/enable periodic address book update. |
| `grpcDeadline` | `number` | Optional JS/TS field for per-request gRPC deadline in milliseconds. |
| `requestTimeout` | `number` | Optional JS/TS field for full operation timeout in milliseconds. |

## Notes

- Use SDK-native examples for your language if you are using a custom `network` object.
- Use `shard` and `realm` only with custom object `network` configs. In Java, these fields are ignored when `network` is a named string.
- For JS/TS, keep `requestTimeout` greater than `grpcDeadline`.
- Do not commit real private keys in repository files.
