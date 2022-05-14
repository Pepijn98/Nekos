package dev.vdbroek.nekos.models

data class UserResponse(
    val user: User
)

data class User(
    val id: String,
    val username: String,
    val createdAt: String,
    val favoritesReceived: Int,
    val likesReceived: Int,
    val favorites: ArrayList<String>,
    val likes: ArrayList<String>,
    val uploads: Int,
    val roles: ArrayList<String>,
    val verified: Boolean,
    val savedTags: ArrayList<String>
)

data class LoginResponse(
    val token: String
)
