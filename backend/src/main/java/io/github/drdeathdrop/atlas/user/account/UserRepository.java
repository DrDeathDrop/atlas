package io.github.drdeathdrop.atlas.user.account;

import io.github.drdeathdrop.atlas.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);

    @Query("select u.id from User u where u.enabled = true and u.role in :roles")
    List<UUID> findEnabledIdsByRoleIn(@Param("roles") Collection<Role> roles);
}
