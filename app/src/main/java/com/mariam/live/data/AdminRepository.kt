package com.mariam.live.data

class AdminRepository {
    private val admins = mutableSetOf<String>()

    fun addAdmin(userId: String) {
        admins.add(userId)
    }

    fun removeAdmin(userId: String) {
        admins.remove(userId)
    }

    fun isAdmin(userId: String): Boolean {
        return userId in admins
    }
}
