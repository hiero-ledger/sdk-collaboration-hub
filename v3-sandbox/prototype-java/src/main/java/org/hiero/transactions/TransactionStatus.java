package org.hiero.transactions;

/**
 * Status of a transaction. Mirrors the meta-language abstraction
 * {@code transactions.TransactionStatus}.
 *
 * <p>The meta-language defines this as an abstraction with a single immutable
 * {@code code:int32} field so that custom services on the consensus node can
 * register additional status codes at runtime — an enum cannot be extended in
 * any of the supported SDK languages.
 *
 * <p>The Java mapping is therefore an interface; the standard codes are provided
 * by {@link BasicTransactionStatus}, custom services can define their own
 * implementation classes.
 */
public interface TransactionStatus {

    /**
     * @return the status code (unique per consensus node service)
     */
    int code();
}
