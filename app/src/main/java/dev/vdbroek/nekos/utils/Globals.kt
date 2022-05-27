package dev.vdbroek.nekos.utils

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

val IS_DARK = booleanPreferencesKey("is_dark")
val MANUAL = booleanPreferencesKey("manual")
val STAGGERED = booleanPreferencesKey("staggered")
val NSFW = booleanPreferencesKey("nsfw")

val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
val TOKEN = stringPreferencesKey("token")
val USERNAME = stringPreferencesKey("username")
