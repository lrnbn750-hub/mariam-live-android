package com.mariam.live.data

class RoomMemberRepository {
    private val members = mutableMapOf<String, MutableSet<String>>()

    fun joinRoom(roomId: String, userId: String) {
        members.getOrPut(roomId) { mutableSetOf() }.add(userId)
    }

    fun leaveRoom(roomId: String, userId: String) {
        members[roomId]?.remove(userId)
    }

    fun getMembers(roomId: String): Set<String> {
        return members[roomId]?.toSet() ?: emptySet()
    }

    fun isRoomFull(roomId: String): Boolean {
        return getMembers(roomId).size >= 8
    }
}
