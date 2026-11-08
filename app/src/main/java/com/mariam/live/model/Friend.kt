package com.mariam.live.model

data class Friend(
    val id: String = "",
    val userId: String = "",
    val friendId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
