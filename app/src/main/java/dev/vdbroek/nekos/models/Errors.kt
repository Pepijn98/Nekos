package dev.vdbroek.nekos.models

data class HttpException(
    val message: String?
)

class EndException(override val message: String) : Exception(message)

class ApiException(
    httpException: HttpException,
    label: String = "UNKNOWN"
) : Exception(
    if (httpException.message != null)
        "[$label]: ${httpException.message}"
    else
        "[$label]: Unknown error from nekos.moe"
)
