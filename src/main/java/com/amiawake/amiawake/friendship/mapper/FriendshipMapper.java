package com.amiawake.amiawake.friendship.mapper;

import com.amiawake.amiawake.friendship.dto.FriendResponse;
import com.amiawake.amiawake.friendship.dto.IncomingFriendRequestResponse;
import com.amiawake.amiawake.friendship.dto.OutgoingFriendRequestResponse;
import com.amiawake.amiawake.friendship.entity.Friendship;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.userstate.dto.UserStateResponse;

public final class FriendshipMapper {

    private FriendshipMapper() {
    }

    public static IncomingFriendRequestResponse toIncomingRequestResponse(
            Friendship friendship
    ) {
        return new IncomingFriendRequestResponse(
                friendship.getRequester().getUsername()
        );
    }

    public static FriendResponse toFriendResponse(
            Friendship friendship,
            User currentUser,
            UserStateResponse userState
    ) {
        User friend = friendship.getOtherUser(currentUser);

        return new FriendResponse(
                friend.getUsername(),
                friend.getDisplayName(),
                friend.getStatus(),
                userState.state(),
                userState.confidence(),
                userState.calculatedAt()
        );
    }

    public static OutgoingFriendRequestResponse toOutgoingRequestResponse(
            Friendship friendship,
            User currentUser
    ) {
        User requestReceiver = friendship.getOtherUser(currentUser);

        return new OutgoingFriendRequestResponse(
                requestReceiver.getUsername()
        );
    }
}