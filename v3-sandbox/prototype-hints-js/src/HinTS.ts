
import * as crypto from 'crypto';

export interface KeyShare {
    id: number;
    share: bigint;
}

export interface ThresholdKeyPair {
    publicKey: bigint; // Represents the aggregated public key
    shares: KeyShare[];
    m: number;
    n: number;
}

export interface PartialSignature {
    id: number;
    partialSig: bigint;
}

// We use a 128-bit prime for mock scaffolding: 2^127 - 1 (Mersenne prime)
export const PRIME = 170141183460469231731687303715884105727n;

export function generateKeys(n: number, m: number): ThresholdKeyPair {
    if (m > n) throw new Error("Threshold m cannot be greater than n");
    if (m < 1) throw new Error("Threshold m must be at least 1");

    const coeffs: bigint[] = [];
    for (let i = 0; i < m; i++) {
        coeffs.push(randomBigInt(PRIME));
    }

    // eslint-disable-next-line security/detect-object-injection
    const privateKey = coeffs[0] || 0n;
    const publicKey = privateKey; // In a real discrete log scheme, pub = g^privateKey

    const shares: KeyShare[] = [];
    for (let i = 1; i <= n; i++) {
        let shareVal = 0n;
        const x = BigInt(i);
        let xPow = 1n;
        for (const coeff of coeffs) {
            shareVal = (shareVal + coeff * xPow) % PRIME;
            xPow = (xPow * x) % PRIME;
        }
        shares.push({ id: i, share: shareVal });
    }

    return { publicKey, shares, m, n };
}

export function partialSign(message: string, share: KeyShare): PartialSignature {
    const msgHash = hashMessage(message);
    const partialSig = (share.share * msgHash) % PRIME;
    return { id: share.id, partialSig };
}

export function aggregateSignatures(partialSigs: PartialSignature[], m: number): bigint {
    if (partialSigs.length < m) {
        throw new Error(`Insufficient shares for aggregation. Expected at least ${m}, got ${partialSigs.length}`);
    }

    let aggregated = 0n;
    const sharesToUse = partialSigs.slice(0, m);

    for (const shareI of sharesToUse) {
        let num = 1n;
        let den = 1n;
        for (const shareJ of sharesToUse) {
            if (shareI.id !== shareJ.id) {
                const xi = BigInt(shareI.id);
                const xj = BigInt(shareJ.id);

                num = (num * (-xj)) % PRIME;
                den = (den * (xi - xj)) % PRIME;
            }
        }

        num = (num + PRIME) % PRIME;
        den = (den + PRIME) % PRIME;

        const denInv = modInverse(den, PRIME);
        const lagrangeBasis = (num * denInv) % PRIME;

        const term = (shareI.partialSig * lagrangeBasis) % PRIME;
        aggregated = (aggregated + term) % PRIME;
    }

    return aggregated;
}

export function verify(message: string, signature: bigint, publicKey: bigint): boolean {
    const msgHash = hashMessage(message);
    const expectedSig = (publicKey * msgHash) % PRIME;
    return signature === expectedSig;
}

function hashMessage(message: string): bigint {
    const hash = crypto.createHash('sha256').update(message).digest('hex');
    return BigInt('0x' + hash) % PRIME;
}

function randomBigInt(max: bigint): bigint {
    const hex = crypto.randomBytes(16).toString('hex');
    return BigInt('0x' + hex) % max;
}

function modInverse(a: bigint, m: bigint): bigint {
    let [m0, x0, x1] = [m, 0n, 1n];
    if (m === 1n) return 0n;
    let aMod = (a % m + m) % m;
    while (aMod > 1n) {
        const q = aMod / m;
        const t = m;
        m = aMod % m;
        aMod = t;
        const t2 = x0;
        x0 = x1 - q * x0;
        x1 = t2;
    }
    if (x1 < 0n) x1 += m0;
    return x1;
}
