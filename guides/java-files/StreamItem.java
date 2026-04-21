package org.hiero.sdk;

import java.util.Objects;

/**
 * A sealed result type representing a single item in a {@link HieroStream} that is either a success
 * value or an error. This is the Java mapping of the meta-language {@code streamResult<TYPE>}.
 *
 * <p>{@code StreamItem} enables per-item error handling in streams without terminating the stream.
 * When the SDK encounters a problem with an individual item (e.g., a malformed message, a parse error,
 * or invalid data), it yields a {@link Error} variant instead of throwing an exception. The stream
 * continues producing subsequent items. Terminal stream-level errors (connection lost, authorization
 * revoked) are not represented as {@code StreamItem} — they are thrown as exceptions from the
 * stream's iterator.
 *
 * <p><strong>Error levels in streams:</strong>
 * <ul>
 *   <li><strong>Per-item errors (non-terminal):</strong> Represented as {@link Error} variants.
 *       The stream continues. The consumer decides how to handle each error (skip, log, collect, abort).</li>
 *   <li><strong>Stream-level errors (terminal):</strong> Thrown as exceptions from
 *       {@link java.util.Iterator#next()} or {@link java.util.Iterator#hasNext()}. The stream is over.</li>
 * </ul>
 *
 * <p><strong>Usage with pattern matching (Java 17+):</strong>
 * <pre>{@code
 * try (HieroStream<StreamItem<TopicMessage>> stream = subscription.subscribe(client)) {
 *     for (StreamItem<TopicMessage> item : stream) {
 *         switch (item) {
 *             case StreamItem.Success<TopicMessage> s -> process(s.value());
 *             case StreamItem.Error<TopicMessage> e -> {
 *                 if (e.error() instanceof ThrottleException throttle) {
 *                     log.info("Throttled, retryAfter={}", throttle.getRetryAfter());
 *                 } else {
 *                     log.warn("Item error: {}", e.error().getMessage());
 *                 }
 *             }
 *         }
 *     }
 * } catch (TopicDeletedException ex) {
 *     // Terminal stream-level error
 *     log.error("Topic was deleted", ex);
 * }
 * }</pre>
 *
 * @param <T> the type of the success value
 * @see HieroStream
 * @see <a href="https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/guides/api-guideline.md">
 *     API Guideline — streamResult</a>
 */
public sealed interface StreamItem<T> permits StreamItem.Success, StreamItem.Error {

    /**
     * A successful stream item containing a value.
     *
     * @param value the non-null success value
     * @param <T>   the type of the success value
     */
    record Success<T>(@NonNull T value) implements StreamItem<T> {

        public Success {
            Objects.requireNonNull(value, "value must not be null");
        }
    }

    /**
     * A failed stream item containing an error. The stream continues after yielding this item —
     * the error is non-terminal. The consumer decides whether to skip, log, collect, or abort.
     *
     * @param error the non-null error that caused this item to fail
     * @param <T>   the type of the success value (not present in this variant)
     */
    record Error<T>(@NonNull Throwable error) implements StreamItem<T> {

        public Error {
            Objects.requireNonNull(error, "error must not be null");
        }
    }
}