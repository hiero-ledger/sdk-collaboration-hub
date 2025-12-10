# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Dynamic network version detection implementation for issue #36:
  - `SemanticVersion` record for representing semantic versions with comparison support
  - `NetworkVersionInfo` record containing HAPI protobuf version and Hedera Services version
  - `Feature` enum defining network features and their minimum required versions
  - `HieroClient.getNetworkVersionInfo()` method to query network version from consensus nodes
  - `HieroClient.isFeatureSupported(Feature)` method to check feature availability
  - Automatic version caching with 5-minute TTL to handle long-running clients
  - Thread-safe implementation with double-checked locking
  - Support for features: HIP-1021, HIP-1086, EVM address aliases, lazy creation, and contract call improvements

### Changed

### Fixed

### Deprecated

### Removed

### Documentation
- Added commit signing requirements documentation in `docs/sdk_developers/signing.md` [#82bd7f0](https://github.com/hiero-ledger/sdk-collaboration-hub/commit/82bd7f0b459fb4fd9e8073a2dca8db66bff5f03f)
