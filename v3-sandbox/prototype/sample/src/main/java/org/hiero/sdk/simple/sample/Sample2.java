package org.hiero.sdk.simple.sample;

import org.hiero.sdk.simple.HieroClient;
import org.hiero.sdk.simple.network.Account;
import org.hiero.sdk.simple.network.AccountId;
import org.hiero.sdk.simple.network.Hbar;
import org.hiero.sdk.simple.network.keys.KeyAlgorithm;
import org.hiero.sdk.simple.network.keys.KeyEncoding;
import org.hiero.sdk.simple.network.keys.KeyPair;
import org.hiero.sdk.simple.network.keys.PrivateKey;
import org.hiero.sdk.simple.network.keys.PublicKey;
import org.hiero.sdk.simple.transactions.AccountCreateTransaction;

public class Sample2 {

    public static void main(String[] args) throws Exception {
        final Account operatorAccount = createOperatorAccount();
        final HieroClient hieroClient = HieroClient.create(operatorAccount, "hedera-testnet");
        final PublicKey publicKeyForNewAccount = KeyPair.generate(KeyAlgorithm.ED25519).publicKey();

        new AccountCreateTransaction().
                withKey(publicKeyForNewAccount)
                .withInitialBalance(Hbar.of(2))
                .packTransaction(hieroClient)
                .sendAndWait();
        System.out.println("huhu");
    }

    private static Account createOperatorAccount() {
        final AccountId operatorAccountId = AccountId.from("0.0.1001");
        final PrivateKey operatorPrivateKey = PrivateKey.from(KeyAlgorithm.ECDSA, KeyEncoding.DER,
                "3030020100300706052b8104000a0422042086cb696f64aefea6450546c58f5c19e4353aea309d7f03ee52255f235f5e410b");
        return Account.of(operatorAccountId, operatorPrivateKey);
    }
}
