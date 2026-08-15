package com.gimenes.alex.rickandmorty.core.network.dto

import kotlinx.serialization.Serializable

/**
 * The `{name, url}` reference shape the Rick and Morty API uses for a character's `origin` and
 * `location`, and (in later issues) for an episode's `characters` back-references. When a
 * character's origin/location is unknown the API represents that with `name: "unknown"` and
 * `url: ""` rather than omitting the object entirely, so both fields default to an empty string
 * instead of being modeled as nullable.
 */
@Serializable
data class LocationReferenceDto(
    val name: String = "",
    val url: String = "",
)
