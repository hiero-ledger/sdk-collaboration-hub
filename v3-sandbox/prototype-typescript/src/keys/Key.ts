/* eslint-disable no-unused-vars, @typescript-eslint/no-unused-vars */
import type { KeyAlgorithm } from "./KeyAlgorithm.js";
import type { KeyType } from "./KeyType.js";

export abstract class Key {
  public constructor(
    public readonly bytes: Uint8Array,
    public readonly algorithm: KeyAlgorithm,
    public readonly type: KeyType,
  ) {}

  public toRawBytes(): Uint8Array {
    return new Uint8Array(this.bytes);
  }

  public abstract toPEM(): string;
}

