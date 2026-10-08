package io.github.drdeathdrop.atlas.user.account;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "atlas.bootstrap.admin")
public record AdminBootstrapProperties(String email, String password) {
}
