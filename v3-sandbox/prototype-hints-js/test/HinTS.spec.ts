import { test, describe } from 'node:test';
import * as assert from 'node:assert';
import * as HinTS from '../src/HinTS';

describe('HIP-1200: hinTS threshold signature scheme', () => {
    test('should successfully generate keys with correct m and n', () => {
        const { m, n, shares, publicKey } = HinTS.generateKeys(5, 3);
        assert.strictEqual(m, 3);
        assert.strictEqual(n, 5);
        assert.strictEqual(shares.length, 5);
        assert.ok(publicKey !== undefined);
    });

    test('should throw an error if m > n', () => {
        assert.throws(() => HinTS.generateKeys(3, 5), /Threshold m cannot be greater than n/);
    });

    test('should throw an error if m < 1', () => {
        assert.throws(() => HinTS.generateKeys(5, 0), /Threshold m must be at least 1/);
    });

    test('should generate valid partial signatures', () => {
        const { shares } = HinTS.generateKeys(5, 3);
        const shareZero = shares[0];
        if (!shareZero) throw new Error("Share missing");
        
        const partialSig = HinTS.partialSign("test message", shareZero);
        assert.strictEqual(partialSig.id, shareZero.id);
        assert.ok(partialSig.partialSig !== undefined);
    });

    test('should aggregate signatures and verify correctly', () => {
        const message = "Hello, Hiero!";
        const { shares, m, publicKey } = HinTS.generateKeys(5, 3);

        const partialSigs = shares.slice(0, 3).map(share => HinTS.partialSign(message, share));

        const aggregatedSig = HinTS.aggregateSignatures(partialSigs, m);

        const isValid = HinTS.verify(message, aggregatedSig, publicKey);
        assert.strictEqual(isValid, true);
    });

    test('should fail to aggregate with fewer than m shares', () => {
        const message = "Hello, Hiero!";
        const { shares, m } = HinTS.generateKeys(5, 3);

        const partialSigs = shares.slice(0, 2).map(share => HinTS.partialSign(message, share));

        assert.throws(() => HinTS.aggregateSignatures(partialSigs, m), /Insufficient shares/);
    });

    test('should verify with any combination of m shares', () => {
        const message = "Hello, Hiero!";
        const { shares, m, publicKey } = HinTS.generateKeys(5, 3);

        const selectedShares = [shares[1], shares[3], shares[4]];
        const validShares = selectedShares.filter(s => s !== undefined) as HinTS.KeyShare[];
        
        const partialSigs = validShares.map(share => HinTS.partialSign(message, share));

        const aggregatedSig = HinTS.aggregateSignatures(partialSigs, m);
        const isValid = HinTS.verify(message, aggregatedSig, publicKey);
        assert.strictEqual(isValid, true);
    });

    test('should fail verification if message is tampered with', () => {
        const { shares, m, publicKey } = HinTS.generateKeys(5, 3);

        const partialSigs = shares.slice(0, 3).map(share => HinTS.partialSign("Original message", share));
        const aggregatedSig = HinTS.aggregateSignatures(partialSigs, m);

        const isValid = HinTS.verify("Tampered message", aggregatedSig, publicKey);
        assert.strictEqual(isValid, false);
    });

    test('should fail verification for incorrect signature', () => {
        const message = "Hello, Hiero!";
        const { shares, m, publicKey } = HinTS.generateKeys(5, 3);

        const partialSigs = shares.slice(0, 3).map(share => HinTS.partialSign(message, share));
        const aggregatedSig = HinTS.aggregateSignatures(partialSigs, m);

        const isValid = HinTS.verify(message, aggregatedSig + 1n, publicKey);
        assert.strictEqual(isValid, false);
    });
});
