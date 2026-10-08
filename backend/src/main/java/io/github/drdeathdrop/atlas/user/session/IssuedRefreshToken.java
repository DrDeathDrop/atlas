package io.github.drdeathdrop.atlas.user.session;

import java.time.Instant;
import java.util.UUID;

public record IssuedRefreshToken(UUID userId, String value, Instant expiresAt) {
}
