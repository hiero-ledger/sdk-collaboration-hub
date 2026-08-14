# Mirror Node Contract Query API

## Description

## API Schema

```
namespace mirrornode.contract
requires common, keys

@@finalType
Contract {
    @@immutable contractId: common.ContractId
    @@immutable @@nullable adminKey: keys.PublicKey
    @@immutable @@nullable autoRenewAccount: common.AccountId
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

abstraction ContractRepository {
    @@async @@throws(mirror-node-error)
    Page<Contract> findAll()

    @@async @@throws(mirror-node-error)
    @@nullable Contract findById(contractId: ContractId)
}

```