import { KeyAlgorithm } from "./KeyAlgorithm.js";
import { PrivateKey } from "./PrivateKey.js";
import type { PublicKey } from "./PublicKey.js";

export class KeyPair {
  public readonly publicKey: PublicKey;
  public readonly privateKey: PrivateKey;

  public constructor(publicKey: PublicKey, privateKey: PrivateKey) {
    this.publicKey = publicKey;
    this.privateKey = privateKey;
  }

  public static generate(algorithm: KeyAlgorithm): KeyPair {
    const privateKey = PrivateKey.generate(algorithm);
    return new KeyPair(privateKey.createPublicKey(), privateKey);
  }
}
