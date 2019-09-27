package xyz.kurozero.nekosmoe.helper

import com.github.kittinunf.fuel.httpPost
import com.google.gson.Gson
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import xyz.kurozero.nekosmoe.model.Nekos
import com.github.kittinunf.result.Result

data class Response<out V : Any?, out E : Exception?>(val value: V, val exception: E)

object Api {
    const val baseUrl = "https://nekos.moe/api/v1"
    const val version = "1.0.0"
    const val userAgent = "NekosApp/v$version (https://github.com/KurozeroPB/Nekos)"
    val gson = Gson()

    fun requestNekosAsync(toSkip: Int, sort: String): Deferred<Response<Nekos?, Exception?>> {
        val reqbody = "{\"nsfw\": false, \"limit\": 50, \"skip\": $toSkip, \"sort\": \"$sort\"}"
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