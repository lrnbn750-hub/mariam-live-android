package com.mariam.live.data

class RoomPermissionRepository {
    private val speakers = mutableMapOf<String, MutableSet<String>>()

    fun allowSpeaking(roomId: String, userId: String) {
        speakers.getOrPut(roomId) { mutableSetOf() }.add(userId)
    }

    fun muteSpeaker(roomId: String, userId: String) {
        speakers[roomId]?.remove(userId)
    }

    fun canSpeak(roomId: String, userId: String): Boolean {
        return speakers[roomId]?.contains(userId) == true
    }
}
