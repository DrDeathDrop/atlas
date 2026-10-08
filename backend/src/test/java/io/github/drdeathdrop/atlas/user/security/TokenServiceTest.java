package io.github.drdeathdrop.atlas.user.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import io.github.drdeathdrop.atlas.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private final SecretKey key = key("0123456789abcdef0123456789abcdef");
    private final TokenService tokenService = new TokenService(
            new NimbusJwtEncoder(new ImmutableSecret<>(key)),
            new JwtProperties(null, Duration.ofMinutes(15)));

    @Test
    void issuedTokenIsVerifiableAndCarriesTheUser() {
        UUID userId = UUID.randomUUID();

        AccessToken token = tokenService.issueAccessToken(userId, "ivcho@example.com", Role.DISPATCHER);

        Jwt decoded = decoder(key).decode(token.value());
        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getClaimAsString(TokenService.EMAIL_CLAIM)).isEqualTo("ivcho@example.com");
        assertThat(decoded.getClaimAsString(TokenService.ROLE_CLAIM)).isEqualTo("DISPATCHER");
        assertThat(token.expiresInSeconds()).isEqualTo(900);
        assertThat(Duration.between(decoded.getIssuedAt(), decoded.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void tokenIsRejectedByADifferentKey() {
        AccessToken token = tokenService.issueAccessToken(UUID.randomUUID(), "ivcho@example.com", Role.VIEWER);

        JwtDecoder otherDecoder = decoder(key("ffffffffffffffffffffffffffffffff"));

        assertThatThrownBy(() -> otherDecoder.decode(token.value())).isInstanceOf(JwtException.class);
    }

    private static SecretKey key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static JwtDecoder decoder(SecretKey key) {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
