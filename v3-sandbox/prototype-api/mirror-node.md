# Mirror Node Query API

## Description

This API defines the types and repository abstractions for querying a Hiero Mirror Node via its REST API.
It covers accounts, tokens, NFTs, transactions, topics, contracts, and network-level data.
All domain models are read-only DTOs (`@@finalType` with `@@immutable` fields) returned by the REST API.

## API Schema

```
namespace mirrorNode
requires common

// Additional ID types (should ultimately live in common)
TokenId extends Address {
}

TopicId extends Address {
}

ContractId extends Address {
}

// Paginated result set mapping to the REST API links.next cursor pattern
abstraction Page<$$T> {
    @@immutable data: list<$$T>
    @@immutable size: int32
    @@immutable pageIndex: int32

    bool hasNext()
    bool isFirst()

    @@async
    @@throws(mirror-node-error)
    Page<$$T> next()

    @@async
    @@throws(mirror-node-error)
    Page<$$T> first()
}

enum TokenType {
    FUNGIBLE_COMMON
    NON_FUNGIBLE_UNIQUE
}

enum TokenSupplyType {
    INFINITE
    FINITE
}

enum TransactionResult {
    SUCCESS
    FAIL
}

enum BalanceModification {
    CREDIT
    DEBIT
}

// All known transaction types. Each carries a protocolName matching the REST API wire value.
enum TransactionType {
    ACCOUNT_CREATE
    ACCOUNT_DELETE
    ACCOUNT_UPDATE
    CRYPTO_TRANSFER
    TOPIC_CREATE
    TOPIC_MESSAGE_SUBMIT
    TOKEN_CREATE
    TOKEN_MINT
    TOKEN_BURN
    TOKEN_TRANSFER
    CONTRACT_CREATE
    CONTRACT_CALL
    ETHEREUM
    ...              // full list derived from the Mirror Node OpenAPI spec
    UNKNOWN

    @@immutable protocolName: string
}

@@finalType
FixedFee {
    @@immutable amount: int64
    @@immutable @@nullable collectorAccountId: AccountId
    @@immutable @@nullable denominatingTokenId: TokenId
}

@@finalType
FractionalFee {
    @@immutable numeratorAmount: int64
    @@immutable denominatorAmount: int64
    @@immutable @@nullable collectorAccountId: AccountId
    @@immutable @@nullable denominatingTokenId: TokenId
}

@@finalType
RoyaltyFee {
    @@immutable numeratorAmount: int64
    @@immutable denominatorAmount: int64
    @@immutable fallbackFeeAmount: int64
    @@immutable @@nullable collectorAccountId: AccountId
    @@immutable @@nullable denominatingTokenId: TokenId
}

@@finalType
CustomFee {
    @@immutable fixedFees: list<FixedFee>
    @@immutable fractionalFees: list<FractionalFee>
    @@immutable royaltyFees: list<RoyaltyFee>
}

@@finalType
AccountInfo {
    @@immutable accountId: AccountId
    @@immutable evmAddress: string
    @@immutable balance: int64
    @@immutable ethereumNonce: int64
    @@immutable pendingReward: int64
}

@@finalType
Token {
    @@immutable @@nullable tokenId: TokenId
    @@immutable name: string
    @@immutable symbol: string
    @@immutable type: TokenType
    @@immutable decimals: int64
    @@immutable metadata: bytes
}

@@finalType
TokenInfo {
    @@immutable tokenId: TokenId
    @@immutable type: TokenType
    @@immutable name: string
    @@immutable symbol: string
    @@immutable @@nullable memo: string
    @@immutable decimals: int64
    @@immutable metadata: bytes
    @@immutable createdTimestamp: zonedDateTime
    @@immutable modifiedTimestamp: zonedDateTime
    @@immutable @@nullable expiryTimestamp: zonedDateTime
    @@immutable supplyType: TokenSupplyType
    @@immutable initialSupply: string   // string to avoid int64 overflow
    @@immutable totalSupply: string
    @@immutable maxSupply: string
    @@immutable treasuryAccountId: AccountId
    @@immutable deleted: bool
    @@immutable customFees: CustomFee
}

@@finalType
Balance {
    @@immutable accountId: AccountId
    @@immutable balance: int64
    @@immutable decimals: int64
}

@@finalType
Nft {
    @@immutable tokenId: TokenId
    @@immutable serial: int64
    @@immutable owner: AccountId
    @@immutable metadata: bytes
}

@@finalType
NftMetadata {
    @@immutable tokenId: TokenId
    @@immutable name: string
    @@immutable symbol: string
    @@immutable treasuryAccountId: AccountId
}

@@finalType
Transfer {
    @@immutable account: AccountId
    @@immutable amount: int64
    @@immutable isApproval: bool
}

@@finalType
TokenTransfer {
    @@immutable tokenId: TokenId
    @@immutable account: AccountId
    @@immutable amount: int64
    @@immutable isApproval: bool
}

@@finalType
NftTransfer {
    @@immutable isApproval: bool
    @@immutable @@nullable senderAccountId: AccountId
    @@immutable @@nullable receiverAccountId: AccountId
    @@immutable serialNumber: int64
    @@immutable @@nullable tokenId: TokenId
}

@@finalType
StakingRewardTransfer {
    @@immutable account: AccountId
    @@immutable amount: int64
}

@@finalType
TransactionInfo {
    @@immutable transactionId: string
    @@immutable transactionHash: bytes
    @@immutable chargedTxFee: int64
    @@immutable consensusTimestamp: zonedDateTime
    @@immutable @@nullable entityId: string
    @@immutable maxFee: string
    @@immutable memo: bytes
    @@immutable name: TransactionType
    @@immutable nonce: int32
    @@immutable @@nullable node: string
    @@immutable @@nullable parentConsensusTimestamp: zonedDateTime
    @@immutable result: string
    @@immutable scheduled: bool
    @@immutable validDurationSeconds: string
    @@immutable validStartTimestamp: zonedDateTime
    @@immutable transfers: list<Transfer>
    @@immutable tokenTransfers: list<TokenTransfer>
    @@immutable nftTransfers: list<NftTransfer>
    @@immutable stakingRewardTransfers: list<StakingRewardTransfer>
}

@@finalType
Topic {
    @@immutable topicId: TopicId
    @@immutable @@nullable adminKey: string
    @@immutable @@nullable submitKey: string
    @@immutable @@nullable feeScheduleKey: string
    @@immutable @@nullable autoRenewAccount: AccountId
    @@immutable autoRenewPeriod: int32
    @@immutable createdTimestamp: zonedDateTime
    @@immutable deleted: bool
    @@immutable memo: string
    @@immutable fixedFees: list<FixedFee>
    @@immutable @@nullable feeExemptKeyList: list<string>
    @@immutable fromTimestamp: zonedDateTime
    @@immutable toTimestamp: zonedDateTime
}

@@finalType
ChunkInfo {
    @@immutable initialTransactionId: string
    @@immutable nonce: int32
    @@immutable number: int32
    @@immutable total: int32
    @@immutable scheduled: bool
}

@@finalType
TopicMessage {
    @@immutable @@nullable chunkInfo: ChunkInfo
    @@immutable consensusTimestamp: zonedDateTime
    @@immutable message: string
    @@immutable payerAccountId: AccountId
    @@immutable runningHash: bytes
    @@immutable runningHashVersion: int32
    @@immutable sequenceNumber: int64
    @@immutable topicId: TopicId
}

@@finalType
Contract {
    @@immutable contractId: ContractId
    @@immutable @@nullable adminKey: string
    @@immutable @@nullable autoRenewAccount: AccountId
    @@immutable autoRenewPeriod: int32
    @@immutable createdTimestamp: zonedDateTime
    @@immutable deleted: bool
    @@immutable @@nullable expirationTimestamp: zonedDateTime
    @@immutable @@nullable fileId: string
    @@immutable @@nullable evmAddress: string
    @@immutable @@nullable memo: string
    @@immutable @@nullable maxAutomaticTokenAssociations: int32
    @@immutable @@nullable nonce: int64
    @@immutable @@nullable obtainerId: string
    @@immutable permanentRemoval: bool
    @@immutable @@nullable proxyAccountId: string
    @@immutable fromTimestamp: zonedDateTime
    @@immutable toTimestamp: zonedDateTime
    @@immutable @@nullable bytecode: string
    @@immutable @@nullable runtimeBytecode: string
}

@@finalType
ExchangeRate {
    @@immutable centEquivalent: int32
    @@immutable hbarEquivalent: int32
    @@immutable expirationTime: zonedDateTime
}

@@finalType
ExchangeRates {
    @@immutable currentRate: ExchangeRate
    @@immutable nextRate: ExchangeRate
}

@@finalType
NetworkFee {
    @@immutable gas: int64
    @@immutable transactionType: string
}

@@finalType
NetworkStake {
    @@immutable maxStakeReward: int64
    @@immutable maxStakeRewardPerHbar: int64
    @@immutable maxTotalReward: int64
    @@immutable nodeRewardFeeFraction: double
    @@immutable reservedStakingRewards: int64
    @@immutable rewardBalanceThreshold: int64
    @@immutable stakeTotal: int64
    @@immutable stakingPeriodDuration: int64
    @@immutable stakingPeriodsStored: int64
    @@immutable stakingRewardFeeFraction: double
    @@immutable stakingRewardRate: int64
    @@immutable stakingStartThreshold: int64
    @@immutable unreservedStakingRewardBalance: int64
}

@@finalType
NetworkSupplies {
    @@immutable releasedSupply: string
    @@immutable totalSupply: string
}

abstraction AccountRepository {
    @@async @@throws(mirror-node-error)
    @@nullable AccountInfo findById(accountId: AccountId)
}

abstraction TokenRepository {
    @@async @@throws(mirror-node-error)
    Page<Token> findByAccount(accountId: AccountId)

    @@async @@throws(mirror-node-error)
    @@nullable TokenInfo findById(tokenId: TokenId)

    @@async @@throws(mirror-node-error)
    Page<Balance> getBalances(tokenId: TokenId)

    @@async @@throws(mirror-node-error)
    Page<Balance> getBalancesForAccount(tokenId: TokenId, accountId: AccountId)
}

abstraction NftRepository {
    @@async @@throws(mirror-node-error)
    Page<Nft> findByOwner(ownerId: AccountId)

    @@async @@throws(mirror-node-error)
    Page<Nft> findByType(tokenId: TokenId)

    @@async @@throws(mirror-node-error)
    @@nullable Nft findByTypeAndSerial(tokenId: TokenId, serialNumber: int64)

    @@async @@throws(mirror-node-error)
    Page<Nft> findByOwnerAndType(ownerId: AccountId, tokenId: TokenId)

    @@async @@throws(mirror-node-error)
    Page<NftMetadata> findAllTypes()

    @@async @@throws(mirror-node-error)
    Page<NftMetadata> findTypesByOwner(ownerId: AccountId)

    @@async @@throws(mirror-node-error)
    @@nullable NftMetadata getNftMetadata(tokenId: TokenId)
}

abstraction TransactionRepository {
    @@async @@throws(mirror-node-error)
    Page<TransactionInfo> findByAccount(accountId: AccountId)

    @@async @@throws(mirror-node-error)
    Page<TransactionInfo> findByAccountAndType(accountId: AccountId, type: TransactionType)

    @@async @@throws(mirror-node-error)
    Page<TransactionInfo> findByAccountAndResult(accountId: AccountId, result: TransactionResult)

    @@async @@throws(mirror-node-error)
    Page<TransactionInfo> findByAccountAndModification(accountId: AccountId, modification: BalanceModification)

    @@async @@throws(mirror-node-error)
    @@nullable TransactionInfo findById(transactionId: string)
}

abstraction TopicRepository {
    @@async @@throws(mirror-node-error)
    @@nullable Topic findById(topicId: TopicId)

    @@async @@throws(mirror-node-error)
    Page<TopicMessage> getMessages(topicId: TopicId)

    @@async @@throws(mirror-node-error)
    @@nullable TopicMessage getMessageBySequenceNumber(topicId: TopicId, sequenceNumber: int64)
}

abstraction ContractRepository {
    @@async @@throws(mirror-node-error)
    Page<Contract> findAll()

    @@async @@throws(mirror-node-error)
    @@nullable Contract findById(contractId: ContractId)
}

abstraction NetworkRepository {
    @@async @@throws(mirror-node-error)
    @@nullable ExchangeRates exchangeRates()

    @@async @@throws(mirror-node-error)
    list<NetworkFee> fees()

    @@async @@throws(mirror-node-error)
    @@nullable NetworkStake stake()

    @@async @@throws(mirror-node-error)
    @@nullable NetworkSupplies supplies()
}

MirrorNodeClient {
    @@immutable accounts: AccountRepository
    @@immutable tokens: TokenRepository
    @@immutable nfts: NftRepository
    @@immutable transactions: TransactionRepository
    @@immutable topics: TopicRepository
    @@immutable contracts: ContractRepository
    @@immutable network: NetworkRepository
}

@@throws(mirror-node-error)
MirrorNodeClient createMirrorNodeClient(mirrorNode: MirrorNode)
```

## Example

```
mirrorNode = MirrorNode(restBaseUrl: "https://mainnet.mirrornode.hedera.com/api/v1")
client = createMirrorNodeClient(mirrorNode)

// Look up an account
accountInfo = client.accounts.findById(fromString("0.0.1234"))

// Iterate paginated NFTs
page = client.nfts.findByOwner(fromString("0.0.5678"))
while page.hasNext() {
    page = page.next()
}

// Query transactions by type
txns = client.transactions.findByAccountAndType(accountId, TransactionType.CRYPTO_TRANSFER)
```

## Questions & Comments

- Should `TokenId`, `TopicId`, and `ContractId` live in the `common` namespace?
- Should keys be typed as `PublicKey` (from `keys` namespace) instead of `string`?
- Should transaction filtering use a query builder pattern instead of separate methods?
