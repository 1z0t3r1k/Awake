package com.amiawake.amiawake.friendship.entity;

import com.amiawake.amiawake.common.exception.CannotAcceptOwnFriendRequestException;
import com.amiawake.amiawake.common.exception.CannotFriendYourselfException;
import com.amiawake.amiawake.common.exception.FriendshipNotPendingException;
import com.amiawake.amiawake.common.exception.UserNotPartOfFriendshipException;
import com.amiawake.amiawake.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "friendships")
public class Friendship {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FriendshipStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Friendship() {
    }

    public Friendship(
            User firstUser,
            User secondUser,
            User requester
    ) {
        boolean requesterCheck = requester.getId().equals(firstUser.getId()) ||
                requester.getId().equals(secondUser.getId());
        int usersOrderCheck = firstUser.getId().compareTo(secondUser.getId());

        this.id = UUID.randomUUID();

        if (requesterCheck) {
            this.requester = requester;
        } else {
            throw new IllegalArgumentException("Requester must be one of the users");
        }

        if (usersOrderCheck < 0) {
            this.user1 = firstUser;
            this.user2 = secondUser;
        } else if (usersOrderCheck > 0) {
            this.user1 = secondUser;
            this.user2 = firstUser;
        } else {
            throw new IllegalArgumentException();
        }

        this.status = FriendshipStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void acceptFriendship(User user) {
        boolean isParticipant =
                user.getId().equals(user1.getId()) ||
                        user.getId().equals(user2.getId());

        if (!isParticipant) {
            throw new UserNotPartOfFriendshipException();
        }

        if (user.getId().equals(requester.getId())) {
            throw new CannotAcceptOwnFriendRequestException();
        }

        if (status != FriendshipStatus.PENDING) {
            throw new FriendshipNotPendingException();
        }

        this.status = FriendshipStatus.ACCEPTED;
        this.updatedAt = Instant.now();
    }

    public static FriendshipPair normalizeUsers(User firstUser, User secondUser) {
        int usersOrderCheck = firstUser.getId().compareTo(secondUser.getId());

        if (usersOrderCheck < 0) {
            return new FriendshipPair(firstUser, secondUser);
        } else if (usersOrderCheck > 0) {
            return new FriendshipPair(secondUser, firstUser);
        } else {
            throw new CannotFriendYourselfException();
        }
    }
}
