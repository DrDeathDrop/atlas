package io.github.drdeathdrop.atlas.user.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    private static final String AGENT = "JUnit";
    private static final String IP = "127.0.0.1";

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, new RefreshTokenProperties(Duration.ofDays(7)));
    }

    @Test
    void issueStoresOnlyTheHashOfTheToken() {
        UUID userId = UUID.randomUUID();

        IssuedRefreshToken issued = service.issue(userId, AGENT, IP);

        RefreshToken stored = savedTokens(1).get(0);
        assertThat(stored.getUserId()).isEqualTo(userId);
        assertThat(stored.getTokenHash()).isEqualTo(RefreshTokenService.hash(issued.value()));
        assertThat(stored.getTokenHash()).isNotEqualTo(issued.value());
        assertThat(stored.getExpiresAt()).isEqualTo(issued.expiresAt());
        assertThat(stored.isRevoked()).isFalse();
    }

    @Test
    void issueProducesADifferentTokenEveryTime() {
        UUID userId = UUID.randomUUID();

        assertThat(service.issue(userId, AGENT, IP).value())
                .isNotEqualTo(service.issue(userId, AGENT, IP).value());
    }

    @Test
    void rotateRevokesThePresentedTokenAndIssuesANewOne() {
        UUID userId = UUID.randomUUID();
        RefreshToken current = token(userId, "current-value", Instant.now().plus(Duration.ofDays(1)));
        when(repository.findByTokenHash(RefreshTokenService.hash("current-value"))).thenReturn(Optional.of(current));

        IssuedRefreshToken rotated = service.rotate("current-value", AGENT, IP);

        assertThat(current.isRevoked()).isTrue();
        assertThat(rotated.userId()).isEqualTo(userId);
        assertThat(rotated.value()).isNotEqualTo("current-value");

        RefreshToken replacement = savedTokens(1).get(0);
        assertThat(replacement.getUserId()).isEqualTo(userId);
        assertThat(replacement.getTokenHash()).isEqualTo(RefreshTokenService.hash(rotated.value()));
    }

    @Test
    void rotateRejectsAnUnknownToken() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("made-up", AGENT, IP))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void rotateRejectsAnExpiredToken() {
        UUID userId = UUID.randomUUID();
        RefreshToken expired = token(userId, "old-value", Instant.now().minus(Duration.ofMinutes(1)));
        when(repository.findByTokenHash(RefreshTokenService.hash("old-value"))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.rotate("old-value", AGENT, IP))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository, never()).save(any());
        verify(repository, never()).revokeAllForUser(any(), any());
    }

    @Test
    void rotateTreatsAReusedTokenAsStolenAndEndsEverySession() {
        UUID userId = UUID.randomUUID();
        RefreshToken alreadyUsed = token(userId, "used-value", Instant.now().plus(Duration.ofDays(1)));
        alreadyUsed.revoke(Instant.now());
        when(repository.findByTokenHash(RefreshTokenService.hash("used-value"))).thenReturn(Optional.of(alreadyUsed));

        assertThatThrownBy(() -> service.rotate("used-value", AGENT, IP))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository).revokeAllForUser(eq(userId), any(Instant.class));
        verify(repository, never()).save(any());
    }

    @Test
    void revokeSessionRefusesASessionOfAnotherUser() {
        UUID owner = UUID.randomUUID();
        UUID someoneElse = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        RefreshToken session = token(owner, "value", Instant.now().plus(Duration.ofDays(1)));
        when(repository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.revokeSession(someoneElse, sessionId))
                .isInstanceOf(SessionNotFoundException.class);

        assertThat(session.isRevoked()).isFalse();
    }

    private List<RefreshToken> savedTokens(int expectedCount) {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository, times(expectedCount)).save(captor.capture());
        return captor.getAllValues();
    }

    private static RefreshToken token(UUID userId, String value, Instant expiresAt) {
        return new RefreshToken(userId, RefreshTokenService.hash(value), Instant.now(), expiresAt, AGENT, IP);
    }
}
