package com.mariam.live.data

class AuthRepository {

    fun login(email: String, password: String): Boolean {
        return email.isNotBlank() && password.length >= 8
    }

    fun register(
        username: String,
        email: String,
        password: String
    ): Boolean {
        return username.length >= 3 &&
               email.isNotBlank() &&
               password.length >= 8
    }
}
