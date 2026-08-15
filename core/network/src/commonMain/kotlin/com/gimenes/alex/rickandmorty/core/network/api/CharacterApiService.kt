package com.gimenes.alex.rickandmorty.core.network.api

import com.gimenes.alex.rickandmorty.core.network.NetworkResult
import com.gimenes.alex.rickandmorty.core.network.RickAndMortyApiConfig
import com.gimenes.alex.rickandmorty.core.network.dto.CharacterDto
import com.gimenes.alex.rickandmorty.core.network.dto.PagedResponseDto
import com.gimenes.alex.rickandmorty.core.network.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Client for the Rick and Morty `/character` REST endpoints. A thin, endpoint-scoped wrapper
 * around the shared [HttpClient]: every call is routed through [safeApiCall] so failures surface
 * as a typed [NetworkResult] instead of a thrown exception.
 */
class CharacterApiService(private val httpClient: HttpClient) {

    /**
     * Fetches a single page of characters. All filter parameters are optional and, when non-null,
     * are passed through to the API as-is (`name`/`type` support partial, case-insensitive
     * matches server-side; `status`/`gender` expect one of the API's known enum values). A `null`
     * value simply omits that query parameter rather than sending an empty one.
     */
    suspend fun getCharacters(
        page: Int? = null,
        name: String? = null,
        status: String? = null,
        species: String? = null,
        type: String? = null,
        gender: String? = null,
    ): NetworkResult<PagedResponseDto<CharacterDto>> = safeApiCall {
        httpClient.get("${RickAndMortyApiConfig.BASE_URL}/character") {
            page?.let { parameter("page", it) }
            name?.let { parameter("name", it) }
            status?.let { parameter("status", it) }
            species?.let { parameter("species", it) }
            type?.let { parameter("type", it) }
            gender?.let { parameter("gender", it) }
        }.body<PagedResponseDto<CharacterDto>>()
    }

    /** Fetches a single character by its numeric id. */
    suspend fun getCharacterById(id: Int): NetworkResult<CharacterDto> = safeApiCall {
        httpClient.get("${RickAndMortyApiConfig.BASE_URL}/character/$id").body<CharacterDto>()
    }
}
