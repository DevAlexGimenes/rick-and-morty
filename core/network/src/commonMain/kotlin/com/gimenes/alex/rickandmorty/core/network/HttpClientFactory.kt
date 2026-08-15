package com.gimenes.alex.rickandmorty.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Base Ktor [HttpClient] configuration shared across platforms. The concrete engine (OkHttp on
 * Android, Darwin on iOS) is resolved automatically from the platform-specific engine dependency
 * declared in this module's androidMain/iosMain source sets.
 */
fun createHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        )
    }
    install(Logging) {
        level = LogLevel.INFO
    }
}
