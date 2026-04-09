package org.hiero.client;

import java.util.Objects;
import org.hiero.common.AccountId;
import org.hiero.keys.PrivateKey;
import org.jspecify.annotations.NonNull;

/**
 * The account that signs and pays for requests. Mirrors the meta-language type
 * {@code client.OperatorAccount}. Both fields are {@code @@immutable}, so the type
 * is implemented as a Java record.
 *
 * @param accountId  the account id of the operator
 * @param privateKey the private key of the operator
 */
public record OperatorAccount(@NonNull AccountId accountId, @NonNull PrivateKey privateKey) {

    public OperatorAccount {
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(privateKey, "privateKey must not be null");
    }

    @Override
    public String toString() {
        // Never include the raw private key in toString — see api-best-practices-java.md
        // section "Implementing toString" / "Never include sensitive data".
        return "OperatorAccount[accountId=" + accountId + ", privateKey=***]";
    }
}
