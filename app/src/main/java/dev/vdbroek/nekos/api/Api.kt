package dev.vdbroek.nekos.api

import com.github.kittinunf.fuel.core.FuelError
import com.google.gson.Gson
import dev.vdbroek.nekos.models.ApiException
import dev.vdbroek.nekos.models.HttpException
import dev.vdbroek.nekos.utils.Response

object Api {

    fun <T> handleException(exception: FuelError?, label: String = "UNKNOWN"): Response<T?, Exception> {
        return if (exception != null) {
            val httpException: HttpException? = try {
                Gson().fromJson(String(exception.errorData), HttpException::class.java)
            } catch (e: Exception) {
                null
            }

            Response(null, if (httpException != null) ApiException(httpException, label) else exception)
        } else {
            Response(null, Exception("[$label]: Invalid response from API"))
        }
    }
}
