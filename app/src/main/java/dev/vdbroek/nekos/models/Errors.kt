package dev.vdbroek.nekos.models

data class HttpException(
    val message: String?
)

class EndException(override val message: String) : Exception(message)
class ApiException(httpException: HttpException) : Exception(httpException.message ?: "Unknown error from nekos.moe")
