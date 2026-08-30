import { describe, expect, it } from "vitest";
import { AccountId } from "../../src/common/index.js";

describe("AccountId", () => {
  it("parses shard.realm.num format", () => {
    const id = AccountId.fromString("0.0.123456");
    expect(id.shard).toBe(0n);
    expect(id.realm).toBe(0n);
    expect(id.num).toBe(123456n);
    expect(id.checksum).toBe("");
  });

  it("parses with checksum suffix", () => {
    const id = AccountId.fromString("0.0.123456-xxxxx");
    expect(id.num).toBe(123456n);
    expect(id.checksum).toBe("xxxxx");
  });

  it("toString returns shard.realm.num", () => {
    expect(AccountId.fromString("0.0.5").toString()).toBe("0.0.5");
  });

  it("toStringWithChecksum includes checksum when present", () => {
    const id = AccountId.fromString("0.0.5-abcde");
    expect(id.toStringWithChecksum()).toBe("0.0.5-abcde");
  });

  it("toStringWithChecksum omits dash when no checksum", () => {
    expect(AccountId.fromString("0.0.5").toStringWithChecksum()).toBe("0.0.5");
  });

  it("throws on invalid format", () => {
    expect(() => AccountId.fromString("not-an-id")).toThrow("illegal-format");
    expect(() => AccountId.fromString("0.0")).toThrow("illegal-format");
  });
});
