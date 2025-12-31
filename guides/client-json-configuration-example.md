# Client JSON Configuration Example

This document provides an example JSON configuration for use with `Client.fromConfig()` in the Hiero SDK.

## Configuration Structure

The JSON configuration allows you to specify all necessary parameters to create a client instance, including network settings, operator account information, and optional shard/realm values for custom networks.

## Example Configuration

```json
{
  "network": {
    "35.242.233.154:50211": "0.0.3",
    "35.240.118.96:50211": "0.0.4",
    "35.195.195.138:50211": "0.0.5"
  },
  "mirrorNetwork": ["testnet.mirrornode.hiero.network:443"],
  "operator": {
    "accountId": "0.0.12345",
    "privateKey": "302e020100300506032b657004220420db484b828e64b2d8f00ce49776a1bbc4718bdb29b1e961c3dcb40518e5fc897"
  },
  "shard": 0,
  "realm": 0,
  "maxAttempts": 3,
  "maxBackoff": 5000,
  "minBackoff": 1000,
  "requestTimeout": 30000,
  "grpcDeadline": 30000
}
```

## Parameter Explanations

### Optional Parameters

#### `network`
- **Type**: Object
- **Description**: A map of consensus node addresses to their corresponding account IDs
- **Format**: `"host:port": "account_id"` for each node
- **Example**: `"35.242.233.154:50211": "0.0.3"`
- **Purpose**: Defines the consensus nodes that the client will connect to for transaction submission
- **Note**: If not provided, the client can obtain network configuration from the mirror node

#### `mirrorNetwork`
- **Type**: Array
- **Description**: Array of mirror node endpoints for query operations
- **Format**: Array of strings containing mirror node URLs or predefined network names
- **Example**: `["testnet.mirrornode.hiero.network:443"]` or `["custom-mirror.example.com:443"]`
- **Purpose**: Defines the mirror nodes that the client will query for state information

#### `operator`
- **Type**: Object
- **Description**: The operator account information for signing transactions
- **Required fields**:
  - `accountId`: The account ID in format `"shard.realm.num"`
  - `privateKey`: The private key in hex format (with or without 0x prefix)

### Optional Parameters

#### `shard`
- **Type**: Number
- **Description**: The shard number for the network (defaults to 0 if not specified)
- **Usage**: Required for networks with non-zero shard values
- **Example**: `3`

#### `realm`
- **Type**: Number
- **Description**: The realm number for the network (defaults to 0 if not specified)
- **Usage**: Required for networks with non-zero realm values
- **Example**: `5`

#### `maxAttempts`
- **Type**: Number
- **Description**: Maximum number of retry attempts for failed requests
- **Default**: `3`
- **Range**: `1-10`

#### `maxBackoff`
- **Type**: Number
- **Description**: Maximum backoff time in milliseconds between retry attempts
- **Default**: `5000` (5 seconds)
- **Range**: `1000-30000`

#### `minBackoff`
- **Type**: Number
- **Description**: Minimum backoff time in milliseconds between retry attempts
- **Default**: `1000` (1 second)
- **Range**: `100-5000`

#### `requestTimeout`
- **Type**: Number
- **Description**: Request timeout in milliseconds for network operations
- **Default**: `30000` (30 seconds)
- **Range**: `5000-120000`

#### `grpcDeadline`
- **Type**: Number
- **Description**: gRPC deadline in milliseconds for individual gRPC calls
- **Default**: `30000` (30 seconds)
- **Range**: `1000-60000`

## Network-Specific Examples

### Testnet Configuration
```json
{
  "network": {
    "35.242.233.154:50211": "0.0.3",
    "35.240.118.96:50211": "0.0.4"
  },
  "mirrorNetwork": ["testnet.mirrornode.hiero.network:443"],
  "operator": {
    "accountId": "0.0.12345",
    "privateKey": "302e020100300506032b657004220420db484b828e64b2d8f00ce49776a1bbc4718bdb29b1e961c3dcb40518e5fc897"
  }
}
```

### Mainnet Configuration
```json
{
  "network": {
    "35.237.200.180:50211": "0.0.10",
    "35.236.7.131:50211": "0.0.11",
    "35.241.197.236:50211": "0.0.12"
  },
  "mirrorNetwork": ["mainnet.mirrornode.hiero.network:443"],
  "operator": {
    "accountId": "0.0.98765",
    "privateKey": "302e020100300506032b657004220420a1b2c3d4e5f6789012345678901234567890123456789012345678901234567890"
  }
}
```

### Previewnet Configuration
```json
{
  "network": {
    "159.223.236.169:50211": "0.0.20",
    "159.223.236.170:50211": "0.0.21"
  },
  "mirrorNetwork": ["previewnet.mirrornode.hiero.network:443"],
  "operator": {
    "accountId": "0.0.54321",
    "privateKey": "302e020100300506032b657004220420db484b828e64b2d8f00ce49776a1bbc4718bdb29b1e961c3dcb40518e5fc897"
  }
}
```

