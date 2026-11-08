package com.mariam.live.data

import com.mariam.live.model.Message

class MessageRepository {

    private val messages = mutableListOf<Message>()

    fun getMessages(): List<Message> = messages

    fun sendMessage(message: Message) {
        messages.add(message)
    }

    fun getConversation(userId: String, otherId: String): List<Message> {
        return messages.filter {
            (it.senderId == userId && it.receiverId == otherId) ||
            (it.senderId == otherId && it.receiverId == userId)
        }
    }
}
