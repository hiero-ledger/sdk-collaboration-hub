import type { AccountId } from "./AccountId.js";

export class ConsensusNode {
  public readonly ip: string;
  public readonly port: number;
  public readonly account: AccountId;

  public constructor(ip: string, port: number, account: AccountId) {
    this.ip = ip;
    this.port = port;
    this.account = account;
  }
}
