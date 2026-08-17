package com.amiawake.amiawake.friendship.repository;

import com.amiawake.amiawake.friendship.entity.Friendship;
import com.amiawake.amiawake.friendship.entity.FriendshipStatus;
import com.amiawake.amiawake.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    Optional<Friendship> findByUser1AndUser2(User user1, User user2);

    @Query("""
            SELECT f
            FROM Friendship f
            WHERE (f.user1 = :user OR f.user2 = :user)
              AND f.requester <> :user
              AND f.status = :status
            """)
    List<Friendship> findIncomingRequests(
            @Param("user") User user,
            @Param("status") FriendshipStatus status
    );

    @Query("""
            SELECT f
            FROM Friendship f
            WHERE (f.user1 = :user OR f.user2 = :user)
              AND f.status = :status
            """)
    List<Friendship> findFriendships(
            @Param("user") User user,
            @Param("status") FriendshipStatus status
    );

    @Query("SELECT f FROM Friendship f WHERE f.requester = :user AND f.status = :status")
    List<Friendship> findOutgoingRequests(
            @Param("user") User user,
            @Param("status") FriendshipStatus status
    );
}
