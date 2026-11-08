package com.mariam.live.model

data class CoinTransaction(
    val id: String = "",
    val userId: String = "",
    val amount: Long = 0,
    val type: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
