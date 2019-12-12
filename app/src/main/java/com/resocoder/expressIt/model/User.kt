package com.resocoder.expressIt.model

// defining the user that holds user's data
data class User(val name: String,
                val bio: String,
                val profilePicturePath: String?,
                val registrationTokens: MutableList<String>) {
    constructor(): this("", "", null, mutableListOf())
}