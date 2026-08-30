import { describe, expect, it } from "vitest";
import { KeyAlgorithm, KeyPair, PrivateKey, PublicKey } from "../../src/keys/index.js";

const algorithms = [KeyAlgorithm.ED25519, KeyAlgorithm.ECDSA] as const;
const message = new TextEncoder().encode("hiero sdk v3 prototype");

describe("keys round-trip", () => {
  it.each(algorithms)("generate -> sign -> verify with %s", (algorithm) => {
    const privateKey = PrivateKey.generate(algorithm);
    const publicKey = privateKey.createPublicKey();
    const signature = privateKey.sign(message);

    expect(publicKey.algorithm).toBe(algorithm);
    expect(publicKey.verify(message, signature)).toBe(true);
  });

  it.each(algorithms)("PEM round-trip with %s", (algorithm) => {
    const privateKey = PrivateKey.generate(algorithm);
    const publicKey = privateKey.createPublicKey();

    const restoredPrivate = PrivateKey.fromPEM(privateKey.toPEM());
    const restoredPublic = PublicKey.fromPEM(publicKey.toPEM());
    const signature = restoredPrivate.sign(message);

    expect(restoredPublic.verify(message, signature)).toBe(true);
  });

  it.each(algorithms)("DER round-trip with %s", (algorithm) => {
    const privateKey = PrivateKey.generate(algorithm);
    const publicKey = privateKey.createPublicKey();

    const restoredPrivate = PrivateKey.fromDER(algorithm, privateKey.toRawBytes());
    const restoredPublic = PublicKey.fromDER(algorithm, publicKey.toRawBytes());
    const signature = restoredPrivate.sign(message);

    expect(restoredPublic.verify(message, signature)).toBe(true);
  });

  it.each(algorithms)("KeyPair.generate with %s", (algorithm) => {
    const pair = KeyPair.generate(algorithm);
    const signature = pair.privateKey.sign(message);

    expect(pair.publicKey.verify(message, signature)).toBe(true);
  });
});
