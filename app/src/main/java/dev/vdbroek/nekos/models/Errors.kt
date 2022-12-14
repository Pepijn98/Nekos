package dev.vdbroek.nekos.models

import androidx.annotation.Keep

@Keep
data class HttpException(
    val message: String?
)

@Keep
class EndException(override val message: String) : Exception(message)

@Keep
class ApiException(
    httpException: HttpException,
    label: String = "UNKNOWN"
) : Exception(
    if (httpException.message != null)
        "[$label]: ${httpException.message}"
    else
        "[$label]: Unknown error from nekos.moe"
)
