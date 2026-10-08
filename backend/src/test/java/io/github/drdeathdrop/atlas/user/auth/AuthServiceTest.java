package io.github.drdeathdrop.atlas.user.auth;

import io.github.drdeathdrop.atlas.user.Role;
import io.github.drdeathdrop.atlas.user.account.User;
import io.github.drdeathdrop.atlas.user.account.UserRepository;
import io.github.drdeathdrop.atlas.user.security.AccessToken;
import io.github.drdeathdrop.atlas.user.security.TokenService;
import io.github.drdeathdrop.atlas.user.session.InvalidRefreshTokenException;
import io.github.drdeathdrop.atlas.user.session.IssuedRefreshToken;
import io.github.drdeathdrop.atlas.user.session.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final String AGENT = "JUnit";
    private static final String IP = "127.0.0.1";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, tokenService, refreshTokenService);
    }

    @Test
    void loginReturnsBothTokensForCorrectCredentials() {
        User user = user(true);
        IssuedRefreshToken refreshToken = new IssuedRefreshToken(null, "refresh-value", Instant.now().plusSeconds(60));
        when(userRepository.findByEmail("ivcho@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "stored-hash")).thenReturn(true);
        when(tokenService.issueAccessToken(user.getId(), "ivcho@example.com", Role.DISPATCHER))
                .thenReturn(new AccessToken("signed-token", 900));
        when(refreshTokenService.issue(user.getId(), AGENT, IP)).thenReturn(refreshToken);

        LoginResult result = authService.login("Ivcho@Example.com", "secret123", AGENT, IP);

        assertThat(result.tokens().accessToken()).isEqualTo("signed-token");
        assertThat(result.tokens().tokenType()).isEqualTo("Bearer");
        assertThat(result.tokens().expiresIn()).isEqualTo(900);
        assertThat(result.refreshToken()).isSameAs(refreshToken);
    }

    @Test
    void loginRejectsAWrongPassword() {
        when(userRepository.findByEmail("ivcho@example.com")).thenReturn(Optional.of(user(true)));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("ivcho@example.com", "wrong", AGENT, IP))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoTokensIssued();
    }

    @Test
    void loginRejectsAnUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody@example.com", "secret123", AGENT, IP))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoTokensIssued();
    }

    @Test
    void loginRejectsADisabledUserEvenWithTheRightPassword() {
        when(userRepository.findByEmail("ivcho@example.com")).thenReturn(Optional.of(user(false)));
        when(passwordEncoder.matches("secret123", "stored-hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("ivcho@example.com", "secret123", AGENT, IP))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoTokensIssued();
    }

    @Test
    void refreshReturnsANewAccessTokenAndTheRotatedRefreshToken() {
        UUID userId = UUID.randomUUID();
        User user = user(true);
        IssuedRefreshToken rotated = new IssuedRefreshToken(userId, "new-refresh", Instant.now().plusSeconds(60));
        when(refreshTokenService.rotate("old-refresh", AGENT, IP)).thenReturn(rotated);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenService.issueAccessToken(user.getId(), "ivcho@example.com", Role.DISPATCHER))
                .thenReturn(new AccessToken("new-access", 900));

        LoginResult result = authService.refresh("old-refresh", AGENT, IP);

        assertThat(result.tokens().accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isSameAs(rotated);
    }

    @Test
    void refreshEndsAllSessionsWhenTheUserHasBeenDisabled() {
        UUID userId = UUID.randomUUID();
        IssuedRefreshToken rotated = new IssuedRefreshToken(userId, "new-refresh", Instant.now().plusSeconds(60));
        when(refreshTokenService.rotate("old-refresh", AGENT, IP)).thenReturn(rotated);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(false)));

        assertThatThrownBy(() -> authService.refresh("old-refresh", AGENT, IP))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenService).revokeAllForUser(userId);
        verify(tokenService, never()).issueAccessToken(any(), any(), any());
    }

    private void verifyNoTokensIssued() {
        verify(tokenService, never()).issueAccessToken(any(), any(), any());
        verify(refreshTokenService, never()).issue(any(), any(), any());
    }

    private static User user(boolean enabled) {
        User user = new User();
        user.setEmail("ivcho@example.com");
        user.setPasswordHash("stored-hash");
        user.setFullName("Ivaylo Atanasov");
        user.setRole(Role.DISPATCHER);
        user.setEnabled(enabled);
        return user;
    }
}
