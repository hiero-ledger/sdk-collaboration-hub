package org.hiero.sdk.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method or attribute accessor must be implemented in a thread-safe manner.
 * The SDK may invoke the annotated element concurrently from multiple threads, and the implementation must
 * guarantee correctness under concurrent access.
 *
 * <p>When applied to an <strong>interface method</strong>, it documents a contract: any implementation of
 * this method must be thread-safe. When applied to a <strong>concrete method</strong> or
 * <strong>field accessor</strong> (getter/setter), it indicates that the implementation is thread-safe.
 *
 * <p>The optional {@link #group()} parameter groups methods and attribute accessors that can be called
 * concurrently with each other. The SDK may invoke any combination of elements in the same group at
 * the same time. Without a group, the element must only be safe for concurrent calls on its own.
 *
 * <p><strong>Implementation guidance (in order of preference):</strong>
 * <ol>
 *   <li>Immutable snapshots — return defensive copies so no synchronization is needed</li>
 *   <li>{@code volatile} fields — for simple flags or single references</li>
 *   <li>{@code java.util.concurrent.atomic.*} — for single-field atomic updates</li>
 *   <li>Concurrent collections — {@code ConcurrentHashMap}, {@code CopyOnWriteArrayList}</li>
 *   <li>{@code java.util.concurrent.locks.Lock} — for multi-field invariants; prefer over
 *       {@code synchronized}</li>
 *   <li>{@code synchronized} — last resort for simple cases where a {@code Lock} adds no benefit</li>
 * </ol>
 *
 * <p>Blocking mechanisms ({@code synchronized}, {@code Lock}) should only be used when non-blocking
 * alternatives cannot express the required invariant.
 *
 * <p><strong>Example without group:</strong>
 * <pre>{@code
 * public interface StatsService {
 *
 *     @ThreadSafe
 *     void resetStats();
 * }
 * }</pre>
 *
 * <p><strong>Example with group:</strong>
 * <pre>{@code
 * public interface DataCache {
 *
 *     @ThreadSafe(group = "cache")
 *     void updateCache(byte[] data);
 *
 *     @ThreadSafe(group = "cache")
 *     byte[] readCache();
 *
 *     @ThreadSafe
 *     void resetStats();
 * }
 * }</pre>
 *
 * <p>In this example, {@code updateCache} and {@code readCache} are in the {@code "cache"} group,
 * meaning the SDK may call them concurrently with each other. {@code resetStats} has no group and
 * must only be safe for concurrent calls on its own.
 *
 * @see <a href="https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/guides/api-guideline.md">
 *     API Guideline — @@threadSafe</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ThreadSafe {

    /**
     * Optional group name. Methods and attribute accessors in the same group can be called concurrently
     * with each other by the SDK. Without a group (empty string), the element must only be safe for
     * concurrent calls on its own.
     *
     * @return the group name, or empty string if ungrouped
     */
    String group() default "";
}