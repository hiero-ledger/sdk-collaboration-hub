/*
 * Single JPMS module that hosts all of the V3 prototype namespaces.
 *
 * NOTE: The Java best-practice guide recommends one JPMS module per meta-language
 * namespace. The prototype intentionally collapses all namespaces into a single
 * module to keep the build simple. The cleaner module split is documented in
 * REPORT.md as a deferred follow-up.
 */
module org.hiero.sdk.prototype {

    // Compile-time only — JSpecify nullability annotations are erased after compile.
    requires static org.jspecify;

    // Runtime — used internally by the keys impl package only, never exposed in
    // the public API. The public API only ever exposes our own types.
    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;

    // ---- Public API namespaces ----
    exports org.hiero.sdk.annotation;
    exports org.hiero.common;
    exports org.hiero.config;
    exports org.hiero.keys;
    exports org.hiero.keys.io;
    exports org.hiero.client;
    exports org.hiero.transactions;
    exports org.hiero.transactions.accounts;
    exports org.hiero.transactions.spi;

    // ---- Internal `impl` packages are intentionally NOT exported ----
}
