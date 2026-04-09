package org.hiero.transactions.accounts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.hiero.client.HieroClient;
import org.hiero.client.OperatorAccount;
import org.hiero.common.AccountId;
import org.hiero.common.ConsensusNode;
import org.hiero.common.Hbar;
import org.hiero.common.HbarUnit;
import org.hiero.common.Ledger;
import org.hiero.common.MirrorNode;
import org.hiero.config.NetworkSetting;
import org.hiero.config.impl.InMemoryNetworkSetting;
import org.hiero.keys.KeyAlgorithm;
import org.hiero.keys.PrivateKey;
import org.hiero.keys.PublicKey;
import org.hiero.transactions.Transaction;
import org.junit.jupiter.api.Test;

class AccountCreateTransactionBuilderTest {

    @Test
    void buildsAccountCreateTransaction() {
        // given
        final NetworkSetting setting = newInMemoryNetwork();
        final PrivateKey opKey = PrivateKey.generatePrivateKey(KeyAlgorithm.ED25519);
        final OperatorAccount operator = new OperatorAccount(new AccountId(0L, 0L, 2L), opKey);
        final HieroClient client = HieroClient.createClient(setting, operator);
        final PublicKey newAccountKey = PrivateKey.generatePrivateKey(KeyAlgorithm.ED25519).createPublicKey();

        // when
        final Transaction tx = new AccountCreateTransactionBuilder()
                .setKey(newAccountKey)
                .setInitialBalance(new Hbar(100L, HbarUnit.HBAR))
                .build(client);

        // then
        assertNotNull(tx);
    }

    @Test
    void rejectsBuildWithoutKey() {
        // given
        final NetworkSetting setting = newInMemoryNetwork();
        final PrivateKey opKey = PrivateKey.generatePrivateKey(KeyAlgorithm.ED25519);
        final OperatorAccount operator = new OperatorAccount(new AccountId(0L, 0L, 2L), opKey);
        final HieroClient client = HieroClient.createClient(setting, operator);

        // when / then
        assertThrows(IllegalStateException.class,
                () -> new AccountCreateTransactionBuilder()
                        .setInitialBalance(new Hbar(100L, HbarUnit.HBAR))
                        .build(client));
    }

    @Test
    void initialBalanceDefaultsToZero() {
        // given / when
        final AccountCreateTransactionBuilder builder = new AccountCreateTransactionBuilder();

        // then
        assertEquals(0L, builder.getInitialBalance().toTinybars());
    }

    private static NetworkSetting newInMemoryNetwork() {
        final Ledger ledger = new Ledger(new byte[]{1}, "prototype-net");
        final ConsensusNode node = new ConsensusNode("127.0.0.1", 50211, new AccountId(0L, 0L, 3L));
        return new InMemoryNetworkSetting(ledger, Set.of(node), Set.<MirrorNode>of());
    }
}
