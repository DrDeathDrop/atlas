package io.github.drdeathdrop.atlas.user.account;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException(String email) {
        super("A user with email " + email + " already exists");
    }
}
