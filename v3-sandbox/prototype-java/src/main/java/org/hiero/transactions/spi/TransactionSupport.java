package org.hiero.transactions.spi;

import org.hiero.transactions.Receipt;
import org.hiero.transactions.Record;
import org.hiero.transactions.Response;
import org.hiero.transactions.TransactionBuilder;
import org.jspecify.annotations.NonNull;

/**
 * Per-transaction-type SPI. Mirrors the meta-language abstraction
 * {@code transactions-spi.TransactionSupport<$$TransactionBuilder, $$Response, $$Receipt, $$Record>}.
 *
 * <p><strong>The Java mapping deliberately omits the protobuf-typed methods
 * ({@code updateBody}, {@code convert(...)} variants) and the gRPC method
 * descriptor.</strong> The {@code transactions-spi}, {@code grpc} and
 * {@code hiero-proto} namespaces in the meta-language are placeholders — see
 * REPORT.md "Placeholder namespaces". Once these are defined, this interface will
 * gain the corresponding {@code update}/{@code convert} methods.
 *
 * <p>The {@code @@throws(not-found-error)} factory method
 * {@code getTransactionSupport(transactionType)} is implemented as
 * {@link TransactionSupportRegistry#getTransactionSupport(Class)}.
 *
 * @param <BUILDER> the concrete builder type this SPI supports
 * @param <RECEIPT> the receipt type
 * @param <RECORD>  the record type
 * @param <RESP>    the response type
 */
public interface TransactionSupport<
        BUILDER extends TransactionBuilder<BUILDER, RECEIPT, RECORD, RESP>,
        RECEIPT extends Receipt,
        RECORD extends Record<RECEIPT>,
        RESP extends Response<RECEIPT, RECORD>> {

    /**
     * Returns the concrete {@link TransactionBuilder} class this SPI supports. Maps
     * the meta-language {@code type getTransactionType()} method.
     *
     * @return the supported transaction builder class
     */
    @NonNull
    Class<BUILDER> getTransactionType();
}
