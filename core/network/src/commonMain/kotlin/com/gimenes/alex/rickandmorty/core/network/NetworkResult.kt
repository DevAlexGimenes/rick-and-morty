package com.gimenes.alex.rickandmorty.core.network

/**
 * Outcome of a call made through [safeApiCall]: either the decoded [Success] payload or a typed
 * [NetworkError] describing why the call failed. Callers switch on this instead of catching Ktor
 * exceptions directly.
 */
sealed interface NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>
    data class Failure(val error: NetworkError) : NetworkResult<Nothing>
}

/**
 * Transforms a successful [NetworkResult] payload, passing failures through unchanged.
 */
inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> =
    when (this) {
        is NetworkResult.Success -> NetworkResult.Success(transform(data))
        is NetworkResult.Failure -> this
    }
