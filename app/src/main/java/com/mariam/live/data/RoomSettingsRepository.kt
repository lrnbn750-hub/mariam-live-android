package com.mariam.live.data

data class RoomSettings(
    val roomId: String,
    var title: String,
    var isPrivate: Boolean = false,
    var maxMembers: Int = 8
)

class RoomSettingsRepository {
    private val settings = mutableMapOf<String, RoomSettings>()

    fun save(roomSettings: RoomSettings) {
        settings[roomSettings.roomId] = roomSettings
    }

    fun get(roomId: String): RoomSettings? {
        return settings[roomId]
    }

    fun delete(roomId: String) {
        settings.remove(roomId)
    }
}
