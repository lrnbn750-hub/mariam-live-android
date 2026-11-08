package com.mariam.live.utils

object AppUtils {
    fun isValidId(id: String): Boolean {
        return id.isNotBlank()
    }

    fun isValidAmount(amount: Int): Boolean {
        return amount > 0
    }

    fun formatCoins(coins: Int): String {
        return "%,d".format(coins)
    }
}
