# Service API

The service layer is a layer on topic of the classical SDK functionalities (that exists in V2 and V3).
The idea is to make it easier to use the SDK and to allow a better integration in enterprise applications.

## API Schema

```
namespace enterprise.service

Page<T> {
  @immutable pageIndex:int32;
  @immutable size:int32;
  @immutable data:list<T>;
  @immutable hasNext:boolean;
  @immutable isFirst:boolean;
  
  @async Page<T> next();
  @async Page<T> first();
}

```