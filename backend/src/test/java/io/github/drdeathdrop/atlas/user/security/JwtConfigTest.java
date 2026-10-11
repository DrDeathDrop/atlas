package io.github.drdeathdrop.atlas.user.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import javax.crypto.SecretKey;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    private static final String SECRET = "a-signing-key-that-is-long-enough-123";

    private final JwtConfig config = new JwtConfig();

    @Test
    void aConfiguredSecretBecomesTheSigningKey() {
        SecretKey key = config.jwtSigningKey(properties(SECRET), new MockEnvironment());

        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(key.getEncoded()).hasSize(SECRET.length());
    }

    @Test
    void aSecretThatIsTooShortIsRefused() {
        assertThatThrownBy(() -> config.jwtSigningKey(properties("too-short"), new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32");
    }

    @Test
    void withoutASecretARandomKeyIsUsedInDevelopment() {
        SecretKey first = config.jwtSigningKey(properties(""), new MockEnvironment());
        SecretKey second = config.jwtSigningKey(properties(null), new MockEnvironment());

        assertThat(first.getEncoded()).hasSize(32);
        assertThat(first.getEncoded()).isNotEqualTo(second.getEncoded());
    }

    @Test
    void withoutASecretTheApplicationRefusesToStartInProduction() {
        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles(JwtConfig.PRODUCTION_PROFILE);

        assertThatThrownBy(() -> config.jwtSigningKey(properties(" "), production))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ATLAS_JWT_SECRET must be set");
    }

    private static JwtProperties properties(String secret) {
        return new JwtProperties(secret, Duration.ofMinutes(15));
    }
}
