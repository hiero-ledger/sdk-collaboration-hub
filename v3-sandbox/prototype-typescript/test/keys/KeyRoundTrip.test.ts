import { describe, expect, it } from "vitest";
import { KeyAlgorithm, PrivateKey, PublicKey } from "../../src/keys/index.js";

const algorithms = [KeyAlgorithm.ED25519, KeyAlgorithm.ECDSA] as const;
const message = new TextEncoder().encode("hiero sdk v3 keys proof of concept");

describe("keys round trip", () => {
  it.each(algorithms)("generates, signs, and verifies with %s", (algorithm) => {
    const privateKey = PrivateKey.generate(algorithm);
    const publicKey = privateKey.createPublicKey();

    const signature = privateKey.sign(message);

    expect(publicKey.algorithm).toBe(algorithm);
    expect(publicKey.verify(message, signature)).toBe(true);
  });

  it.each(algorithms)("round-trips private and public keys through PEM with %s", (algorithm) => {
    const privateKey = PrivateKey.generate(algorithm);
    const importedPrivateKey = PrivateKey.fromPEM(privateKey.toPEM());
    const importedPublicKey = PublicKey.fromPEM(importedPrivateKey.createPublicKey().toPEM());

    const signature = importedPrivateKey.sign(message);

    expect(importedPrivateKey.algorithm).toBe(algorithm);
    expect(importedPublicKey.algorithm).toBe(algorithm);
    expect(importedPublicKey.verify(message, signature)).toBe(true);
  });

  it.each(algorithms)("round-trips private and public keys through DER bytes with %s", (algorithm) => {
    const privateKey = PrivateKey.generate(algorithm);
    const publicKey = privateKey.createPublicKey();

    const importedPrivateKey = PrivateKey.fromDER(algorithm, privateKey.toRawBytes());
    const importedPublicKey = PublicKey.fromDER(algorithm, publicKey.toRawBytes());
    const signature = importedPrivateKey.sign(message);

    expect(importedPublicKey.verify(message, signature)).toBe(true);
  });
});

