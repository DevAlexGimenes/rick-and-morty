package com.gimenes.alex.rickandmorty.core.network

import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * Runs [block] (typically a single Ktor request + [io.ktor.client.call.body] decode) and converts
 * whatever it throws into a typed [NetworkResult] instead of letting the exception escape
 * `core:network`.
 *
 * Requires the [io.ktor.client.HttpClient] used inside [block] to have been configured with
 * [applyRickAndMortyDefaults] (or equivalent `expectSuccess = true`), otherwise non-2xx responses
 * won't throw [ResponseException] and will instead attempt to decode as if successful.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): NetworkResult<T> =
    try {
        NetworkResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        NetworkResult.Failure(NetworkError.Http(statusCode = e.response.status.value, cause = e))
    } catch (e: ContentConvertException) {
        // Thrown by the ContentNegotiation plugin (e.g. JsonConvertException) when a response
        // body can't be decoded into the requested type.
        NetworkResult.Failure(NetworkError.Parsing(e))
    } catch (e: SerializationException) {
        // Covers decode failures raised directly by kotlinx.serialization outside the
        // ContentNegotiation pipeline.
        NetworkResult.Failure(NetworkError.Parsing(e))
    } catch (e: IOException) {
        NetworkResult.Failure(NetworkError.Connectivity(e))
    } catch (e: Exception) {
        NetworkResult.Failure(NetworkError.Unknown(e))
    }
