package io.github.drdeathdrop.atlas.user.auth;

import io.github.drdeathdrop.atlas.user.UserSummary;
import io.github.drdeathdrop.atlas.user.account.User;
import io.github.drdeathdrop.atlas.user.account.UserRepository;
import io.github.drdeathdrop.atlas.user.security.AccessToken;
import io.github.drdeathdrop.atlas.user.security.TokenService;
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

    /** A hash of nothing in particular, compared against when the email is unknown. */
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String email, String rawPassword) {
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

        AccessToken token = tokenService.issueAccessToken(user.getId(), user.getEmail(), user.getRole());
        return TokenResponse.bearer(token.value(), token.expiresInSeconds());
    }

    @Transactional(readOnly = true)
    public UserSummary currentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(InvalidCredentialsException::new);

        return new UserSummary(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.isEnabled());
    }
}
