import type { ConsensusNode } from "../common/ConsensusNode.js";
import type { Ledger } from "../common/Ledger.js";
import type { MirrorNode } from "../common/MirrorNode.js";

export class NetworkSetting {
  public readonly ledger: Ledger;
  private readonly _consensusNodes: ReadonlySet<ConsensusNode>;
  private readonly _mirrorNodes: ReadonlySet<MirrorNode>;

  public constructor(ledger: Ledger, consensusNodes: ConsensusNode[], mirrorNodes: MirrorNode[]) {
    this.ledger = ledger;
    this._consensusNodes = new Set(consensusNodes);
    this._mirrorNodes = new Set(mirrorNodes);
  }

  public getConsensusNodes(): ReadonlySet<ConsensusNode> {
    return this._consensusNodes;
  }

  public getMirrorNodes(): ReadonlySet<MirrorNode> {
    return this._mirrorNodes;
  }
}

const _registry = new Map<string, NetworkSetting>();

export const HEDERA_MAINNET_IDENTIFIER = "hedera-mainnet";
export const HEDERA_TESTNET_IDENTIFIER = "hedera-testnet";

export function registerNetworkSetting(identifier: string, setting: NetworkSetting): void {
  _registry.set(identifier, setting);
}

export function getNetworkSetting(identifier: string): NetworkSetting {
  const setting = _registry.get(identifier);
  if (!setting) {
    throw new Error(`not-found-error: no network registered for identifier "${identifier}"`);
  }
  return setting;
}
