export const KeyType = {
  PUBLIC: "PUBLIC",
  PRIVATE: "PRIVATE",
} as const;

export type KeyType = (typeof KeyType)[keyof typeof KeyType];
