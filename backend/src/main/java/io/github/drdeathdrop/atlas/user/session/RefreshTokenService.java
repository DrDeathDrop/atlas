package io.github.drdeathdrop.atlas.user.session;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private static final int TOKEN_BYTES = 32;
    private static final int MAX_USER_AGENT_LENGTH = 255;
    private static final Duration DEFAULT_TTL = Duration.ofDays(7);

    private final RefreshTokenRepository repository;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, RefreshTokenProperties properties) {
        this.repository = repository;
        this.ttl = properties.ttl() != null ? properties.ttl() : DEFAULT_TTL;
    }

    @Transactional
    public IssuedRefreshToken issue(UUID userId, String userAgent, String ipAddress) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        repository.save(new RefreshToken(userId, hash(value), now, expiresAt, shorten(userAgent), ipAddress));

        return new IssuedRefreshToken(userId, value, expiresAt);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public IssuedRefreshToken rotate(String presentedValue, String userAgent, String ipAddress) {
        String hash = hash(presentedValue);
        Instant now = Instant.now();

        RefreshToken token = repository.findByTokenHash(hash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (token.isRevoked()) {
            repository.revokeAllForUser(token.getUserId(), now);
            throw new InvalidRefreshTokenException();
        }

        if (token.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        token.revoke(now);
        return issue(token.getUserId(), userAgent, ipAddress);
    }

    @Transactional
    public void revoke(String presentedValue) {
        repository.findByTokenHash(hash(presentedValue))
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<SessionSummary> activeSessions(UUID userId) {
        return repository
                .findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(userId, Instant.now())
                .stream()
                .map(token -> new SessionSummary(token.getId(), token.getCreatedAt(), token.getExpiresAt(),
                        token.getUserAgent(), token.getIpAddress()))
                .toList();
    }

    @Transactional
    public void revokeSession(UUID userId, UUID sessionId) {
        RefreshToken token = repository.findById(sessionId)
                .filter(found -> found.getUserId().equals(userId))
                .orElseThrow(SessionNotFoundException::new);

        token.revoke(Instant.now());
    }

    static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String shorten(String userAgent) {
        if (userAgent == null || userAgent.length() <= MAX_USER_AGENT_LENGTH) {
            return userAgent;
        }
        return userAgent.substring(0, MAX_USER_AGENT_LENGTH);
    }
}
