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

// Holds the ABI-encoded return value of a smart contract call.
// Accessors decode by position; index 0 is the first return value in the Solidity signature.
// All accessors are @@nullable — they return null when the value at that index is absent or is
// a different Solidity type than requested.
ContractCallResult {
    @@immutable rawResult: bytes                           // raw ABI-encoded bytes from the network response

    @@nullable string getString(index: int32)              // decodes a Solidity string return value
    @@nullable bytes getBytes(index: int32)                // decodes a Solidity bytes return value
    @@nullable int64 getInt64(index: int32)                // decodes a Solidity int64 return value
    @@nullable uint64 getUint64(index: int32)              // decodes a Solidity uint64 return value
    @@nullable bool getBool(index: int32)                  // decodes a Solidity bool return value
    @@nullable common.Address getAddress(index: int32)     // decodes a Solidity address return value
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

## Questions & Comments

- Should `ContractCallResult` expose accessors by name (e.g. `getString(name: string)`) in addition to by index, using the ABI JSON attached to the call? Named access requires the caller to supply the ABI, which adds complexity but improves readability.
- How should tuple and fixed/dynamic array return types be handled? A `getBytes` fallback lets callers decode them manually, but a typed `getArray` or `getTuple` accessor would be cleaner in languages that support it.
- `uint64` is the largest unsigned integer type that maps cleanly across all 7 target languages. Solidity `uint256` and `int256` appear in the `//TODO` factory methods above; the same gap exists for return values. Should `ContractCallResult` expose a `BigInteger`-style accessor, or leave wide integers to `rawResult`?
- Is `rawResult` always standard ABI encoding, or do custom precompiles return non-ABI-encoded bytes? If the latter, callers using `rawResult` directly may need to know which encoding to expect.