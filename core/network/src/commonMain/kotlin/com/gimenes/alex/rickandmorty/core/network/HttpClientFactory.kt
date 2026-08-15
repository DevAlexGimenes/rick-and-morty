package com.gimenes.alex.rickandmorty.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val REQUEST_TIMEOUT_MILLIS = 15_000L
private const val CONNECT_TIMEOUT_MILLIS = 10_000L
private const val SOCKET_TIMEOUT_MILLIS = 15_000L

/**
 * Base Ktor [HttpClient] configuration shared across every platform *and* by tests: JSON content
 * negotiation, request/connect/socket timeouts, request logging, and `expectSuccess` so that
 * non-2xx responses surface as [io.ktor.client.plugins.ResponseException] rather than being
 * silently decoded. Pairing this with [safeApiCall] is what lets network/HTTP/parse failures come
 * out as a typed [NetworkResult] instead of a raw Ktor exception.
 *
 * Exposed as a standalone extension (rather than being buried inside [createHttpClient]) so
 * MockEngine-backed tests configure the client identically to production.
 */
fun HttpClientConfig<*>.applyRickAndMortyDefaults() {
    expectSuccess = true
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
    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
    }
}

/**
 * Base Ktor [HttpClient] configuration shared across platforms. The concrete engine (OkHttp on
 * Android, Darwin on iOS) is resolved automatically from the platform-specific engine dependency
 * declared in this module's androidMain/iosMain source sets.
 */
fun createHttpClient(): HttpClient = HttpClient {
    applyRickAndMortyDefaults()
}
