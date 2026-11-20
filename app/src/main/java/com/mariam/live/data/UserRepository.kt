package com.mariam.live.data

import com.mariam.live.model.User

class UserRepository {

    private val users = mutableListOf<User>()

    fun getUsers(): List<User> = users

    fun addUser(user: User) {
        users.add(user)
    }

    fun findUser(id: String): User? {
        return users.find { it.id == id }
    }
}
