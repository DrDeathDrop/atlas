package io.github.drdeathdrop.atlas.user.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Creates the key that signs access tokens, and the two objects that use it:
 * an encoder that produces tokens and a decoder that verifies them.
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    /** HS256 needs a key of at least 256 bits. */
    private static final int MIN_KEY_BYTES = 32;

    @Bean
    SecretKey jwtSigningKey(JwtProperties properties) {
        String secret = properties.secret();
        byte[] keyBytes;

        if (secret == null || secret.isBlank()) {
            keyBytes = new byte[MIN_KEY_BYTES];
            new SecureRandom().nextBytes(keyBytes);
            log.warn("ATLAS_JWT_SECRET is not set. Using a random signing key: "
                    + "every token becomes invalid when the application restarts. "
                    + "Set ATLAS_JWT_SECRET outside local development.");
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < MIN_KEY_BYTES) {
                throw new IllegalStateException(
                        "ATLAS_JWT_SECRET must be at least " + MIN_KEY_BYTES + " characters long");
            }
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
