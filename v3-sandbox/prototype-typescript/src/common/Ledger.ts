export class Ledger {
  public readonly id: Uint8Array;
  public readonly name: string | null;

  public constructor(id: Uint8Array, name: string | null = null) {
    this.id = id;
    this.name = name;
  }
}
