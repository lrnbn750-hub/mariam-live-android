package com.mariam.live.utils

object GiftUtils {
    fun isValidGiftPrice(price: Int): Boolean {
        return price > 0
    }

    fun calculateTotal(price: Int, quantity: Int): Int {
        if (price <= 0 || quantity <= 0) return 0
        return price * quantity
    }
}
