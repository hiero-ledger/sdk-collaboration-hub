package org.hiero.sdk.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method or attribute accessor must be implemented in a
 * thread-safe manner. The SDK may invoke the annotated element concurrently from
 * multiple threads, and the implementation must guarantee correctness under concurrent
 * access.
 *
 * <p>This annotation is the Java mapping of the meta-language {@code @@threadSafe}
 * annotation defined in the
 * <a href="https://github.com/hiero-ledger/sdk-collaboration-hub/blob/main/guides/api-guideline.md">
 * API guideline</a>. See {@code guides/java-files/ThreadSafe.java} for the canonical
 * source.
 *
 * <p>The optional {@link #group()} parameter groups methods and attribute accessors
 * that can be called concurrently with each other. Without a group, the element must
 * only be safe for concurrent calls on its own.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ThreadSafe {

    /**
     * Optional group name. Methods and attribute accessors in the same group can be
     * called concurrently with each other by the SDK. Without a group (empty string),
     * the element must only be safe for concurrent calls on its own.
     *
     * @return the group name, or empty string if ungrouped
     */
    String group() default "";
}
