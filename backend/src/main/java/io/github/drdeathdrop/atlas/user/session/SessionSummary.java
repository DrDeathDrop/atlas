package io.github.drdeathdrop.atlas.user.session;

import java.time.Instant;
import java.util.UUID;

public record SessionSummary(UUID id, Instant createdAt, Instant expiresAt, String userAgent, String ipAddress) {
}
