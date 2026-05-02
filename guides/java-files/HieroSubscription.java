package org.hiero.sdk;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@link Flow.Subscription} implementation that drives a pull-based {@link HieroStream} on a virtual
 * thread and delivers items to a {@link Flow.Subscriber} with backpressure support. This class is the
 * bridge between the pull-based and push-based streaming models in the Hiero SDK.
 *
 * <p>This class is package-private and not part of the public API. It is used internally by
 * {@link HieroPublisher} to adapt a {@code HieroStream} into a {@code Flow.Publisher}.
 *
 * <p><strong>Backpressure:</strong> Items are only delivered to the subscriber when there is outstanding
 * demand via {@link #request(long)}. If the subscriber has not requested any items, the virtual thread
 * parks until demand becomes available or the subscription is cancelled. This ensures the subscriber
 * is never overwhelmed by a fast producer.
 *
 * <p><strong>Cancellation:</strong> Calling {@link #cancel()} closes the underlying {@code HieroStream}
 * and stops the virtual thread. The cancellation is cooperative — the drain loop checks the cancelled
 * flag before delivering each item. Cancellation is idempotent.
 *
 * <p><strong>Terminal signals:</strong>
 * <ul>
 *   <li>When the stream completes naturally (iterator exhausted), {@link Flow.Subscriber#onComplete()}
 *       is called.</li>
 *   <li>When the stream throws an exception (terminal stream-level error),
 *       {@link Flow.Subscriber#onError(Throwable)} is called.</li>
 *   <li>Neither terminal signal is sent after cancellation.</li>
 * </ul>
 *
 * <p><strong>Threading:</strong> The drain loop runs on a virtual thread (Java 21+). The virtual thread
 * parks cheaply when waiting for demand, without blocking an OS thread. The subscriber's
 * {@code onNext}, {@code onError}, and {@code onComplete} methods are called from this virtual thread.
 *
 * @param <T> the type of elements delivered to the subscriber
 * @see HieroPublisher
 * @see HieroStream
 * @see <a href="https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/guides/api-guideline.md">
 *     API Guideline — @@streaming</a>
 */
final class HieroSubscription<T> implements Flow.Subscription {

    private final HieroStream<T> stream;
    private final Flow.Subscriber<? super T> subscriber;
    private final AtomicLong requested = new AtomicLong(0);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * Creates a new subscription and immediately starts a virtual thread to drive the pull loop.
     *
     * @param stream     the pull-based stream to drain; must not be null
     * @param subscriber the subscriber to deliver items to; must not be null
     */
    HieroSubscription(@NonNull final HieroStream<T> stream,
                      @NonNull final Flow.Subscriber<? super T> subscriber) {
        this.stream = Objects.requireNonNull(stream, "stream must not be null");
        this.subscriber = Objects.requireNonNull(subscriber, "subscriber must not be null");
        Thread.ofVirtual().name("hiero-stream-publisher").start(this::drainLoop);
    }

    /**
     * Requests {@code n} more items from the stream. The drain loop will deliver up to {@code n}
     * additional items to the subscriber before waiting for more demand.
     *
     * <p>If {@code n} is not positive, the subscription is cancelled and
     * {@link Flow.Subscriber#onError(Throwable)} is called with an {@link IllegalArgumentException},
     * as required by the Reactive Streams specification.
     *
     * @param n the number of items to request; must be positive
     */
    @Override
    public void request(final long n) {
        if (n <= 0) {
            cancel();
            subscriber.onError(new IllegalArgumentException(
                    "Flow.Subscription.request requires a positive count, got: " + n));
            return;
        }
        requested.addAndGet(n);
    }

    /**
     * Cancels the subscription. Closes the underlying {@link HieroStream} and signals the drain loop
     * to stop. After cancellation, no further items or terminal signals are delivered to the subscriber.
     *
     * <p>This method is idempotent — calling it multiple times has no additional effect. It is safe
     * to call from any thread.
     */
    @Override
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            stream.close();
        }
    }

    /**
     * The main loop that pulls items from the {@link HieroStream} and delivers them to the subscriber.
     * This method runs on a virtual thread started in the constructor.
     *
     * <p>The loop respects backpressure by waiting for outstanding demand before delivering each item.
     * It checks the cancellation flag before each delivery to ensure prompt shutdown.
     */
    private void drainLoop() {
        try {
            final Iterator<T> iterator = stream.iterator();
            while (iterator.hasNext()) {
                while (requested.get() <= 0) {
                    if (cancelled.get()) {
                        return;
                    }
                    Thread.sleep(1); // virtual thread parks cheaply
                }
                if (cancelled.get()) {
                    return;
                }
                final T item = iterator.next();
                requested.decrementAndGet();
                subscriber.onNext(item);
            }
            if (!cancelled.get()) {
                subscriber.onComplete();
            }
        } catch (final Exception e) {
            if (!cancelled.get()) {
                subscriber.onError(e);
            }
        }
    }
}
