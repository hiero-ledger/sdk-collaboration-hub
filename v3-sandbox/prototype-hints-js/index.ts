import { HinTS } from './src/HinTS';

console.log("=== HIP-1200: hinTS Threshold Signature Scheme Demo ===\n");

// 1. Key Generation
const n = 5; // Total shares
const m = 3; // Threshold required to sign

console.log(`1. Generating threshold keys (n=${n}, m=${m})...`);
const { publicKey, shares } = HinTS.generateKeys(n, m);
console.log(`   Public Key: ${publicKey}`);
console.log(`   Total Shares Generated: ${shares.length}\n`);

// 2. Message to sign
const message = "Hello, Hiero!";
console.log(`2. Message to sign: "${message}"\n`);

// 3. Partial Signing
console.log(`3. Generating partial signatures using only ${m} shares...`);
// Let's use share #1, #3, and #5
const selectedShares = [shares[0]!, shares[2]!, shares[4]!];
const partialSigs = selectedShares.map(share => {
    const partial = HinTS.partialSign(message, share);
    console.log(`   Share ID ${share.id} generated partial signature: ${partial.partialSig}`);
    return partial;
});
console.log("");

// 4. Signature Aggregation
console.log(`4. Aggregating ${m} partial signatures...`);
const aggregatedSignature = HinTS.aggregateSignatures(partialSigs, m);
console.log(`   Aggregated Signature: ${aggregatedSignature}\n`);

// 5. Verification
console.log(`5. Verifying the aggregated signature against the Public Key...`);
const isValid = HinTS.verify(message, aggregatedSignature, publicKey);
console.log(`   Signature is Valid? => ${isValid ? '✅ YES' : '❌ NO'}\n`);

// 6. Tampering Simulation
console.log(`6. Simulating verification with a tampered message...`);
const isTamperedValid = HinTS.verify("Tampered message", aggregatedSignature, publicKey);
console.log(`   Tampered Signature is Valid? => ${isTamperedValid ? '✅ YES' : '❌ NO'}`);
