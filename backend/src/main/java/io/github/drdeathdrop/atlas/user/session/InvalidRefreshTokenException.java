package io.github.drdeathdrop.atlas.user.session;

/**
 * The refresh token is missing, unknown, expired or revoked. The caller has
 * to log in again.
 */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("The session is no longer valid. Please log in again.");
    }
}
