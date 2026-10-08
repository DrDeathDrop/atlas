package io.github.drdeathdrop.atlas.user.security;

import io.github.drdeathdrop.atlas.user.Role;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Issues signed access tokens.
 *
 * A token carries the user's id as its subject, plus the email and role.
 * The server keeps no record of it: a request is trusted because the
 * signature is valid and the token has not expired.
 */
@Service
public class TokenService {

    public static final String ROLE_CLAIM = "role";
    public static final String EMAIL_CLAIM = "email";

    private static final String ISSUER = "atlas";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    private final JwtEncoder jwtEncoder;
    private final Duration accessTokenTtl;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.accessTokenTtl = properties.accessTokenTtl() != null ? properties.accessTokenTtl() : DEFAULT_TTL;
    }

    public AccessToken issueAccessToken(UUID userId, String email, Role role) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtl))
                .claim(EMAIL_CLAIM, email)
                .claim(ROLE_CLAIM, role.name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, accessTokenTtl.toSeconds());
    }
}
