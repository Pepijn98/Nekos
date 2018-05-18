package xyz.kurozero.nekos

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import kotlinx.serialization.*

data class Nekos(
        val images: ArrayList<Neko>
) {
    class Deserializer : ResponseDeserializable<Nekos> {
        override fun deserialize(content: String): Nekos? = Gson().fromJson(content, Nekos::class.java)
    }
}

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
)

data class NekoUploader(
        val id: String,
        val username: String
)

data class NekoApprover(
        val id: String,
        val username: String
)

@Serializable
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
) {
    class Deserializer : ResponseDeserializable<User> {
        override fun deserialize(content: String): User? = Gson().fromJson(content, User::class.java)
    }
}