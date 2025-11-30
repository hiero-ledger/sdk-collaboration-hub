package org.hiero.sdk.simple;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A software version according to <a href="https://semver.org/">semantic versioning</a>.
 *
 * @param major the major version
 * @param minor the minor version
 * @param patch the patch version
 * @param pre   the pre-release version
 * @param build the build metadata
 */
public record SemanticVersion(int major, int minor, int patch, @Nullable String pre, @Nullable String build) implements Comparable<SemanticVersion> {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append(".").append(minor).append(".").append(patch);
        if (pre != null && !pre.isEmpty()) {
            sb.append("-").append(pre);
        }
        if (build != null && !build.isEmpty()) {
            sb.append("+").append(build);
        }
        return sb.toString();
    }

    @Override
    public int compareTo(@NonNull SemanticVersion o) {
        int result = Integer.compare(major, o.major);
        if (result != 0) return result;
        result = Integer.compare(minor, o.minor);
        if (result != 0) return result;
        result = Integer.compare(patch, o.patch);
        if (result != 0) return result;
        
        // Pre-release versions have a lower precedence than the associated normal version.
        // A pre-release version is indicated by a non-empty pre string.
        boolean thisPre = pre != null && !pre.isEmpty();
        boolean otherPre = o.pre != null && !o.pre.isEmpty();

        if (thisPre && !otherPre) return -1;
        if (!thisPre && otherPre) return 1;
        if (!thisPre && !otherPre) return 0;

        // Both are pre-releases, compare lexically
        return pre.compareTo(o.pre);
    }
}
