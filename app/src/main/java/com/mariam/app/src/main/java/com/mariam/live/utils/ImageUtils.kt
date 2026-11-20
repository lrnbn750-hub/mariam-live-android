package com.mariam.live.utils

object ImageUtils {
    fun isValidImageType(mimeType: String?): Boolean {
        return mimeType?.startsWith("image/") == true
    }

    fun isValidVideoType(mimeType: String?): Boolean {
        return mimeType?.startsWith("video/") == true
    }
}
