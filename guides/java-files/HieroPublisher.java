package org.hiero.sdk;

import java.util.Objects;
import java.util.concurrent.Flow;

/**
 * A push-based adapter that wraps a pull-based {@link HieroStream} as a
 * {@link java.util.concurrent.Flow.Publisher}. This enables consumers to use the standard
 * Reactive Streams protocol for push-based consumption, while all domain logic (retry, reconnect,
 * error handling) remains in the underlying pull-based {@code HieroStream}.
 *
 * <p>This adapter contains <strong>no domain logic</strong>. It drives the pull loop on a virtual
 * thread (Java 21+) and delivers items to the {@link Flow.Subscriber} according to the
 * Reactive Streams specification. Backpressure is respected through the
 * {@link Flow.Subscription#request(long)} protocol — items are only delivered when the subscriber
 * has outstanding demand.
 *
 * <p><strong>Architecture:</strong>
 * <pre>
 * Network (gRPC/WebSocket)
 *     → SDK internal retry/reconnect
 *         → HieroStream&lt;T&gt; (pull — canonical implementation)
 *             → HieroPublisher&lt;T&gt; (push adapter — this class)
 *                 → Flow.Subscriber&lt;T&gt; (consumer)
 * </pre>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 * HieroStream<StreamItem<TopicMessage>> pullStream = subscription.subscribe(client);
 * Flow.Publisher<StreamItem<TopicMessage>> publisher = new HieroPublisher<>(pullStream);
 *
 * publisher.subscribe(new Flow.Subscriber<>() {
 *     private Flow.Subscription subscription;
 *
 *     @Override
 *     public void onSubscribe(Flow.Subscription subscription) {
 *         this.subscription = subscription;
 *         subscription.request(1);
 *     }
 *
 *     @Override
 *     public void onNext(StreamItem<TopicMessage> item) {
 *         switch (item) {
 *             case StreamItem.Success<TopicMessage> s -> process(s.value());
 *             case StreamItem.Error<TopicMessage> e -> log.warn("Bad message", e.error());
 *         }
 *         subscription.request(1);
 *     }
 *
 *     @Override
 *     public void onError(Throwable throwable) {
 *         log.error("Stream failed", throwable);
 *     }
 *
 *     @Override
 *     public void onComplete() {
 *         log.info("Stream completed");
 *     }
 * });
 * }</pre>
 *
 * <p><strong>Reactive library integration:</strong>
 * <p>Since this class implements {@link Flow.Publisher}, it integrates directly with any reactive
 * library that supports the Reactive Streams specification:
 * <ul>
 *   <li>Project Reactor: {@code JdkFlowAdapter.flowPublisherToFlux(publisher)}</li>
 *   <li>RxJava 3: {@code FlowInterop.fromFlowPublisher(publisher)}</li>
 * </ul>
 *
 * <p><strong>Thread safety:</strong> Each call to {@link #subscribe(Flow.Subscriber)} creates an
 * independent subscription with its own virtual thread. The publisher itself is stateless and can
 * be shared across threads. However, the underlying {@link HieroStream} supports only a single
 * subscriber — calling {@code subscribe} more than once on the same publisher will fail because
 * the stream's iterator can only be obtained once.
 *
 * @param <T> the type of elements published. When per-item error handling is needed, this is
 *            typically {@link StreamItem StreamItem&lt;V&gt;} where {@code V} is the value type.
 * @see HieroStream
 * @see StreamItem
 * @see <a href="https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/guides/api-guideline.md">
 *     API Guideline — @@streaming</a>
 */
public final class HieroPublisher<T> implements Flow.Publisher<T> {

    private final HieroStream<T> stream;

    /**
     * Creates a new publisher that wraps the given pull-based stream.
     *
     * @param stream the pull-based stream to adapt; must not be null
     * @throws NullPointerException if {@code stream} is null
     */
    public HieroPublisher(@NonNull final HieroStream<T> stream) {
        this.stream = Objects.requireNonNull(stream, "stream must not be null");
    }

    /**
     * Subscribes the given subscriber to this publisher. A virtual thread is started to drive the
     * pull loop and deliver items to the subscriber according to the Reactive Streams protocol.
     *
     * <p>The subscriber's {@link Flow.Subscriber#onSubscribe(Flow.Subscription)} method is called
     * synchronously before this method returns, providing the subscription through which the
     * subscriber can request items and cancel.
     *
     * @param subscriber the subscriber to receive items; must not be null
     * @throws NullPointerException if {@code subscriber} is null
     */
    @Override
    public void subscribe(@NonNull final Flow.Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber must not be null");
        final HieroSubscription<T> subscription = new HieroSubscription<>(stream, subscriber);
        subscriber.onSubscribe(subscription);
    }
}