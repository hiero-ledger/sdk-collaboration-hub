import { createPublicKey, verify as nodeVerify, type KeyObject } from "node:crypto";
import { Key } from "./Key.js";
import { KeyAlgorithm } from "./KeyAlgorithm.js";
import { KeyType } from "./KeyType.js";

function algorithmFromKeyObject(keyObject: KeyObject): KeyAlgorithm {
  if (keyObject.asymmetricKeyType === "ed25519") {
    return KeyAlgorithm.ED25519;
  }

  if (keyObject.asymmetricKeyType === "ec") {
    return KeyAlgorithm.ECDSA;
  }

  throw new Error(`Unsupported public key type: ${keyObject.asymmetricKeyType ?? "unknown"}`);
}

export class PublicKey extends Key {
  private readonly keyObject: KeyObject;

  private constructor(keyObject: KeyObject, algorithm: KeyAlgorithm) {
    const der = keyObject.export({ format: "der", type: "spki" });
    super(new Uint8Array(der), algorithm, KeyType.PUBLIC);
    this.keyObject = keyObject;
  }

  public static fromPEM(value: string): PublicKey {
    const keyObject = createPublicKey(value);
    return new PublicKey(keyObject, algorithmFromKeyObject(keyObject));
  }

  public static fromDER(algorithm: KeyAlgorithm, rawBytes: Uint8Array): PublicKey {
    const keyObject = createPublicKey({
      key: Buffer.from(rawBytes),
      format: "der",
      type: "spki",
    });
    return new PublicKey(keyObject, algorithm);
  }

  public verify(message: Uint8Array, signature: Uint8Array): boolean {
    const digest = this.algorithm === KeyAlgorithm.ED25519 ? null : "sha256";
    return nodeVerify(digest, Buffer.from(message), this.keyObject, Buffer.from(signature));
  }

  public toPEM(): string {
    return this.keyObject.export({ format: "pem", type: "spki" }).toString();
  }
}

