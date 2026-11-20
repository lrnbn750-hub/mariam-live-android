package com.mariam.live.data

class BlockRepository {
    private val blockedUsers = mutableMapOf<String, MutableSet<String>>()

    fun blockUser(userId: String, blockedUserId: String) {
        blockedUsers.getOrPut(userId) { mutableSetOf() }.add(blockedUserId)
    }

    fun unblockUser(userId: String, blockedUserId: String) {
        blockedUsers[userId]?.remove(blockedUserId)
    }

    fun isBlocked(userId: String, blockedUserId: String): Boolean {
        return blockedUsers[userId]?.contains(blockedUserId) == true
    }

    fun getBlockedUsers(userId: String): Set<String> {
        return blockedUsers[userId] ?: emptySet()
    }
}
