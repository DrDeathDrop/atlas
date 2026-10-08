package io.github.drdeathdrop.atlas.user.auth;

import io.github.drdeathdrop.atlas.user.UserSummary;
import io.github.drdeathdrop.atlas.user.account.User;
import io.github.drdeathdrop.atlas.user.account.UserRepository;
import io.github.drdeathdrop.atlas.user.security.AccessToken;
import io.github.drdeathdrop.atlas.user.security.TokenService;
import io.github.drdeathdrop.atlas.user.session.InvalidRefreshTokenException;
import io.github.drdeathdrop.atlas.user.session.IssuedRefreshToken;
import io.github.drdeathdrop.atlas.user.session.RefreshTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;

    /** A hash of nothing in particular, compared against when the email is unknown. */
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public LoginResult login(String email, String rawPassword, String userAgent, String ipAddress) {
        Optional<User> found = userRepository.findByEmail(email.toLowerCase());

        if (found.isEmpty()) {
            // Do the same amount of hashing work as for a real user, so the
            // response time does not reveal whether the email exists.
            passwordEncoder.matches(rawPassword, dummyPasswordHash);
            throw new InvalidCredentialsException();
        }

        User user = found.get();

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!user.isEnabled()) {
            throw new InvalidCredentialsException();
        }

        IssuedRefreshToken refreshToken = refreshTokenService.issue(user.getId(), userAgent, ipAddress);
        return new LoginResult(accessTokenFor(user), refreshToken);
    }

    /**
     * Exchanges a refresh token for a new access token and a new refresh
     * token. Deliberately not transactional: the rotation commits on its
     * own, so a revocation it performs is never undone by a later failure.
     */
    public LoginResult refresh(String presentedRefreshToken, String userAgent, String ipAddress) {
        IssuedRefreshToken rotated = refreshTokenService.rotate(presentedRefreshToken, userAgent, ipAddress);

        Optional<User> user = userRepository.findById(rotated.userId()).filter(User::isEnabled);
        if (user.isEmpty()) {
            // The account was disabled or removed after the session started.
            refreshTokenService.revokeAllForUser(rotated.userId());
            throw new InvalidRefreshTokenException();
        }

        return new LoginResult(accessTokenFor(user.get()), rotated);
    }

    public void logout(String presentedRefreshToken) {
        refreshTokenService.revoke(presentedRefreshToken);
    }

    @Transactional(readOnly = true)
    public UserSummary currentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(InvalidCredentialsException::new);

        return new UserSummary(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.isEnabled());
    }

    private TokenResponse accessTokenFor(User user) {
        AccessToken token = tokenService.issueAccessToken(user.getId(), user.getEmail(), user.getRole());
        return TokenResponse.bearer(token.value(), token.expiresInSeconds());
    }
}
