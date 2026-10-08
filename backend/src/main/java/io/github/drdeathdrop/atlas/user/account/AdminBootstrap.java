package io.github.drdeathdrop.atlas.user.account;

import io.github.drdeathdrop.atlas.user.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;


@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final AdminBootstrapProperties properties;

    public AdminBootstrap(UserRepository userRepository, UserService userService,
                          AdminBootstrapProperties properties) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        String password = properties.password();
        boolean generated = password == null || password.isBlank();
        if (generated) {
            password = randomPassword();
        }

        userService.createUser(properties.email(), password, "Administrator", Role.ADMIN);

        if (generated) {
            log.warn("Created the first administrator. Email: {}  Password: {}  "
                    + "(generated, shown only this once)", properties.email(), password);
        } else {
            log.info("Created the first administrator: {}", properties.email());
        }
    }

    private static String randomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
