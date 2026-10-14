export const KeyAlgorithm = {
  ED25519: "ED25519",
  ECDSA: "ECDSA",
} as const;

export type KeyAlgorithm = (typeof KeyAlgorithm)[keyof typeof KeyAlgorithm];
