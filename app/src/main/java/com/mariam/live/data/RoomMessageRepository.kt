package com.mariam.live.data

data class RoomMessage(
    val id: String,
    val roomId: String,
    val userId: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class RoomMessageRepository {
    private val messages = mutableListOf<RoomMessage>()

    fun sendMessage(message: RoomMessage) {
        messages.add(message)
    }

    fun getMessages(roomId: String): List<RoomMessage> {
        return messages.filter { it.roomId == roomId }
    }

    fun deleteMessage(id: String) {
        messages.removeAll { it.id == id }
    }
}
