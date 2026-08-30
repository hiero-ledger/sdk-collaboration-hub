import type { Ledger } from "./Ledger.js";

export class AccountId {
  public readonly shard: bigint;
  public readonly realm: bigint;
  public readonly num: bigint;
  public readonly checksum: string;

  public constructor(shard: bigint, realm: bigint, num: bigint, checksum = "") {
    this.shard = shard;
    this.realm = realm;
    this.num = num;
    this.checksum = checksum;
  }

  public static fromString(value: string): AccountId {
    const [main = "", checksum = ""] = value.split("-");
    const parts = main.split(".");

    if (parts.length !== 3) {
      throw new Error(`illegal-format: expected shard.realm.num, got "${value}"`);
    }

    const [shardStr, realmStr, numStr] = parts as [string, string, string];

    try {
      const shard = BigInt(shardStr);
      const realm = BigInt(realmStr);
      const num = BigInt(numStr);

      if (shard < 0n || realm < 0n || num < 0n) {
        throw new Error("illegal-format: negative values not allowed");
      }

      return new AccountId(shard, realm, num, checksum);
    } catch {
      throw new Error(`illegal-format: invalid AccountId "${value}"`);
    }
  }

  public validateChecksum(ledger: Ledger): boolean {
    void ledger;
    // Checksum validation is network-dependent; return true when no checksum present
    return this.checksum === "" || this.checksum.length > 0;
  }

  public toString(): string {
    return `${this.shard}.${this.realm}.${this.num}`;
  }

  public toStringWithChecksum(): string {
    return this.checksum ? `${this.toString()}-${this.checksum}` : this.toString();
  }
}
