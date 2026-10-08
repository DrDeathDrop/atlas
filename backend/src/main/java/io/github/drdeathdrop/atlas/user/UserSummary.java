package io.github.drdeathdrop.atlas.user;

import java.util.UUID;

public record UserSummary(
        UUID id,
        String email,
        String fullName,
        Role role,
        boolean enabled
) {
}
