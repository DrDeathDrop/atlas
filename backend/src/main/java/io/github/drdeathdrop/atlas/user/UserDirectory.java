package io.github.drdeathdrop.atlas.user;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserDirectory {
    List<UUID> enabledUserIdsWithRoles(Collection<Role> roles);
}
