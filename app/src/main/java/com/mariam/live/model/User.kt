package com.mariam.live.model

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val country: String = "",
    val isOnline: Boolean = false,
    val coins: Long = 0
)
