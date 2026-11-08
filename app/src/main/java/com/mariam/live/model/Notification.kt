package com.mariam.live.model

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
