
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

/**
 * HinTS: The hinTS threshold signature scheme scaffolding
 * 
 * Implements a conceptual m-of-n threshold signature scheme
 * using Shamir's Secret Sharing over a prime field.
 */
export class HinTS {
    // We use a 128-bit prime for mock scaffolding: 2^127 - 1 (Mersenne prime)
    static readonly PRIME = 170141183460469231731687303715884105727n; 

    /**
     * Key Generation (creating the threshold key pairs and shares)
     */
    static generateKeys(n: number, m: number): ThresholdKeyPair {
        if (m > n) throw new Error("Threshold m cannot be greater than n");
        if (m < 1) throw new Error("Threshold m must be at least 1");

        // The secret polynomial f(x) = a_0 + a_1*x + ... + a_{m-1}*x^{m-1}
        // a_0 is the private key
        const coeffs: bigint[] = [];
        for (let i = 0; i < m; i++) {
            coeffs.push(this.randomBigInt(this.PRIME));
        }

        const privateKey = coeffs[0];
        const publicKey = privateKey; // In a real discrete log scheme, pub = g^privateKey

        const shares: KeyShare[] = [];
        for (let i = 1; i <= n; i++) {
            let shareVal = 0n;
            let x = BigInt(i);
            let xPow = 1n;
            for (let c = 0; c < m; c++) {
                shareVal = (shareVal + coeffs[c]! * xPow) % this.PRIME;
                xPow = (xPow * x) % this.PRIME;
            }
            shares.push({ id: i, share: shareVal });
        }

        return { publicKey, shares, m, n };
    }

    /**
     * Partial Signing (generating a signature share)
     */
    static partialSign(message: string, share: KeyShare): PartialSignature {
        const msgHash = this.hashMessage(message);
        const partialSig = (share.share * msgHash) % this.PRIME;
        return { id: share.id, partialSig };
    }

    /**
     * Signature Aggregation (combining 'm' valid shares into a single threshold signature)
     */
    static aggregateSignatures(partialSigs: PartialSignature[], m: number): bigint {
        if (partialSigs.length < m) {
            throw new Error(`Insufficient shares for aggregation. Expected at least ${m}, got ${partialSigs.length}`);
        }

        // Use Lagrange interpolation at x=0
        let aggregated = 0n;
        const sharesToUse = partialSigs.slice(0, m);

        for (let i = 0; i < m; i++) {
            let num = 1n;
            let den = 1n;
            for (let j = 0; j < m; j++) {
                if (i !== j) {
                    const xi = BigInt(sharesToUse[i]!.id);
                    const xj = BigInt(sharesToUse[j]!.id);
                    
                    num = (num * (-xj)) % this.PRIME;
                    den = (den * (xi - xj)) % this.PRIME;
                }
            }
            
            // Handle negative numbers for modulo
            num = (num + this.PRIME) % this.PRIME;
            den = (den + this.PRIME) % this.PRIME;

            const denInv = this.modInverse(den, this.PRIME);
            const lagrangeBasis = (num * denInv) % this.PRIME;
            
            const term = (sharesToUse[i]!.partialSig * lagrangeBasis) % this.PRIME;
            aggregated = (aggregated + term) % this.PRIME;
        }

        return aggregated;
    }

    /**
     * Verification (verifying the aggregated signature against the public key)
     */
    static verify(message: string, signature: bigint, publicKey: bigint): boolean {
        const msgHash = this.hashMessage(message);
        const expectedSig = (publicKey * msgHash) % this.PRIME;
        return signature === expectedSig;
    }

    private static hashMessage(message: string): bigint {
        const hash = crypto.createHash('sha256').update(message).digest('hex');
        return BigInt('0x' + hash) % this.PRIME;
    }

    private static randomBigInt(max: bigint): bigint {
        const hex = crypto.randomBytes(16).toString('hex');
        return BigInt('0x' + hex) % max;
    }

    private static modInverse(a: bigint, m: bigint): bigint {
        let [m0, x0, x1] = [m, 0n, 1n];
        if (m === 1n) return 0n;
        // make sure a is positive
        a = (a % m + m) % m;
        while (a > 1n) {
            let q = a / m;
            let t = m;
            m = a % m;
            a = t;
            t = x0;
            x0 = x1 - q * x0;
            x1 = t;
        }
        if (x1 < 0n) x1 += m0;
        return x1;
    }
}
