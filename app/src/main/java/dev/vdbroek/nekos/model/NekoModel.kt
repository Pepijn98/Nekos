@file:Suppress("unused")

package dev.vdbroek.nekos.model

data class Nekos(
    val images: MutableList<Neko>
)

data class Neko(
    val id: String,
    val originalHash: String,
    val uploader: NekoUploader,
    val approver: NekoApprover?,
    val nsfw: Boolean,
    val artist: String,
    val tags: ArrayList<String>,
    val comments: ArrayList<String>,
    val createdAt: String,
    val likes: Int,
    val favorites: Int
) {
    fun getPostUrl(): String = "https://nekos.moe/post/${this.id}"
    fun getImageUrl(): String = "https://nekos.moe/image/${this.id}"
    fun getThumbnailUrl(): String = "https://nekos.moe/thumbnail/${this.id}"
}

data class NekoUploader(
    val id: String,
    val username: String
)

data class NekoApprover(
    val id: String,
    val username: String
)

data class NekoException(
    val message: String?
)

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