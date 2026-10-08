package io.github.drdeathdrop.atlas.user.session;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings under {@code atlas.security.refresh-token} in application.yaml.
 */
@ConfigurationProperties(prefix = "atlas.security.refresh-token")
public record RefreshTokenProperties(Duration ttl) {
}
