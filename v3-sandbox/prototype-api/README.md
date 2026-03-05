# Prototype API

This file defines the API for the prototype in the format defined at our [api-guideline](../../guides/api-guideline.md).

## APIs

- [Common API](common.md)
- [Configuration API](config.md)
- [Key API](keys.md)
- [Client API](client.md)
- [Requests API](requests.md) — Request hierarchy, contracts, and all request/query/subscription types
- [Requests SPI API](requests-spi.md) — Internal execution loop and SPI methods
- [Transactions API](transactions.md) — Transaction builder, PackedTransaction, and response types (integrates with Requests API)
- [Transactions SPI API](transactions-spi.md) — TransactionSupport data-layer SPI (integrates with Requests SPI)

Every SDK will depend in its public API on [our protobuf definitions](hiero-proto.md) and [GRPC](grpc.md).