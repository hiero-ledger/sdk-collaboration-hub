import type { AccountId } from "../common/AccountId.js";
import type { Ledger } from "../common/Ledger.js";
import type { PrivateKey } from "../keys/PrivateKey.js";
import type { NetworkSetting } from "../config/NetworkSetting.js";

export class OperatorAccount {
  public readonly accountId: AccountId;
  public readonly privateKey: PrivateKey;

  public constructor(accountId: AccountId, privateKey: PrivateKey) {
    this.accountId = accountId;
    this.privateKey = privateKey;
  }
}

export class HieroClient {
  public readonly operatorAccount: OperatorAccount;
  public readonly ledger: Ledger;
  private readonly networkSetting: NetworkSetting;

  private constructor(networkSetting: NetworkSetting, operatorAccount: OperatorAccount) {
    this.networkSetting = networkSetting;
    this.operatorAccount = operatorAccount;
    this.ledger = networkSetting.ledger;
  }

  public static createClient(networkSetting: NetworkSetting, operatorAccount: OperatorAccount): HieroClient {
    return new HieroClient(networkSetting, operatorAccount);
  }

  public getNetworkSetting(): NetworkSetting {
    return this.networkSetting;
  }
}
