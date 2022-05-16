package dev.vdbroek.nekos.api

import com.github.kittinunf.fuel.core.FuelError
import com.google.gson.Gson
import dev.vdbroek.nekos.models.ApiException
import dev.vdbroek.nekos.models.HttpException
import dev.vdbroek.nekos.utils.Response

object Api {

    fun <T> handleException(exception: FuelError?): Response<T?, Exception> {
        return if (exception != null) {
            val httpException: HttpException? = try {
                Gson().fromJson(exception.response.responseMessage, HttpException::class.java)
            } catch (e: Exception) {
                null
            }

            Response(null, if (httpException != null) ApiException(httpException) else exception)
        } else {
            Response(null, Exception("No data returned"))
        }
    }
}
