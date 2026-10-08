package io.github.drdeathdrop.atlas.user.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "atlas.security.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl) {
}
