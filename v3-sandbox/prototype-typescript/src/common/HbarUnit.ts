export class HbarUnit {
  public static readonly TINYBAR  = new HbarUnit("tℏ", 1n);
  public static readonly MICROBAR = new HbarUnit("μℏ", 100n);
  public static readonly MILLIBAR = new HbarUnit("mℏ", 100_000n);
  public static readonly HBAR     = new HbarUnit("ℏ",  100_000_000n);
  public static readonly KILOBAR  = new HbarUnit("kℏ", 100_000_000_000n);
  public static readonly MEGABAR  = new HbarUnit("Mℏ", 100_000_000_000_000n);
  public static readonly GIGABAR  = new HbarUnit("Gℏ", 100_000_000_000_000_000n);

  private static readonly _all: HbarUnit[] = [
    HbarUnit.TINYBAR, HbarUnit.MICROBAR, HbarUnit.MILLIBAR,
    HbarUnit.HBAR, HbarUnit.KILOBAR, HbarUnit.MEGABAR, HbarUnit.GIGABAR,
  ];

  public readonly symbol: string;
  public readonly tinybars: bigint;

  private constructor(symbol: string, tinybars: bigint) {
    this.symbol = symbol;
    this.tinybars = tinybars;
  }

  public static values(): HbarUnit[] {
    return [...HbarUnit._all];
  }
}
