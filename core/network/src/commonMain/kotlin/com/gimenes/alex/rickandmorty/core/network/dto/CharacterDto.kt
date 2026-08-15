package com.gimenes.alex.rickandmorty.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Wire-format representation of a single character as returned by the Rick and Morty API, both
 * from the `/character` list endpoint (as an entry in [PagedResponseDto.results]) and from
 * `/character/{id}`.
 *
 * Every field besides [id] defaults to an empty ([String]) or empty-list value rather than being
 * nullable: the real API represents "unknown"/unset values (e.g. an unknown origin, or a blank
 * `type`) as empty strings, not as missing/null JSON fields, but has in practice been observed to
 * occasionally omit fields outright - the defaults keep decoding resilient to that without
 * weakening the type to nullable everywhere.
 */
@Serializable
data class CharacterDto(
    val id: Int,
    val name: String = "",
    val status: String = "",
    val species: String = "",
    val type: String = "",
    val gender: String = "",
    val origin: LocationReferenceDto = LocationReferenceDto(),
    val location: LocationReferenceDto = LocationReferenceDto(),
    val image: String = "",
    val episode: List<String> = emptyList(),
    val url: String = "",
    val created: String = "",
)
