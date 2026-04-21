package org.hiero.sdk;

import java.util.Iterator;
import java.util.Objects;

/**
 * A pull-based asynchronous stream of items. This is the primary streaming interface in the Hiero SDK.
 * Consumers iterate over items using a standard {@code for} loop or {@link Iterator}, and the stream
 * is cancelled and cleaned up by calling {@link #close()} — typically via try-with-resources.
 *
 * <p>{@code HieroStream} extends {@link Iterable} so it can be used in enhanced {@code for} loops,
 * and {@link AutoCloseable} so it integrates with try-with-resources for automatic resource cleanup.
 * When closed, the stream releases all underlying resources (gRPC connections, network subscriptions,
 * internal buffers, etc.).
 *
 * <p>The {@link Iterator} returned by {@link #iterator()} blocks on {@link Iterator#next()} until
 * the next item is available. On Java 21+, this is efficient when consumed on a virtual thread —
 * the virtual thread parks without blocking an OS thread. On older Java versions, the consumer
 * thread blocks, which is acceptable for most use cases since stream processing typically dedicates
 * a thread to consumption.
 *
 * <p>A {@code HieroStream} must only be iterated once. Calling {@link #iterator()} a second time
 * must throw {@link IllegalStateException}.
 *
 * <p><strong>Pull-based usage (primary):</strong>
 * <pre>{@code
 * try (HieroStream<StreamItem<TopicMessage>> stream = subscription.subscribe(client)) {
 *     for (StreamItem<TopicMessage> item : stream) {
 *         switch (item) {
 *             case StreamItem.Success<TopicMessage> s -> process(s.value());
 *             case StreamItem.Error<TopicMessage> e -> log.warn("Bad message", e.error());
 *         }
 *     }
 * } // close() cancels the stream and releases resources
 * }</pre>
 *
 * <p><strong>Push-based usage (convenience adapter):</strong>
 * <p>A {@code HieroStream} can be wrapped in a {@link HieroPublisher} to obtain a
 * {@link java.util.concurrent.Flow.Publisher} for push-based consumption via the standard
 * Reactive Streams protocol. The push adapter contains no domain logic — all retry, reconnect,
 * and error handling logic lives in the pull-based {@code HieroStream} implementation.
 *
 * <p><strong>Thread safety:</strong> A {@code HieroStream} is not thread-safe. It must be consumed
 * by a single thread (or a single virtual thread). Concurrent iteration from multiple threads
 * produces undefined behavior.
 *
 * @param <T> the type of elements in the stream. When per-item error handling is needed, this is
 *            typically {@link StreamItem StreamItem&lt;V&gt;} where {@code V} is the value type.
 * @see StreamItem
 * @see HieroPublisher
 * @see <a href="https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/guides/api-guideline.md">
 *     API Guideline — @@streaming</a>
 */
public interface HieroStream<T> extends Iterable<T>, AutoCloseable {

    /**
     * Returns an iterator over the items in this stream. The iterator blocks on {@link Iterator#next()}
     * until the next item is available or the stream ends.
     *
     * <p>This method must only be called once. A second call must throw {@link IllegalStateException}
     * to prevent multiple consumers from reading the same stream concurrently.
     *
     * <p>The iterator signals stream completion by returning {@code false} from {@link Iterator#hasNext()}.
     * Terminal stream-level errors are thrown as exceptions from {@link Iterator#hasNext()} or
     * {@link Iterator#next()}.
     *
     * @return a non-null iterator over the stream items
     * @throws IllegalStateException if {@code iterator()} has already been called on this stream
     */
    @Override
    Iterator<T> iterator();

    /**
     * Cancels the stream and releases all underlying resources. After this method returns, no further
     * items will be produced and the underlying network connection (gRPC stream, WebSocket, etc.) is
     * closed.
     *
     * <p>This method is idempotent — calling it multiple times has no additional effect. It does not
     * throw exceptions. If the stream has already completed naturally, calling {@code close()} is a
     * no-op.
     *
     * <p>When used with try-with-resources, {@code close()} is called automatically when the block
     * exits, whether normally or due to an exception.
     */
    @Override
    void close();
}