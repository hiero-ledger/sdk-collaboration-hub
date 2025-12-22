package org.hiero.sdk.simple.sample;

import static java.lang.System.Logger.Level.INFO;

import io.github.cdimascio.dotenv.Dotenv;
import org.hiero.sdk.simple.HieroClient;
import org.hiero.sdk.simple.network.Account;
import org.hiero.sdk.simple.network.AccountId;
import org.hiero.sdk.simple.network.Hbar;
import org.hiero.sdk.simple.network.keys.Key;
import org.hiero.sdk.simple.network.keys.KeyAlgorithm;
import org.hiero.sdk.simple.network.keys.KeyEncoding;
import org.hiero.sdk.simple.network.keys.KeyPair;
import org.hiero.sdk.simple.network.keys.PrivateKey;
import org.hiero.sdk.simple.transactions.AccountCreateReceipt;
import org.hiero.sdk.simple.transactions.AccountCreateTransaction;

public class Sample {

    private final static System.Logger log = System.getLogger(Sample.class.getName());

    public static void main(String[] args) throws Exception {
        final Account operatorAccount = createOperatorAccount();
        final HieroClient hieroClient = HieroClient.create(operatorAccount, "hedera-testnet");
        final Key publicKeyForNewAccount = KeyPair.generate(KeyAlgorithm.ED25519).privateKey();

        final AccountCreateReceipt accountCreateReceipt = new AccountCreateTransaction()
                .withKey(publicKeyForNewAccount)
                .withInitialBalance(Hbar.of(2))
                .withKey(publicKeyForNewAccount)
                .packTransaction(hieroClient)
                .sendAndWait()
                .queryRecordAndWait()
                .receipt();
        log.log(INFO, "Account {0} created", accountCreateReceipt.createdAccount());
    }

    private static Account createOperatorAccount() {
        // Load environment variables from .env file
        final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        final AccountId operatorAccountId = AccountId.from(dotenv.get("OPERATOR_ACCOUNT_ID"));
        final PrivateKey operatorPrivateKey = PrivateKey.from(KeyAlgorithm.ECDSA, KeyEncoding.DER, dotenv.get("OPERATOR_PRIVATE_KEY"));
        return Account.of(operatorAccountId, operatorPrivateKey);
    }
}
