# NFT Service API

Service definition for NFT interaction.

## API Schema

```
namespace enterprise.service.nft
requires common, keys, client

NftType {
    @immutable id:common.TokenId,
    @immutable name:string,
    @immutable symbol:string,
    @immutable treasuryAccount:client.Account
}

NftId {
    @immutable type:NftType,
    @immutable serial:uint64
}

Nft extends NftId {
    @immutable owner:common.AccountId,
    @immutable metadata:bytes
}

NftService {

    @@throws(service-error) @nullable NftType getNftType(id:common.TokenId)
    
    @@throws(service-error) @nullable Nft getNft(id:NftId);

    @@throws(service-error) Page<NftType> findAllTypes()
    
    @@throws(service-error) Page<NftType> findTypesByOwner(ownerId:common.AccountId);

    @@throws(service-error) Page<Nft> findByOwner(ownerId:common.AccountId);
    
    @@throws(service-error) Page<Nft> findByType(type:NftType);
    
    @@throws(service-error) Page<Nft> findByOwnerAndType(ownerId:common.AccountId, type:NftType);
    
    @@throws(service-error) NftType createNftType(name:string, symbol:string)
    
    @@throws(service-error) NftType createNftType(name:string, symbol:string, supplierKey:keys.PublicKey)
    
    @@throws(service-error) NftType createNftType(name:string, symbol:string, treasuryAccount:client.Account)
    
    @@throws(service-error) NftType createNftType(name:string, symbol:string, supplierKey:keys.PublicKey, treasuryAccount:client.Account)
    
    @@throws(service-error) void associateAccountToNftType(type:NftType, account:client.Account)
    
    @@throws(service-error) void associateAccountsToNftType(types:collection<NftType>, account:client.Account)
    
    @@throws(service-error) void dissociateAccountToNftType(type:NftType, account:client.Account)
    
    @@throws(service-error) void dissociateAccountsToNftType(types:collection<NftType>, account:client.Account)

    @@throws(service-error) Nft mintNft(type:NftType, metadata:bytes)
    
    @@throws(service-error) Nft mintNft(type:NftType, supplierKey:keys.PublicKey, metadata:bytes)

    @@throws(service-error) list<Nft> mintNfts(type:NftType, metadata:list<bytes>)

    @@throws(service-error) list<Nft> mintNfts(type:NftType, supplierKey:keys.PublicKey, metadata:list<bytes>)

    @@throws(service-error) void burnNft(nft:NftId)

    @@throws(service-error) void burnNft(nft:NftId, supplierKey:keys.PublicKey)

    @@throws(service-error) void burnNfts(nfts:collection<NftId>)

    @@throws(service-error) void burnNfts(nfts:collection<NftId>, supplierKey:keys.PublicKey)
    
    @@throws(service-error) void transferNft(nft:NftId, fromAccount:client.Account, toAccountId:common.AccountId)
    
    @@throws(service-error) void transferNfts(nfts:collection<NftId>, fromAccount:client.Account, toAccountId:common.AccountId)
}
```