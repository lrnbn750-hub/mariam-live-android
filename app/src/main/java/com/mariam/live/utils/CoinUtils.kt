package com.mariam.live.utils

object CoinUtils {
    fun canAfford(balance: Int, price: Int): Boolean {
        return balance >= price && price > 0
    }

    fun calculateRemaining(balance: Int, price: Int): Int {
        return (balance - price).coerceAtLeast(0)
    }

    fun format(balance: Int): String {
        return "%,d".format(balance)
    }
}
