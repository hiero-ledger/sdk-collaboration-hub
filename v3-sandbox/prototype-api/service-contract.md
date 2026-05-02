# Contract Service API

Service definition for smart contract interaction.

## API Schema

```
namespace enterprise.service.contract
requires common

ParamSupplier<$$SolidityType> {
    $$SolidityType getNativeContractValue()
}

Param<$$LangType, $$SolidityType> {
    @@immutable value:$$LangType
    @@immutable nativeType:string
    @@immutable supplier:ParamSupplier<$$SolidityType>
}

ContractCallResult {
    //TODO: Provide a good and extentsible way to receive 0-N of the possible return types
}

SmartContractService {

    @@throws(service-error) common.ContractId createContract(fileId:common.FileId, constructorParams:Param<ANY, ANY>...)
    
    @@throws(service-error) common.ContractId createContract(contents:bytes, constructorParams:Param<ANY, ANY>...)
    
    @@throws(service-error) ContractCallResult callContractFunction(contractId:common.ContractId, functionName:string, params:Param<ANY, ANY>...)
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
Param<int256> int256(value:int256)
```