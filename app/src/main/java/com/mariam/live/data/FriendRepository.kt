package com.mariam.live.data

import com.mariam.live.model.Friend

class FriendRepository {

    private val friends = mutableListOf<Friend>()

    fun getFriends(userId: String): List<Friend> {
        return friends.filter { it.userId == userId }
    }

    fun addFriend(friend: Friend) {
        friends.add(friend)
    }

    fun removeFriend(userId: String, friendId: String) {
        friends.removeAll {
            it.userId == userId && it.friendId == friendId
        }
    }
}
