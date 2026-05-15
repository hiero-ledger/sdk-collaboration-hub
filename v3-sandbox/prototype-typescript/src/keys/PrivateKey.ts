import {
  createPrivateKey,
  createPublicKey,
  generateKeyPairSync,
  sign as nodeSign,
  type KeyObject,
} from "node:crypto";
import { Key } from "./Key.js";
import { KeyAlgorithm } from "./KeyAlgorithm.js";
import { KeyType } from "./KeyType.js";
import { PublicKey } from "./PublicKey.js";

function algorithmFromKeyObject(keyObject: KeyObject): KeyAlgorithm {
  if (keyObject.asymmetricKeyType === "ed25519") return KeyAlgorithm.ED25519;
  if (keyObject.asymmetricKeyType === "ec") return KeyAlgorithm.ECDSA;
  throw new Error(`Unsupported key type: ${keyObject.asymmetricKeyType ?? "unknown"}`);
}

export class PrivateKey extends Key {
  private readonly keyObject: KeyObject;

  private constructor(keyObject: KeyObject, algorithm: KeyAlgorithm) {
    const der = keyObject.export({ format: "der", type: "pkcs8" });
    super(new Uint8Array(der), algorithm, KeyType.PRIVATE);
    this.keyObject = keyObject;
  }

  public static generate(algorithm: KeyAlgorithm): PrivateKey {
    if (algorithm === KeyAlgorithm.ED25519) {
      const { privateKey } = generateKeyPairSync("ed25519");
      return new PrivateKey(privateKey, algorithm);
    }
    const { privateKey } = generateKeyPairSync("ec", { namedCurve: "secp256k1" });
    return new PrivateKey(privateKey, algorithm);
  }

  public static fromPEM(value: string): PrivateKey {
    const keyObject = createPrivateKey(value);
    return new PrivateKey(keyObject, algorithmFromKeyObject(keyObject));
  }

  public static fromDER(algorithm: KeyAlgorithm, rawBytes: Uint8Array): PrivateKey {
    const keyObject = createPrivateKey({ key: Buffer.from(rawBytes), format: "der", type: "pkcs8" });
    return new PrivateKey(keyObject, algorithm);
  }

  public sign(message: Uint8Array): Uint8Array {
    const digest = this.algorithm === KeyAlgorithm.ED25519 ? null : "sha256";
    return new Uint8Array(nodeSign(digest, Buffer.from(message), this.keyObject));
  }

  public createPublicKey(): PublicKey {
    return PublicKey.fromPEM(
      createPublicKey(this.keyObject).export({ format: "pem", type: "spki" }).toString(),
    );
  }

  public toPEM(): string {
    return this.keyObject.export({ format: "pem", type: "pkcs8" }).toString();
  }
}
