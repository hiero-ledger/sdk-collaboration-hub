package org.hiero.transactions;

/**
 * Status codes for services that are part of the consensus node repository. Mirrors
 * the meta-language enum {@code transactions.BasicTransactionStatus} which extends
 * {@code TransactionStatus}.
 *
 * <p>The meta-language definition only enumerates a partial list (it ends with
 * {@code ...}). The Java mapping replicates the listed values; the rest are tracked
 * in REPORT.md as "Definition gap: BasicTransactionStatus is incomplete".
 *
 * <p>Per the api-best-practices-java guide, enum attributes annotated with
 * {@code @@immutable} (here: {@code code}) become {@code final} fields set via the
 * constructor. Each enum literal carries its own status code.
 */
public enum BasicTransactionStatus implements TransactionStatus {

    OK(0),
    INVALID_TRANSACTION(1),
    PAYER_ACCOUNT_NOT_FOUND(2),
    GRPC_WEB_PROXY_NOT_SUPPORTED(3);
    // NOTE: more codes are intentionally elided — see REPORT.md.

    private final int code;

    BasicTransactionStatus(final int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
