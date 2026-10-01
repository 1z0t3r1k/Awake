package com.amiawake.amiawake.user.repository;

import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.projection.UserNotificationInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    // Serialize inference even before user_states exists. NO KEY UPDATE remains compatible
    // with foreign-key checks from telemetry inserts in another transaction (PostgreSQL).
    @Query(value = "SELECT id FROM users WHERE id = :userId FOR NO KEY UPDATE", nativeQuery = true)
    UUID lockForInference(@org.springframework.data.repository.query.Param("userId") UUID userId);

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    List<User> findTop20ByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String username,
            String displayName
    );

    @Query("""
            select new com.amiawake.amiawake.user.projection.UserNotificationInfo(
                    u.displayName,
                    u.status
            )
            from User u
            where u.id = :userId
            """)
    Optional<UserNotificationInfo> findUserNotificationInfoById(UUID userId);
}
