import { describe, expect, it } from "vitest";
import { AccountId } from "../../src/common/index.js";
import { ConsensusNode } from "../../src/common/ConsensusNode.js";
import { Ledger } from "../../src/common/Ledger.js";
import { MirrorNode } from "../../src/common/MirrorNode.js";
import {
  NetworkSetting,
  registerNetworkSetting,
  getNetworkSetting,
} from "../../src/config/index.js";

describe("NetworkSetting", () => {
  it("holds consensus and mirror nodes", () => {
    const ledger = new Ledger(new Uint8Array([1, 2, 3]), "testnet");
    const node = new ConsensusNode("127.0.0.1", 50211, AccountId.fromString("0.0.3"));
    const mirror = new MirrorNode("https://testnet.mirrornode.hedera.com/api/v1");
    const setting = new NetworkSetting(ledger, [node], [mirror]);

    expect(setting.getConsensusNodes().size).toBe(1);
    expect(setting.getMirrorNodes().size).toBe(1);
    expect(setting.ledger.name).toBe("testnet");
  });

  it("registers and retrieves network setting", () => {
    const ledger = new Ledger(new Uint8Array([0]), "test");
    const setting = new NetworkSetting(ledger, [], []);

    registerNetworkSetting("test-network", setting);
    expect(getNetworkSetting("test-network")).toBe(setting);
  });

  it("throws not-found-error for unknown identifier", () => {
    expect(() => getNetworkSetting("nonexistent-network-xyz")).toThrow("not-found-error");
  });
});
