package com.mariam.live.utils

object MessageUtils {
    const val MAX_LENGTH = 2000

    fun isValid(message: String): Boolean {
        return message.isNotBlank() &&
                message.length <= MAX_LENGTH
    }

    fun remainingCharacters(message: String): Int {
        return (MAX_LENGTH - message.length).coerceAtLeast(0)
    }
}
