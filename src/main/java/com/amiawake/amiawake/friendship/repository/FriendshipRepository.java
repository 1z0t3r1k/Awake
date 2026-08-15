package com.amiawake.amiawake.friendship.repository;

import com.amiawake.amiawake.friendship.entity.Friendship;
import com.amiawake.amiawake.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    Optional<Friendship> findByUser1AndUser2(User user1, User user2);
}
