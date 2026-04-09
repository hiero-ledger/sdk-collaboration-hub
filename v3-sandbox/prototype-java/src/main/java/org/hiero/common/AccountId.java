package org.hiero.common;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

/**
 * The most common type of address on a Hiero network. Mirrors the meta-language type
 * {@code common.AccountId} which extends {@code common.Address}.
 *
 * <p>All fields are {@code @@immutable}, so the type is implemented as a Java record.
 *
 * @param shard    the shard number (interpreted as unsigned)
 * @param realm    the realm number (interpreted as unsigned)
 * @param num      the account number (interpreted as unsigned)
 * @param checksum the checksum string. May be the empty string when no checksum is
 *                 present — never {@code null}.
 */
public record AccountId(long shard, long realm, long num, @NonNull String checksum) implements Address {

    /**
     * Pattern matching {@code shard.realm.num} or {@code shard.realm.num-checksum}.
     * The checksum, if present, is exactly five lowercase ASCII letters.
     */
    private static final Pattern ACCOUNT_ID_PATTERN =
            Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-([a-z]{5}))?$");

    public AccountId {
        Objects.requireNonNull(checksum, "checksum must not be null");
        if (shard < 0L) {
            throw new IllegalArgumentException("shard must be non-negative");
        }
        if (realm < 0L) {
            throw new IllegalArgumentException("realm must be non-negative");
        }
        if (num < 0L) {
            throw new IllegalArgumentException("num must be non-negative");
        }
    }

    /**
     * Convenience constructor for an {@code AccountId} without a checksum.
     */
    public AccountId(final long shard, final long realm, final long num) {
        this(shard, realm, num, "");
    }

    @Override
    public boolean validateChecksum(@NonNull final Ledger ledger) {
        Objects.requireNonNull(ledger, "ledger must not be null");
        // The exact checksum algorithm is ledger-specific and intentionally not part
        // of the V3 prototype — see REPORT.md "Checksum algorithm".
        throw new UnsupportedOperationException("Checksum validation is not implemented in the prototype");
    }

    @Override
    @NonNull
    public String toString() {
        return shard + "." + realm + "." + num;
    }

    @Override
    @NonNull
    public String toStringWithChecksum() {
        if (checksum.isEmpty()) {
            return toString();
        }
        return toString() + "-" + checksum;
    }

    /**
     * Parses an {@code AccountId} from a string in {@code shard.realm.num} or
     * {@code shard.realm.num-checksum} format. This is the Java mapping of the
     * meta-language factory {@code @@throws(illegal-format) AccountId fromString(...)}.
     *
     * @param accountId the string representation of the account id
     * @return the parsed {@link AccountId}
     * @throws IllegalArgumentException if the format is invalid (Java mapping of
     *                                  {@code illegal-format})
     */
    @NonNull
    public static AccountId fromString(@NonNull final String accountId) {
        Objects.requireNonNull(accountId, "accountId must not be null");
        final Matcher m = ACCOUNT_ID_PATTERN.matcher(accountId);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid account id format: " + accountId);
        }
        try {
            final long shard = Long.parseLong(m.group(1));
            final long realm = Long.parseLong(m.group(2));
            final long num = Long.parseLong(m.group(3));
            final String checksum = m.group(4) == null ? "" : m.group(4);
            return new AccountId(shard, realm, num, checksum);
        } catch (final NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid account id format: " + accountId, ex);
        }
    }
}
