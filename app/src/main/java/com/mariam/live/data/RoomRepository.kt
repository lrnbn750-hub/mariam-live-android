package com.mariam.live.data

import com.mariam.live.model.VoiceRoom

class RoomRepository {
    private val rooms = mutableListOf<VoiceRoom>()

    fun getRooms(): List<VoiceRoom> = rooms.toList()

    fun createRoom(room: VoiceRoom) {
        rooms.add(room)
    }

    fun findRoom(id: String): VoiceRoom? {
        return rooms.find { it.id == id }
    }

    fun deleteRoom(id: String) {
        rooms.removeAll { it.id == id }
    }
}
