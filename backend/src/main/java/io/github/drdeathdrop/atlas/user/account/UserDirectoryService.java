package io.github.drdeathdrop.atlas.user.account;

import io.github.drdeathdrop.atlas.user.Role;
import io.github.drdeathdrop.atlas.user.UserDirectory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class UserDirectoryService implements UserDirectory {
    private final UserRepository userRepository;

    public UserDirectoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> enabledUserIdsWithRoles(Collection<Role> roles) {
        if (roles.isEmpty()) {
            return List.of();
        }
        return userRepository.findEnabledIdsByRoleIn(roles);
    }
}
