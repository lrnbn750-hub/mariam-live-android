package com.mariam.live.model

data class Report(
    val id: String = "",
    val reporterId: String = "",
    val reportedUserId: String = "",
    val reason: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "pending"
)
