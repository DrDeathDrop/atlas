package io.github.drdeathdrop.atlas.user.security;

/**
 * A signed access token and how long it stays valid.
 */
public record AccessToken(String value, long expiresInSeconds) {
}
