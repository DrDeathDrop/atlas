package io.github.drdeathdrop.atlas.user.session;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Lets a user see where they are logged in and end any of those sessions.
 * The user is always taken from the verified access token.
 */
@RestController
@RequestMapping("/api/auth/sessions")
public class SessionController {

    private final RefreshTokenService refreshTokenService;

    public SessionController(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @GetMapping
    public List<SessionSummary> list(@AuthenticationPrincipal Jwt jwt) {
        return refreshTokenService.activeSessions(UUID.fromString(jwt.getSubject()));
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId) {
        refreshTokenService.revokeSession(UUID.fromString(jwt.getSubject()), sessionId);
    }
}
