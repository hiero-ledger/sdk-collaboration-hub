package org.hiero.transactions.spi;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.hiero.sdk.annotation.ThreadSafe;
import org.hiero.transactions.TransactionBuilder;
import org.jspecify.annotations.NonNull;

/**
 * Static registry that exposes the namespace-level factory methods of the
 * {@code transactions-spi} namespace:
 *
 * <ul>
 *   <li>{@code @@throws(not-found-error) TransactionSupport getTransactionSupport(type)}</li>
 *   <li>{@code set<TransactionSupport> getAllTransactionSupports()}</li>
 * </ul>
 *
 * <p>The api-best-practice rule "factories should be implemented as static methods
 * on the type that is created" cannot apply directly to {@link TransactionSupport}
 * because the factories return arbitrary {@link TransactionSupport} subtypes,
 * not a specific instance — there is no obvious owning type. They are therefore
 * grouped on this dedicated registry.
 */
public final class TransactionSupportRegistry {

    private static final ConcurrentMap<Class<? extends TransactionBuilder<?, ?, ?, ?>>, TransactionSupport<?, ?, ?, ?>>
            REGISTRY = new ConcurrentHashMap<>();

    private TransactionSupportRegistry() {
    }

    /**
     * Registers the given {@link TransactionSupport} implementation. Idempotent —
     * registering the same support twice has no effect.
     *
     * @param support the support to register
     */
    @ThreadSafe
    public static <
            B extends TransactionBuilder<B, R, REC, RESP>,
            R extends org.hiero.transactions.Receipt,
            REC extends org.hiero.transactions.Record<R>,
            RESP extends org.hiero.transactions.Response<R, REC>>
    void register(@NonNull final TransactionSupport<B, R, REC, RESP> support) {
        Objects.requireNonNull(support, "support must not be null");
        REGISTRY.put(support.getTransactionType(), support);
    }

    /**
     * Returns the {@link TransactionSupport} registered for the given builder type.
     *
     * @param transactionBuilderType the builder type
     * @return the registered {@link TransactionSupport}
     * @throws NoSuchElementException Java mapping of the meta-language
     *                                {@code @@throws(not-found-error)}
     */
    @SuppressWarnings("unchecked")
    @ThreadSafe
    @NonNull
    public static <B extends TransactionBuilder<B, ?, ?, ?>> TransactionSupport<B, ?, ?, ?> getTransactionSupport(
            @NonNull final Class<B> transactionBuilderType) {
        Objects.requireNonNull(transactionBuilderType, "transactionBuilderType must not be null");
        final TransactionSupport<?, ?, ?, ?> support = REGISTRY.get(transactionBuilderType);
        if (support == null) {
            throw new NoSuchElementException(
                    "No TransactionSupport registered for builder type: " + transactionBuilderType.getName());
        }
        return (TransactionSupport<B, ?, ?, ?>) support;
    }

    /**
     * @return an immutable snapshot of all registered {@link TransactionSupport}
     * instances. Mirrors the meta-language {@code set<TransactionSupport> getAllTransactionSupports()}.
     */
    @ThreadSafe
    @NonNull
    public static Set<TransactionSupport<?, ?, ?, ?>> getAllTransactionSupports() {
        return Collections.unmodifiableSet(Set.copyOf(REGISTRY.values()));
    }
}
