package io.github.drdeathdrop.atlas.user.session;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "atlas.security.refresh-token")
public record RefreshTokenProperties(Duration ttl) {
}
