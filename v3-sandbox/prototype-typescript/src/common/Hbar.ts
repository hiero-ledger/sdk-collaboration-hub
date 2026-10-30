import { HbarUnit } from "./HbarUnit.js";

export class Hbar {
  public readonly amount: bigint;
  public readonly unit: HbarUnit;

  public constructor(amount: bigint | number, unit: HbarUnit = HbarUnit.HBAR) {
    this.amount = BigInt(amount);
    this.unit = unit;
  }

  public toTinybars(): bigint {
    return this.amount * this.unit.tinybars;
  }

  public to(targetUnit: HbarUnit): Hbar {
    const tinybars = this.toTinybars();
    return new Hbar(tinybars / targetUnit.tinybars, targetUnit);
  }
}
