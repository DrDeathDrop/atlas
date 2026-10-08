package io.github.drdeathdrop.atlas.user.auth;

import io.github.drdeathdrop.atlas.user.session.IssuedRefreshToken;

public record LoginResult(TokenResponse tokens, IssuedRefreshToken refreshToken) {
}
