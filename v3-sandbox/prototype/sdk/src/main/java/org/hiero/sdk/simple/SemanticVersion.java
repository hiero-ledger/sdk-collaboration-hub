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

    /**
     * Returns a string representation of this semantic version.
     *
     * @return a string in the format "major.minor.patch[-pre][+build]"
     */
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

    /**
     * Compares this version with another version for order.
     *
     * @param o the version to compare to
     * @return negative if this version is less, positive if greater, zero if equal
     */
    @Override
    public int compareTo(@NonNull SemanticVersion o) {
        int result = Integer.compare(major, o.major);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(minor, o.minor);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(patch, o.patch);
        if (result != 0) {
            return result;
        }
        
        return comparePreRelease(o);
    }

    /**
     * Compares pre-release versions.
     *
     * @param other the other version to compare to
     * @return comparison result for pre-release versions
     */
    private int comparePreRelease(@NonNull SemanticVersion other) {
        boolean thisPre = pre != null && !pre.isEmpty();
        boolean otherPre = other.pre != null && !other.pre.isEmpty();

        if (thisPre && !otherPre) {
            return -1;
        }
        if (!thisPre && otherPre) {
            return 1;
        }
        if (!thisPre && !otherPre) {
            return 0;
        }

        // Both are pre-releases, compare lexically
        if (pre == null || other.pre == null) {
            return 0;
        }
        return pre.compareTo(other.pre);
    }
}
