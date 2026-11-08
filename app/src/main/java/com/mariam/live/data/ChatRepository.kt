package com.mariam.live.data

import com.mariam.live.model.Chat

class ChatRepository {

    private val chats = mutableListOf<Chat>()

    fun getChats(userId: String): List<Chat> {
        return chats.filter { userId in it.participantIds }
    }

    fun createChat(chat: Chat) {
        chats.add(chat)
    }

    fun findChat(id: String): Chat? {
        return chats.find { it.id == id }
    }

    fun deleteChat(id: String) {
        chats.removeAll { it.id == id }
    }
}