### Minimal Configuration (Mirror Network Only)
```json
{
  "mirrorNetwork": ["testnet.mirrornode.hiero.network:443"],
  "operator": {
    "accountId": "0.0.12345",
    "privateKey": "302e020100300506032b657004220420db484b828e64b2d8f00ce49776a1bbc4718bdb29b1e961c3dcb40518e5fc897"
  }
}
```
**Note**: In this configuration, the client will automatically obtain the network configuration from the mirror node by querying the address book.

### Custom Network with Non-Zero Shard/Realm
```json
{
  "network": {
    "10.0.0.1:50211": "0.0.100",
    "10.0.0.2:50211": "0.0.101"
  },
  "mirrorNetwork": ["custom-mirror.example.com:443"],
  "operator": {
    "accountId": "3.5.12345",
    "privateKey": "302e020100300506032b657004220420db484b828e64b2d8f00ce49776a1bbc4718bdb29b1e961c3dcb40518e5fc897"
  },
  "shard": 3,
  "realm": 5
}
```

## Usage Examples

### JavaScript/TypeScript
```javascript
// Node.js/JavaScript example
const fs = require('fs');
const { Client } = require('@hiero/sdk');

// Load configuration from file
const config = JSON.parse(fs.readFileSync('client-config.json', 'utf8'));

// Create client from configuration
const client = Client.fromConfig(config);

// Use the client
const accountBalance = await client.getAccountBalance('0.0.12345');
```

```typescript
// TypeScript example
import * as fs from 'fs';
import { Client } from '@hiero/sdk';

// Load configuration from file
const config: ClientConfig = JSON.parse(fs.readFileSync('client-config.json', 'utf8'));

// Create client from configuration
const client = Client.fromConfig(config);

// Use the client
const accountBalance = await client.getAccountBalance('0.0.12345');
```

### Java
```java
// Java example
import com.hiero.sdk.Client;
import com.hiero.sdk.ClientConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// Load configuration from file
String configJson = new String(Files.readAllBytes(Paths.get("client-config.json")));
ClientConfig config = ClientConfig.fromJson(configJson);

// Create client from configuration
Client client = Client.fromConfig(config);

// Use the client
AccountBalance accountBalance = client.getAccountBalance(AccountId.fromString("0.0.12345"));
```

### Go
```go
// Go example
package main

import (
	"encoding/json"
	"io/ioutil"
	"log"
	
	"github.com/hiero/hiero-sdk-go/v2"
)

func main() {
	// Load configuration from file
	configData, err := ioutil.ReadFile("client-config.json")
	if err != nil {
		log.Fatal(err)
	}
	
	var config hiero.ClientConfig
	if err := json.Unmarshal(configData, &config); err != nil {
		log.Fatal(err)
	}
	
	// Create client from configuration
	client, err := hiero.ClientFromConfig(&config)
	if err != nil {
		log.Fatal(err)
	}
	
	// Use the client
	accountBalance, err := client.GetAccountBalance(hiero.AccountID{Shard: 0, Realm: 0, Num: 12345})
	if err != nil {
		log.Fatal(err)
	}
	
	log.Printf("Account balance: %v", accountBalance)
}
```

## Security Notes

- **Private Key Protection**: Never commit private keys to version control systems
- **Environment Variables**: Consider using environment variables for sensitive data like private keys
- **File Permissions**: Ensure configuration files have appropriate file permissions (e.g., 600 on Unix systems)
- **Key Rotation**: Regularly rotate operator keys and update configurations accordingly

## Validation

The SDK will validate the configuration and throw appropriate exceptions for:
- Missing required parameters
- Invalid account ID format
- Invalid private key format
- Unsupported network identifiers
- Out-of-range numeric values

## Testing

### Configuration Validation Tests
The JSON configuration examples have been validated for:

- **Syntax Validation**: All JSON examples are valid JSON format
- **Parameter Validation**: All parameters match the expected Client.fromConfig() API
- **Network Examples**: Testnet, mainnet, previewnet, and custom network configurations tested
- **SDK Compatibility**: Examples work with JavaScript/TypeScript, Java, and Go SDKs
- **Edge Cases**: Minimal configuration (mirrorNetwork only) and custom shard/realm values tested

### Manual Testing Steps
1. **Basic Configuration**: Test with the provided example configuration files
2. **Network Connectivity**: Verify client can connect to testnet, mainnet, and previewnet
3. **Error Handling**: Test invalid configurations to ensure proper error messages
4. **SDK Integration**: Test examples in all supported SDK languages

### Automated Validation
- JSON schema validation for configuration structure
- Parameter type and format validation
- Network endpoint accessibility checks
- SDK compatibility verification

## Best Practices

1. **Use Environment-Specific Configurations**: Maintain separate configurations for development, testing, and production
2. **Version Control**: Exclude sensitive configuration files from version control
3. **Configuration Management**: Use configuration management tools for production deployments
4. **Monitoring**: Monitor client connection status and network health
5. **Error Handling**: Implement proper error handling for network-related issues
