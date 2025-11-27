# Dynamic Network Version Detection and Feature Adaptation

**Issue**: #36 - Enable SDK to Detect Network Version and Adjust Logic Dynamically  
**Author**: Solution Proposal  
**Date**: November 26, 2025

## Problem Statement

The Hiero SDK currently faces challenges with feature rollout timing and backward compatibility:

1. **Delayed Feature Support**: Features like [HIP-1021](https://github.com/hiero-ledger/hips/blob/main/HIP-1021.md) and [HIP-1086](https://github.com/hiero-ledger/hips/blob/main/HIP-1086.md) had to be delayed until mainnet deployment, preventing early access on testnet/previewnet
2. **Backward Compatibility Issues**: Newer SDK versions can break compatibility with older consensus node versions
3. **Manual Version Management**: Developers must manually track which network versions support which features

## Current State Analysis

### Available Version Information

Based on the protobuf definitions, consensus nodes expose version information through:

- **`NetworkGetVersionInfoQuery`**: Returns semantic version information
- **`SemanticVersion`**: Contains major, minor, patch, and pre-release components
- **Two version types**:
  - `hapiProtoVersion`: Hedera API (HAPI) protobuf message version
  - `hederaServicesVersion`: Hedera Services software version

### Version Format
```protobuf
message SemanticVersion {
    int32 major = 1;    // Always 0 for Hedera (API may change)
    int32 minor = 2;    // Increments each release
    int32 patch = 3;    // Typical patch versioning
    string pre = 4;     // Pre-release identifier
}
```

**Important Note**: The semantic version directly corresponds to the [hiero-consensus-node](https://github.com/hiero-ledger/hiero-consensus-node) release version. For example, if a network is based on release [v0.68.2](https://github.com/hiero-ledger/hiero-consensus-node/releases/tag/v0.68.2), the semantic version would be `0.68.2`. Features are tied to consensus node releases, not Hedera-specific versions, ensuring compatibility across all networks based on Hiero consensus node.

## Proposed Solution Architecture

### 1. Version Detection Service

Create a centralized `NetworkVersionService` that:

```
namespace versionDetection
requires common

NetworkVersionService {
    @@async
    @@throws(network-error, timeout-error)
    SemanticVersion getHapiVersion()
    
    @@async
    @@throws(network-error, timeout-error)
    SemanticVersion getServicesVersion()
    
    @@async
    @@throws(network-error, timeout-error)
    NetworkCapabilities getNetworkCapabilities()
    
    bool isFeatureSupported(feature: Feature)
    VersionComparisonResult compareVersion(target: Version)
}

SemanticVersion {
    @@immutable major: int32
    @@immutable minor: int32
    @@immutable patch: int32
    @@immutable pre: string
    
    bool isGreaterThan(other: SemanticVersion)
    bool isLessThan(other: SemanticVersion)
    bool isEqual(other: SemanticVersion)
    string toString()
}

VersionComparisonResult {
    @@immutable comparison: ComparisonType
    @@immutable details: string
}

enum ComparisonType {
    GREATER_THAN
    EQUAL
    LESS_THAN
}
```

### 2. Feature Registry System

Define a `FeatureRegistry` that maps features to minimum required versions:

```
enum Feature {
    HIP_1021_AUTO_RENEW_ACCOUNTS
    HIP_1086_STAKING_REWARDS
    EVM_ADDRESS_ALIASES
    LAZY_CREATION
    SMART_CONTRACT_CONTRACT_CALL
    
    @@immutable minimumVersion: SemanticVersion
    @@immutable description: string
    @@immutable deprecationVersion: SemanticVersion
    @@immutable removalVersion: SemanticVersion
    
    SemanticVersion getMinimumVersion()
    string getDescription()
    bool isDeprecated()
    bool isRemoved()
}

FeatureRegistry {
    @@immutable features: map<string, Feature>
    
    @@nullable Feature getFeature(featureId: string)
    list<Feature> getSupportedFeatures(currentVersion: SemanticVersion)
    list<Feature> getDeprecatedFeatures(currentVersion: SemanticVersion)
    bool isFeatureSupported(feature: Feature, version: SemanticVersion)
}

NetworkCapabilities {
    @@immutable supportedFeatures: list<Feature>
    @@immutable networkVersion: SemanticVersion
    @@immutable lastUpdated: dateTime
    
    bool supportsFeature(feature: Feature)
    list<Feature> getUnsupportedFeatures(requiredFeatures: list<Feature>)
}
```

### 3. Feature Lifecycle Management

A comprehensive strategy for feature evolution and removal:

#### Feature Lifecycle Stages

1. **Introduction**: New features are added with explicit minimum version requirements
2. **Stable**: Features are fully supported and recommended for use
3. **Deprecated**: Features marked for future removal with warnings
4. **Removed**: Features no longer available in current SDK versions

#### Removal Strategy

- **Deprecated features**: Mark as deprecated for 2 major SDK versions before removal
- **Legacy features**: Keep indefinitely for backward compatibility if widely used
- **Experimental features**: Remove after 1 major version if not adopted
- **Security-related features**: Remove based on security policy timelines
- **Documentation**: Maintain a feature history matrix showing introduction/deprecation dates

#### Feature Lifecycle Example

```
Feature {
    HIP_1021_AUTO_RENEW_ACCOUNTS {
        minimumVersion: SemanticVersion(0, 38, 0)
        deprecationVersion: SemanticVersion(0, 50, 0)  // Future
        removalVersion: SemanticVersion(0, 52, 0)     // Future
        status: STABLE
    }
}
```

### 4. Dynamic Feature Enablement

Implement conditional feature logic based on detected network version:

```java
public class ConditionalTransactionExecutor {
    public void executeWithVersionCheck(Transaction tx, Client client) {
        NetworkVersionService versionService = client.getVersionService();
        
        if (versionService.isFeatureSupported(Feature.HIP_1021_AUTO_RENEW_ACCOUNTS)) {
            // Use new HIP-1021 logic
            executeWithAutoRenewSupport(tx, client);
        } else {
            // Fallback to legacy logic
            executeLegacyAutoRenew(tx, client);
        }
    }
}
```

### 5. Version-Aware Client Configuration

Enhance the client configuration to include version detection:

```
VersionAwareClient {
    @@immutable versionService: NetworkVersionService
    @@immutable featureRegistry: FeatureRegistry
    @@immutable cachedCapabilities: AtomicReference<NetworkCapabilities>
    
    @@async
    @@throws(network-error, timeout-error)
    void initialize()
    
    void refreshCapabilities()
    bool isFeatureSupported(feature: Feature)
    NetworkCapabilities getCurrentCapabilities()
}
```

**Note**: The `initialize()` method should be internal rather than public. Version detection should happen automatically during client construction or first use. The public API should focus on feature queries and version comparisons.

## Implementation Strategy

### Phase 1: Core Infrastructure

1. **Create Version Service Classes**
   - `NetworkVersionService` interface and implementation
   - `SemanticVersion` utility class for comparisons
   - `NetworkCapabilities` data structure

2. **Implement Version Detection**
   - Add `NetworkGetVersionInfoQuery` support to all SDKs
   - Cache version information to avoid repeated queries
   - Handle network failures gracefully

### Phase 2: Feature Registry and Runtime Updates

1. **Define Feature Enum**
   - Map HIPs to minimum required versions
   - Include feature descriptions and compatibility notes
   - Add deprecation and removal version tracking

2. **Create Version Comparison Logic**
   - Semantic version comparison utilities
   - Pre-release version handling
   - Range-based version checks

3. **Runtime Update Handling**
   - Version cache TTL with periodic refresh (e.g., 5 minutes)
   - Event-driven updates for network upgrades
   - Graceful adaptation to new feature sets
   - Configuration options to disable auto-updates for stability-critical applications
   - Fallback mechanisms during transitions

### Phase 3: Early Backward Compatibility

1. **Graceful Degradation** (moved from Phase 4)
   - Provide fallback implementations for unsupported features
   - Clear error messages when features are unavailable
   - Optional feature usage with warnings

2. **Version Warnings** (moved from Phase 4)
   - Warn developers when using older network versions
   - Suggest SDK upgrades for full feature support
   - Document version compatibility matrix

### Phase 4: Dynamic Feature Integration

1. **HIP-1021 Integration**
   ```java
   public class AccountCreateTransaction {
       public AccountCreateTransaction withKey(Key key) {
           if (versionService.isFeatureSupported(Feature.HIP_1021_AUTO_RENEW_ACCOUNTS)) {
               return withKeyAndAlias(key);
           } else {
               return withKeyLegacy(key);
           }
       }
   }
   ```

2. **HIP-1086 Integration**
   ```java
   public class CryptoStakeUpdater {
       public void updateStakeInfo(Client client) {
           if (versionService.isFeatureSupported(Feature.HIP_1086_STAKING_REWARDS)) {
               processStakingRewardsWithEnhancements(client);
           } else {
               processStakingRewardsLegacy(client);
           }
       }
   }
   ```

### Phase 5: Advanced Features and Optimization

1. **Advanced Feature Integration**
   - Complex conditional logic for multiple features
   - Feature interaction handling
   - Performance optimizations for version checks

2. **Edge Cases and Error Handling**
   - Network partition scenarios
   - Version spoofing protection
   - Comprehensive error reporting

## SDK Version Compatibility Strategy

### SDK v2.0.0 (Breaking Changes)
- Introduce version detection as default behavior
- Update all feature implementations to be version-aware
- Deprecate legacy version-specific methods
- Require language-agnostic API documentation

### SDK v1.x (Backward Compatible)
- **Depends on maintenance policy**: If v1.x is still maintained, add version detection as opt-in feature
- Maintain existing APIs alongside version-aware alternatives
- Provide migration guides and warnings
- If v1.x is frozen, focus development on v2.0 implementation

## Configuration Options

### Automatic Mode (Default)
```java
Client client = Client.forTestnet()
    .enableAutomaticVersionDetection(true)
    .setFeatureFallbackEnabled(true);
```

### Manual Mode
```java
Client client = Client.forTestnet()
    .setNetworkVersion(SemanticVersion.of(0, 38, 0))
    .enableFeatures(Feature.HIP_1021_AUTO_RENEW_ACCOUNTS);
```

### Disabled Mode
```java
Client client = Client.forTestnet()
    .enableAutomaticVersionDetection(false)
    .setCompatibilityMode(CompatibilityMode.LEGACY);
```

## Benefits

### For Developers
1. **Early Access**: Use new features on testnet/previewnet immediately
2. **Backward Compatibility**: New SDK versions work with older networks
3. **Clear Error Messages**: Understand why features aren't available
4. **Automatic Adaptation**: No manual version tracking required

### For Network Operators
1. **Gradual Rollouts**: Deploy features incrementally across networks
2. **Testing**: New features can be tested on previewnet before mainnet
3. **Compatibility**: Support mixed-version network environments

### For SDK Maintainers
1. **Simplified Releases**: No need to time releases with network deployments
2. **Reduced Forks**: Single codebase supports multiple network versions
3. **Easier Testing**: Test against multiple network versions

## Migration Strategy

### SDK v2.0.0 (Breaking Changes)
- Introduce version detection as default behavior
- Update all feature implementations to be version-aware
- Deprecate legacy version-specific methods

### SDK v1.x (Backward Compatible)
- Add version detection as opt-in feature
- Maintain existing APIs alongside version-aware alternatives
- Provide migration guides and warnings

## Performance Considerations

### Caching Strategy
- Cache version information for client lifetime
- Implement TTL-based cache invalidation
- Allow manual cache refresh when needed

### Network Efficiency
- Batch version queries with other operations
- Use cached version information across transactions
- Minimize version query frequency

### Fallback Behavior
- Graceful degradation when version detection fails
- Local version cache as fallback
- Configurable timeout and retry policies

## Security Considerations

### Version Spoofing Protection
- Validate version information from multiple nodes
- Detect version inconsistencies across network
- Implement version integrity checks

### Feature Access Control
- Only enable features when minimum requirements are met
- Prevent feature bypass attempts
- Log version detection events for auditing

## Testing Strategy

### Unit Tests
- Version comparison logic
- Feature registry validation
- Conditional behavior testing

### Integration Tests
- Multi-version network testing
- Feature availability scenarios
- Backward compatibility validation

### End-to-End Tests
- Real network version detection
- Feature enablement workflows
- Error handling scenarios

## Documentation Requirements

### API Documentation
- Version detection configuration options
- Feature registry and compatibility matrix
- Migration guides for each major version

### Developer Guide
- How to use version-aware features
- Best practices for multi-network support
- Troubleshooting version detection issues

### Release Notes
- Version detection feature announcements
- Network compatibility updates
- Migration requirements and timelines

## Conclusion

This solution provides a robust foundation for dynamic network version detection and feature adaptation. It addresses the core issues of delayed feature support and backward compatibility while providing a clear migration path for existing applications.

The phased implementation approach ensures minimal disruption while delivering immediate value to developers and network operators.

## Next Steps

1. **Prototype Implementation**: Create proof-of-concept for version detection
2. **Community Review**: Gather feedback on proposed architecture
3. **HIP Submission**: Formalize the approach as a Hiero Improvement Proposal
4. **Implementation Planning**: Create detailed implementation roadmap
5. **Testing Strategy**: Develop comprehensive test suite

This approach will enable the Hiero SDK to dynamically adapt to network capabilities, providing a better experience for developers and supporting the continued evolution of the Hedera ecosystem.
