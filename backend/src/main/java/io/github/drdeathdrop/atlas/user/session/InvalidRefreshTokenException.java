package io.github.drdeathdrop.atlas.user.session;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("The session is no longer valid. Please log in again.");
    }
}
