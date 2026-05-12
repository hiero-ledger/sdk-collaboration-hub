import type { KeyAlgorithm } from "./KeyAlgorithm.js";
import type { KeyType } from "./KeyType.js";

export abstract class Key {
  public readonly bytes: Uint8Array;
  public readonly algorithm: KeyAlgorithm;
  public readonly type: KeyType;

  public constructor(bytes: Uint8Array, algorithm: KeyAlgorithm, type: KeyType) {
    this.bytes = bytes;
    this.algorithm = algorithm;
    this.type = type;
  }

  public toRawBytes(): Uint8Array {
    return new Uint8Array(this.bytes);
  }

  public abstract toPEM(): string;
}
