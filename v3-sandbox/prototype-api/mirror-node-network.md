# Mirror Node Network Query API

## Description

## API Schema

```
namespace mirrornode.network
requires common, keys

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
    @@immutable releasedSupply: int256
    @@immutable totalSupply: int256
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

```