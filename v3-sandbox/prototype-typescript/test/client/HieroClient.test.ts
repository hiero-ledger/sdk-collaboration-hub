import { describe, expect, it } from "vitest";
import { AccountId, Ledger } from "../../src/common/index.js";
import { NetworkSetting } from "../../src/config/index.js";
import { PrivateKey } from "../../src/keys/index.js";
import { KeyAlgorithm } from "../../src/keys/KeyAlgorithm.js";
import { HieroClient, OperatorAccount } from "../../src/client/index.js";

describe("HieroClient", () => {
  it("creates client with operator and network", () => {
    const ledger = new Ledger(new Uint8Array([1]), "mainnet");
    const network = new NetworkSetting(ledger, [], []);
    const privateKey = PrivateKey.generate(KeyAlgorithm.ED25519);
    const operator = new OperatorAccount(AccountId.fromString("0.0.2"), privateKey);

    const client = HieroClient.createClient(network, operator);

    expect(client.operatorAccount.accountId.toString()).toBe("0.0.2");
    expect(client.ledger.name).toBe("mainnet");
    expect(client.getNetworkSetting()).toBe(network);
  });
});
