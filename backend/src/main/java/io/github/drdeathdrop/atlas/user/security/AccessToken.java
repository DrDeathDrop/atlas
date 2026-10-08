package io.github.drdeathdrop.atlas.user.security;

public record AccessToken(String value, long expiresInSeconds) {
}
