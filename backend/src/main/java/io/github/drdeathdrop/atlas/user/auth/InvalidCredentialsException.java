package io.github.drdeathdrop.atlas.user.auth;

/**
 * Thrown for any failed login. It deliberately does not say whether the
 * email was unknown, the password wrong or the account disabled, so a
 * caller cannot use the login endpoint to find out which emails exist.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
