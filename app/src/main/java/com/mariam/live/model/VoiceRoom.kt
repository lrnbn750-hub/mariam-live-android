package com.mariam.live.model

data class VoiceRoom(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val imageUrl: String = "",
    val participants: Int = 0,
    val maxParticipants: Int = 8,
    val isActive: Boolean = false
)
