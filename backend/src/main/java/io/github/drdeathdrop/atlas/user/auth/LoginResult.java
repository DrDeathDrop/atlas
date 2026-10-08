package io.github.drdeathdrop.atlas.user.auth;

import io.github.drdeathdrop.atlas.user.session.IssuedRefreshToken;

/**
 * What a successful login or refresh produces: the access token for the
 * response body, and the refresh token that goes into a cookie.
 */
public record LoginResult(TokenResponse tokens, IssuedRefreshToken refreshToken) {
}
