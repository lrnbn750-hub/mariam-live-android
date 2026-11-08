package com.mariam.live.utils

object VoiceRoomUtils {
    const val MAX_MEMBERS = 8

    fun canJoin(currentMembers: Int): Boolean {
        return currentMembers < MAX_MEMBERS
    }

    fun isRoomFull(currentMembers: Int): Boolean {
        return currentMembers >= MAX_MEMBERS
    }
}
