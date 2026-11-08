package com.mariam.live.utils

object ProfileUtils {
    fun isValidBio(bio: String): Boolean {
        return bio.length <= 150
    }

    fun isValidDisplayName(name: String): Boolean {
        return name.isNotBlank() && name.length <= 30
    }
}
