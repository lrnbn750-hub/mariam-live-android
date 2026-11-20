package com.mariam.live.data

class HomeRepository {
    fun getWelcomeMessage(): String {
        return "Welcome to BAK LIVE"
    }

    fun getMainSections(): List<String> {
        return listOf(
            "Voice Rooms",
            "Messages",
            "Friends",
            "Gifts",
            "My Profile"
        )
    }
}
