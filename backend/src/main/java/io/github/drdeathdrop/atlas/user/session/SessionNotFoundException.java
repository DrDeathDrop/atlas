package io.github.drdeathdrop.atlas.user.session;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException() {
        super("Session not found");
    }
}
