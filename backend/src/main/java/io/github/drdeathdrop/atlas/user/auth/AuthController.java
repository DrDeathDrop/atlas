package io.github.drdeathdrop.atlas.user.auth;

import io.github.drdeathdrop.atlas.user.UserSummary;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    /**
     * Returns the logged-in user. Who that is comes from the verified token,
     * never from anything the caller puts in the URL or the body.
     */
    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal Jwt jwt) {
        return authService.currentUser(UUID.fromString(jwt.getSubject()));
    }
}
