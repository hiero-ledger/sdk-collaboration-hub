package org.hiero.sdk.validation;

/**
 * Utility class for validating unsigned integer semantics in Java.
 *
 * <p>Java does not have native unsigned integer types. The Hiero SDK maps unsigned meta-language
 * types ({@code uint8}, {@code uint16}, {@code uint64}) to signed Java primitives ({@code byte},
 * {@code short}/{@code int}, {@code long}). This class enforces unsigned semantics by rejecting
 * negative values and values that exceed the unsigned range.
 *
 * <p>All methods throw {@link IllegalArgumentException} if the value is outside the valid range.
 * Values are never silently clamped or transformed.
 *
 * <p><strong>Ranges:</strong>
 * <ul>
 *   <li>{@code uint8}  — 0 to 255</li>
 *   <li>{@code uint16} — 0 to 65535</li>
 *   <li>{@code uint64} — 0 to {@link Long#MAX_VALUE} (negative {@code long} values are rejected)</li>
 * </ul>
 *
 * <p><strong>Usage example:</strong>
 * <pre>{@code
 * public class ConsensusNode {
 *
 *     private final int port;
 *
 *     public ConsensusNode(final int port) {
 *         this.port = UnsignedValidator.requireUint16(port, "port");
 *     }
 * }
 * }</pre>
 *
 * @see <a href="https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/guides/api-best-practices-java.md">
 *     Java API Implementation Guideline — Unsigned Integer Semantics</a>
 */
public final class UnsignedValidator {

    /**
     * Maximum value for an unsigned 8-bit integer.
     */
    public static final int UINT8_MAX = 255;

    /**
     * Maximum value for an unsigned 16-bit integer.
     */
    public static final int UINT16_MAX = 65_535;

    private UnsignedValidator() {
        // Prevent instantiation
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Validates that the given value is within the unsigned 8-bit integer range (0 to 255).
     *
     * <p>This method should be used at input boundaries (constructors, setters, builder methods)
     * for fields mapped from the meta-language type {@code uint8}.
     *
     * @param value the value to validate
     * @param name  the name of the parameter, used in the exception message
     * @return the validated value, for convenient inline use
     * @throws IllegalArgumentException if the value is negative or greater than 255
     */
    public static int requireUint8(final int value, final String name) {
        if (value < 0 || value > UINT8_MAX) {
            throw new IllegalArgumentException(
                    name + " must be in the range 0 to " + UINT8_MAX + " (uint8), but was: " + value
            );
        }
        return value;
    }

    /**
     * Validates that the given value is within the unsigned 16-bit integer range (0 to 65535).
     *
     * <p>This method should be used at input boundaries (constructors, setters, builder methods)
     * for fields mapped from the meta-language type {@code uint16}.
     *
     * @param value the value to validate
     * @param name  the name of the parameter, used in the exception message
     * @return the validated value, for convenient inline use
     * @throws IllegalArgumentException if the value is negative or greater than 65535
     */
    public static int requireUint16(final int value, final String name) {
        if (value < 0 || value > UINT16_MAX) {
            throw new IllegalArgumentException(
                    name + " must be in the range 0 to " + UINT16_MAX + " (uint16), but was: " + value
            );
        }
        return value;
    }

    /**
     * Validates that the given value is non-negative, enforcing unsigned 64-bit integer semantics.
     *
     * <p>Java's {@code long} can represent values from {@link Long#MIN_VALUE} to
     * {@link Long#MAX_VALUE}. Since {@code long} cannot represent the full {@code uint64} range
     * (0 to 2^64 - 1), this method enforces the maximum representable unsigned range: 0 to
     * {@link Long#MAX_VALUE}. Any negative value is rejected.
     *
     * <p>This method should be used at input boundaries (constructors, setters, builder methods)
     * for fields mapped from the meta-language type {@code uint64}.
     *
     * @param value the value to validate
     * @param name  the name of the parameter, used in the exception message
     * @return the validated value, for convenient inline use
     * @throws IllegalArgumentException if the value is negative
     */
    public static long requireUint64(final long value, final String name) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    name + " must be non-negative (uint64), but was: " + value
            );
        }
        return value;
    }
}
