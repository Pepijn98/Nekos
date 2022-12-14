package dev.vdbroek.nekos.models

import androidx.annotation.Keep

@Keep
data class Uploader(
    val id: String,
    val username: String
)

@Keep
data class Approver(
    val id: String,
    val username: String
)

@Keep
data class Neko(
    val id: String,
    val originalHash: String,
    val uploader: Uploader,
    val approver: Approver?,
    val nsfw: Boolean,
    val artist: String?,
    val tags: ArrayList<String>,
    val comments: ArrayList<String>,
    val createdAt: String,
    val likes: Int,
    val favorites: Int
) {
    fun getPostUrl(): String = "https://nekos.moe/post/$id"
    fun getImageUrl(): String = "https://nekos.moe/image/$id"
    fun getThumbnailUrl(): String = "https://nekos.moe/thumbnail/$id"
}

@Keep
data class TagsResponse(
    val options: Any?,
    val tags: ArrayList<String>
)

@Keep
data class NekosResponse(
    val images: MutableList<Neko>
)
