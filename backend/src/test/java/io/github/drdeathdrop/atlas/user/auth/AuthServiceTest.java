package io.github.drdeathdrop.atlas.user.auth;

import io.github.drdeathdrop.atlas.user.Role;
import io.github.drdeathdrop.atlas.user.account.User;
import io.github.drdeathdrop.atlas.user.account.UserRepository;
import io.github.drdeathdrop.atlas.user.security.AccessToken;
import io.github.drdeathdrop.atlas.user.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, tokenService);
    }

    @Test
    void loginReturnsATokenForCorrectCredentials() {
        User user = user(true);
        when(userRepository.findByEmail("ivcho@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "stored-hash")).thenReturn(true);
        when(tokenService.issueAccessToken(user.getId(), "ivcho@example.com", Role.DISPATCHER))
                .thenReturn(new AccessToken("signed-token", 900));

        TokenResponse response = authService.login("Ivcho@Example.com", "secret123");

        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
    }

    @Test
    void loginRejectsAWrongPassword() {
        when(userRepository.findByEmail("ivcho@example.com")).thenReturn(Optional.of(user(true)));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("ivcho@example.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenService, never()).issueAccessToken(any(), any(), any());
    }

    @Test
    void loginRejectsAnUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody@example.com", "secret123"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenService, never()).issueAccessToken(any(), any(), any());
    }

    @Test
    void loginRejectsADisabledUserEvenWithTheRightPassword() {
        when(userRepository.findByEmail("ivcho@example.com")).thenReturn(Optional.of(user(false)));
        when(passwordEncoder.matches("secret123", "stored-hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("ivcho@example.com", "secret123"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenService, never()).issueAccessToken(any(), any(), any());
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
