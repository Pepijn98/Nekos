package dev.vdbroek.nekos.helper

import android.content.Context
import android.os.Build
import com.github.kittinunf.fuel.httpPost
import com.google.gson.Gson
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import dev.vdbroek.nekos.model.Nekos
import com.github.kittinunf.result.Result

data class Response<out V : Any?, out E : Exception?>(val value: V, val exception: E)

object Api {
    const val baseUrl = "https://nekos.moe/api/v1"
    val gson = Gson()

    lateinit var version: String
    lateinit var versionCode: String
    lateinit var userAgent: String

    fun getVersions(ctx: Context): Pair<String, String> {
        val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Pair(packageInfo.versionName, String.format("%03d", packageInfo.longVersionCode))
        } else {
            @Suppress("DEPRECATION")
            Pair(packageInfo.versionName, String.format("%03d", packageInfo.versionCode))
        }
    }

    fun requestNekosAsync(toSkip: Int, sort: String): Deferred<Response<Nekos?, Exception?>> {
        val tags = "-\\\"bare shoulders\\\", -\\\"bikini\\\", -\\\"crop top\\\", -\\\"swimsuit\\\", -\\\"midriff\\\", -\\\"no bra\\\", -\\\"panties\\\", -\\\"covered nipples\\\", -\\\"from behind\\\", -\\\"knees up\\\", -\\\"leotard\\\", -\\\"black bikini top\\\", -\\\"black bikini bottom\\\", -\\\"off-shoulder shirt\\\", -\\\"naked shirt\\\""
        val reqbody = "{\"nsfw\": false, \"tags\": \"$tags\", \"limit\": 50, \"skip\": $toSkip, \"sort\": \"$sort\"}"
        println(reqbody)
        return GlobalScope.async {
            val (_, _, result) = "/images/search".httpPost()
                .header(mapOf("Content-Type" to "application/json"))
                .body(reqbody)
                .responseString()

            val (data, exception) = result
            when (result) {
                is Result.Success -> {
                    if (data != null) {
                        return@async Response(gson.fromJson(data, Nekos::class.java), null)
                    }
                    return@async Response(null, Exception("No data returned"))
                }
                is Result.Failure -> {
                    if (exception != null) {
                        return@async Response(null, exception)
                    }
                    return@async Response(null, Exception("No data returned"))
                }
            }
        }
    }

}