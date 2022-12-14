package dev.vdbroek.nekos.utils

import androidx.annotation.Keep

@Keep
data class Response<out V : Any?, out E : Exception?>(val value: V, val exception: E)
