import { describe, expect, it } from "vitest";
import { Hbar, HbarUnit } from "../../src/common/index.js";

describe("Hbar", () => {
  it("converts HBAR to tinybars correctly", () => {
    expect(new Hbar(1, HbarUnit.HBAR).toTinybars()).toBe(100_000_000n);
  });

  it("converts 100 HBAR to tinybars", () => {
    expect(new Hbar(100, HbarUnit.HBAR).toTinybars()).toBe(10_000_000_000n);
  });

  it("converts TINYBAR to tinybars (1:1)", () => {
    expect(new Hbar(500, HbarUnit.TINYBAR).toTinybars()).toBe(500n);
  });

  it("converts HBAR to TINYBAR via to()", () => {
    const converted = new Hbar(1, HbarUnit.HBAR).to(HbarUnit.TINYBAR);
    expect(converted.toTinybars()).toBe(100_000_000n);
    expect(converted.amount).toBe(100_000_000n);
  });

  it("converts KILOBAR to HBAR via to()", () => {
    const converted = new Hbar(1, HbarUnit.KILOBAR).to(HbarUnit.HBAR);
    expect(converted.amount).toBe(1000n);
  });

  it("HbarUnit.values() returns all units", () => {
    expect(HbarUnit.values()).toHaveLength(7);
  });
});
