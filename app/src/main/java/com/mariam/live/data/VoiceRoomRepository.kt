package com.mariam.live.data

import com.mariam.live.model.VoiceRoom

class VoiceRoomRepository {

    private val rooms = mutableListOf<VoiceRoom>()

    fun getRooms(): List<VoiceRoom> = rooms

    fun createRoom(room: VoiceRoom) {
        rooms.add(room)
    }

    fun removeRoom(id: String) {
        rooms.removeAll { it.id == id }
    }

    fun findRoom(id: String): VoiceRoom? {
        return rooms.find { it.id == id }
    }
}
