package io.github.drdeathdrop.atlas.user.session;

import java.time.Instant;
import java.util.UUID;

/**
 * A freshly created refresh token. This is the only moment the real value
 * exists on the server; after it is sent to the client only the hash remains.
 */
public record IssuedRefreshToken(UUID userId, String value, Instant expiresAt) {
}
