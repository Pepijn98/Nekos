package dev.vdbroek.nekos.utils

data class Response<out V : Any?, out E : Exception?>(val value: V, val exception: E)
