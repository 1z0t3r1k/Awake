package com.amiawake.amiawake.user.repository;

import com.amiawake.amiawake.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    List<User> findTop20ByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String username,
            String displayName
    );
}
