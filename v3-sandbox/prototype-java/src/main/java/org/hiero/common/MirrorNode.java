package org.hiero.common;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Represents a mirror node on a network. Mirrors the meta-language type
 * {@code common.MirrorNode}.
 *
 * @param restBaseUrl base url of the mirror node REST API in
 *                    {@code scheme://host[:port]/api/v1} format
 */
public record MirrorNode(@NonNull String restBaseUrl) {

    public MirrorNode {
        Objects.requireNonNull(restBaseUrl, "restBaseUrl must not be null");
    }
}
