# Client JSON Configuration Example

This document provides an example JSON configuration that can be used with `Client.fromConfig()` across the Hiero SDKs. The configuration defines all necessary parameters to connect to and interact with a Hedera network.

## Example Configuration

```json
{
  "network": {
    "35.242.233.154:50211": "3.5.10",
    "35.240.118.96:50211": "3.5.11",
    "34.127.45.23:50211": "3.5.12"
  },
  "mirrorNetwork": [
    "hcs.testnet.mirrornode.hedera.com:443"
  ],
  "shard": 0,
  "realm": 0,
  "grpcDeadline": 10000,
  "requestTimeout": 120000
}
```

## Configuration Parameters

### Network Configuration

- **`network`** (object, required): Maps consensus node addresses to their corresponding account IDs
  - **Key format**: `"IP:PORT"` (e.g., `"35.242.233.154:50211"`)
  - **Value format**: `"SHARD.REAL.ACCOUNT"` account ID (e.g., `"0.0.3"`)
  - **Purpose**: Defines the consensus nodes that the client will connect to for transaction submission

- **`mirrorNetwork`** (array or string, optional): Mirror node endpoints for query operations
  - **Array format**: `["hcs.testnet.mirrornode.hedera.com:443", "backup.mirror.com:443"]`
  - **String format**: `"hcs.testnet.mirrornode.hedera.com:443"` (single node)
  - **Purpose**: Defines mirror nodes for account queries, transaction records, and other read operations

### Network Identification

- **`shard`** (number, optional): The shard number for the network
  - **Default**: `0` for mainnet/testnet
  - **Purpose**: Identifies which shard the client operates on

- **`realm`** (number, optional): The realm number for the network  
  - **Default**: `0` for mainnet/testnet
  - **Purpose**: Identifies which realm the client operates on

### Timeout Configuration

- **`grpcDeadline`** (number, optional): Maximum duration in milliseconds for individual gRPC requests
  - **Default**: `10000` (10 seconds)
  - **Range**: Must be a positive integer greater than 0
  - **Purpose**: Controls timeout for each individual network request to consensus nodes

- **`requestTimeout`** (number, optional): Maximum duration in milliseconds for complete transaction/query execution
  - **Default**: `120000` (2 minutes)
  - **Range**: Must be a positive integer greater than 0, should be ≥ `grpcDeadline`
  - **Purpose**: Controls overall timeout including retries, backoff, and node rotation

## Network-Specific Examples

### Mainnet Configuration
```json
{
  "network": {
    "35.237.200.180:50211": "0.0.1",
    "35.236.5.219:50211": "0.0.2",
    "35.185.196.93:50211": "0.0.3"
  },
  "mirrorNetwork": [
    "mainnet-public.mirrornode.hedera.com:443"
  ],
  "shard": 0,
  "realm": 0
}
```

### Testnet Configuration
```json
{
  "network": {
    "35.242.233.154:50211": "0.0.3",
    "35.240.118.96:50211": "0.0.4",
    "34.127.45.23:50211": "0.0.5"
  },
  "mirrorNetwork": [
    "hcs.testnet.mirrornode.hedera.com:443"
  ],
  "shard": 0,
  "realm": 0,
  "grpcDeadline": 8000,
  "requestTimeout": 60000
}
```

### Previewnet Configuration
```json
{
  "network": {
    "35.236.10.137:50211": "0.0.6",
    "35.227.193.111:50211": "0.0.7"
  },
  "mirrorNetwork": [
    "hcs.previewnet.mirrornode.hedera.com:443"
  ],
  "shard": 0,
  "realm": 0
}
```

## Usage Examples

### JavaScript/TypeScript
```typescript
import { Client } from '@hashgraph/sdk';

const client = Client.fromConfig({
  network: {
    "35.242.233.154:50211": "0.0.3",
    "35.240.118.96:50211": "0.0.4"
  },
  mirrorNetwork: ["hcs.testnet.mirrornode.hedera.com:443"],
  shard: 0,
  realm: 0,
  grpcDeadline: 10000,
  requestTimeout: 120000
});
```

### Java
```java
import com.hedera.hashgraph.sdk.Client;
import java.util.Map;

Map<String, String> network = Map.of(
    "35.242.233.154:50211", "0.0.3",
    "35.240.118.96:50211", "0.0.4"
);

Client client = Client.fromConfig(Map.of(
    "network", network,
    "mirrorNetwork", "hcs.testnet.mirrornode.hedera.com:443",
    "shard", 0,
    "realm", 0,
    "grpcDeadline", 10000,
    "requestTimeout", 120000
));
```

### Go
```go
import "github.com/hashgraph/hedera-sdk-go/v2"

client, err := hedera.ClientFromConfig(map[string]interface{}{
    "network": map[string]string{
        "35.242.233.154:50211": "0.0.3",
        "35.240.118.96:50211": "0.0.4",
    },
    "mirrorNetwork": []string{"hcs.testnet.mirrornode.hedera.com:443"},
    "shard": 0,
    "realm": 0,
    "grpcDeadline": 10000,
    "requestTimeout": 120000,
})
```

## Best Practices

1. **Use multiple consensus nodes** for better reliability and load balancing
2. **Set appropriate timeouts** based on your network conditions and use case
3. **Ensure `requestTimeout` ≥ `grpcDeadline`** to maintain logical consistency
4. **Use network-specific configurations** for mainnet, testnet, and previewnet
5. **Test connection** before production use to verify all nodes are accessible
6. **Monitor node health** and update configurations when nodes become unavailable

## Notes

- All parameters are optional except `network`
- Default values will be used for optional parameters not specified
- The SDK assumes shard and realm values are correct when provided
- Network configurations may change over time, so keep them updated
