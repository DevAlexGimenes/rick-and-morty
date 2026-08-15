package com.gimenes.alex.rickandmorty.core.network

/**
 * Typed classification of everything that can go wrong making a request through a client created
 * by [createHttpClient]. Deliberately generic and endpoint-agnostic: consumers built on top of
 * `core:network` (e.g. the Character API client) map their own domain errors from this, they
 * don't need to catch raw Ktor/kotlinx.serialization exceptions themselves.
 */
sealed interface NetworkError {

    /**
     * The request never got a response: no connectivity, DNS/connect failure, or the request
     * timed out.
     */
    data class Connectivity(val cause: Throwable) : NetworkError

    /**
     * The server responded, but with a non-2xx status code.
     */
    data class Http(val statusCode: Int, val cause: Throwable? = null) : NetworkError

    /**
     * A response body was received but could not be decoded into the requested type (malformed
     * or unexpected JSON shape).
     */
    data class Parsing(val cause: Throwable) : NetworkError

    /**
     * Anything that doesn't fit the categories above.
     */
    data class Unknown(val cause: Throwable) : NetworkError
}
