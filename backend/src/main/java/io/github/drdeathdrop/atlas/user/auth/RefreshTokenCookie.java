package io.github.drdeathdrop.atlas.user.auth;

import io.github.drdeathdrop.atlas.user.session.IssuedRefreshToken;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.time.Instant;

final class RefreshTokenCookie {
    static final String NAME = "atlas_refresh";
    private static final String PATH = "/api/auth";

    private RefreshTokenCookie() {
    }

    static ResponseCookie of(IssuedRefreshToken token, boolean secure) {
        Duration lifetime = Duration.between(Instant.now(), token.expiresAt());
        return base(token.value(), secure)
                .maxAge(lifetime.isNegative() ? Duration.ZERO : lifetime)
                .build();
    }

    static ResponseCookie cleared(boolean secure) {
        return base("", secure).maxAge(Duration.ZERO).build();
    }

    private static ResponseCookie.ResponseCookieBuilder base(String value, boolean secure) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH);
    }
}
