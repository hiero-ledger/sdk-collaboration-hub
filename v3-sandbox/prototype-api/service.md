# Service API

Service definition for smart contract interaction.

## API Schema

```
namespace enterprise.service.contract
requires common

ParamSupplier<$$Type> {
    //TODO: add a generic param for it? In java a sealed class would be nice. We need to investigate more
    ANY getNativeContractValue()
}

Param<$$Type> {
    @@immutable value:$$Type
    @@immutable nativeType:string
    @@immutable supplier:ParamSupplier<$$Type>
}

ContractCallResult {
    //TODO: Provide a good and extentsible way to receive 0-N of the possible return types
}

SmartContractService {

    @@throws(service-error) common.ContractId createContract(fileId:common.FileId, constructorParams:Param<ANY>...)
    
    @@throws(service-error) common.ContractId createContract(contents:bytes, constructorParams:Param<ANY>...)
    
    @@throws(service-error) ContractCallResult callContractFunction(contractId:common.ContractId, functionName:string, params:Param<ANY>...)
}

// Factory methods for params to wrap native types in solidity types
Param<string> ofString(value:string)
Param<string> ofBytes(value:string)
Param<string> ofBytes23(value:string)
Param<bytes> ofBytes(value:bytes)
Param<bytes> ofBytes23(value:bytes)
Param<string> ofAddress(value:string)
Param<common.Address> ofAddress(value:common.Address)
Param<boolean> ofBool(value:boolean)
Param<uint8> uint8(value:uint8)
Param<int8> int8(value:int8)
//TODO
Param<uint256> uint256(value:uint256)
Param<int256> int8(value:int256)
```