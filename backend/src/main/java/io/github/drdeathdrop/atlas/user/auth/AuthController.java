package io.github.drdeathdrop.atlas.user.auth;

import io.github.drdeathdrop.atlas.user.UserSummary;
import io.github.drdeathdrop.atlas.user.session.InvalidRefreshTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
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
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        LoginResult result = authService.login(
                request.email(), request.password(), userAgent(httpRequest), httpRequest.getRemoteAddr());

        return withRefreshCookie(result, httpRequest);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        LoginResult result = authService.refresh(refreshToken, userAgent(httpRequest), httpRequest.getRemoteAddr());

        return withRefreshCookie(result, httpRequest);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, RefreshTokenCookie.cleared(httpRequest.isSecure()).toString())
                .build();
    }

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal Jwt jwt) {
        return authService.currentUser(UUID.fromString(jwt.getSubject()));
    }

    private static ResponseEntity<TokenResponse> withRefreshCookie(LoginResult result, HttpServletRequest httpRequest) {
        String cookie = RefreshTokenCookie.of(result.refreshToken(), httpRequest.isSecure()).toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie)
                .body(result.tokens());
    }

    private static String userAgent(HttpServletRequest httpRequest) {
        return httpRequest.getHeader(HttpHeaders.USER_AGENT);
    }
}
