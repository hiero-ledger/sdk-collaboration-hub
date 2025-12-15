# Client JSON Configuration Example

This document provides an example JSON configuration for use with `Client.fromConfig()` in the Hiero SDK.

## Configuration Structure

The JSON configuration allows you to specify all necessary parameters to create a client instance, including network settings, operator account information, and optional shard/realm values for custom networks.

## Example Configuration

```json
{
  "network": {
    "35.242.233.154:50211": "3.5.10",
    "35.240.118.96:50211": "3.5.11",
    "35.195.195.138:50211": "3.5.9"
  },
  "mirrorNetwork": "testnet",
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

### Required Parameters

#### `network`
- **Type**: Object
- **Description**: A map of consensus node addresses to their version numbers
- **Format**: `"host:port": "version"` for each node
- **Example**: `"35.242.233.154:50211": "3.5.10"`

#### `mirrorNetwork`
- **Type**: String
- **Description**: The mirror network identifier or URL
- **Values**: Predefined networks like `"testnet"`, `"mainnet"`, `"previewnet"` or custom mirror node URLs
- **Example**: `"testnet"`

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
    "35.242.233.154:50211": "3.5.10",
    "35.240.118.96:50211": "3.5.11"
  },
  "mirrorNetwork": "testnet",
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
    "35.237.200.180:50211": "1.8.2",
    "35.236.7.131:50211": "1.8.2",
    "35.241.197.236:50211": "1.8.2"
  },
  "mirrorNetwork": "mainnet",
  "operator": {
    "accountId": "0.0.98765",
    "privateKey": "302e020100300506032b657004220420a1b2c3d4e5f6789012345678901234567890123456789012345678901234567890"
  }
}
```

### Custom Network with Non-Zero Shard/Realm
```json
{
  "network": {
    "10.0.0.1:50211": "2.0.1",
    "10.0.0.2:50211": "2.0.1"
  },
  "mirrorNetwork": "https://custom-mirror.example.com",
  "operator": {
    "accountId": "3.5.12345",
    "privateKey": "302e020100300506032b657004220420db484b828e64b2d8f00ce49776a1bbc4718bdb29b1e961c3dcb40518e5fc897"
  },
  "shard": 3,
  "realm": 5
}
```

## Usage Example

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

```python
# Python example
import json
from hiero_sdk import Client

# Load configuration from file
with open('client-config.json', 'r') as f:
    config = json.load(f)

# Create client from configuration
client = Client.from_config(config)

# Use the client
account_balance = await client.get_account_balance('0.0.12345')
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

## Best Practices

1. **Use Environment-Specific Configurations**: Maintain separate configurations for development, testing, and production
2. **Version Control**: Exclude sensitive configuration files from version control
3. **Configuration Management**: Use configuration management tools for production deployments
4. **Monitoring**: Monitor client connection status and network health
5. **Error Handling**: Implement proper error handling for network-related issues
