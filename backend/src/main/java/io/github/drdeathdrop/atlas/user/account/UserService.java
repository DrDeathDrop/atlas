package io.github.drdeathdrop.atlas.user.account;

import io.github.drdeathdrop.atlas.user.Role;
import io.github.drdeathdrop.atlas.user.UserSummary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserSummary createUser(String email, String rawPassword, String fullName, Role role) {
        String normalisedEmail = email.toLowerCase();

        if (userRepository.existsByEmail(normalisedEmail)) {
            throw new EmailAlreadyUsedException(normalisedEmail);
        }

        User user = new User();
        user.setEmail(normalisedEmail);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setRole(role);
        user.setEnabled(true);

        User saved = userRepository.save(user);

        return new UserSummary(
                saved.getId(),
                saved.getEmail(),
                saved.getFullName(),
                saved.getRole(),
                saved.isEnabled()
        );
    }
}