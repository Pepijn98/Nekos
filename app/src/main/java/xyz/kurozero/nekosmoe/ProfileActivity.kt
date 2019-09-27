package xyz.kurozero.nekosmoe

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.content_profile.*
import xyz.kurozero.nekosmoe.helper.timestamp

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        tvName.text = user!!.username
        tvLikes.text = user!!.likesReceived.toString()
        tvFavorites.text = user!!.favoritesReceived.toString()
        tvJoined.text = timestamp(user!!.createdAt)
        tvPosted.text = "${user!!.uploads} ${if (user!!.uploads == 1) "image" else "images"}"
        tvGiven.text = "Has given ${user!!.likes.size} likes and ${user!!.favorites.size} favorites"
    }

}