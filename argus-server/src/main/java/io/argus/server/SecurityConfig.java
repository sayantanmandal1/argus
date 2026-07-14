package io.argus.server;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable security policy for {@link ArgusServer}.
 *
 * <ul>
 *   <li><b>apiKeys</b> — bearer tokens accepted on write endpoints; empty disables authentication</li>
 *   <li><b>protectReads</b> — whether search/stats also require a token</li>
 *   <li><b>maxBodyBytes</b> — request body cap (defends against memory-exhaustion DoS)</li>
 *   <li><b>rateLimitPerMinute</b> — per-client request cap (0 = unlimited)</li>
 *   <li><b>allowedOrigin</b> — a single CORS origin, or {@code null} for same-origin only</li>
 * </ul>
 */
public final class SecurityConfig {

    private final Set<String> apiKeys;
    private final boolean protectReads;
    private final int maxBodyBytes;
    private final int rateLimitPerMinute;
    private final String allowedOrigin;

    private SecurityConfig(Builder b) {
        this.apiKeys = Collections.unmodifiableSet(new HashSet<>(b.apiKeys));
        this.protectReads = b.protectReads;
        this.maxBodyBytes = b.maxBodyBytes;
        this.rateLimitPerMinute = b.rateLimitPerMinute;
        this.allowedOrigin = b.allowedOrigin;
    }

    public Set<String> apiKeys() {
        return apiKeys;
    }

    public boolean protectReads() {
        return protectReads;
    }

    public int maxBodyBytes() {
        return maxBodyBytes;
    }

    public int rateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public String allowedOrigin() {
        return allowedOrigin;
    }

    /** Permissive configuration for local development: no auth, generous limits. */
    public static SecurityConfig dev() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link SecurityConfig}. */
    public static final class Builder {
        private final Set<String> apiKeys = new HashSet<>();
        private boolean protectReads = false;
        private int maxBodyBytes = 1 << 20; // 1 MiB
        private int rateLimitPerMinute = 0; // unlimited
        private String allowedOrigin = null;

        public Builder apiKey(String key) {
            if (key != null && !key.isBlank()) {
                apiKeys.add(key);
            }
            return this;
        }

        public Builder protectReads(boolean protectReads) {
            this.protectReads = protectReads;
            return this;
        }

        public Builder maxBodyBytes(int maxBodyBytes) {
            this.maxBodyBytes = Math.max(1, maxBodyBytes);
            return this;
        }

        public Builder rateLimitPerMinute(int rateLimitPerMinute) {
            this.rateLimitPerMinute = Math.max(0, rateLimitPerMinute);
            return this;
        }

        public Builder allowedOrigin(String allowedOrigin) {
            this.allowedOrigin = allowedOrigin;
            return this;
        }

        public SecurityConfig build() {
            return new SecurityConfig(this);
        }
    }
}
