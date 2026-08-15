package com.gimenes.alex.rickandmorty.core.domain.model

/**
 * Framework-agnostic domain representation of a Rick and Morty character (issue #7).
 *
 * This is the type the rest of the app (feature modules, use cases) works with - it has zero
 * dependency on the wire format ([com.gimenes.alex.rickandmorty.core.network.dto.CharacterDto] in
 * `core:network`) or the persistence format
 * ([com.gimenes.alex.rickandmorty.core.database.CharacterEntity] in `core:database`). Mapping
 * to/from those outer-layer types is a `core:data` concern, since that's the module that already
 * depends on `core:domain` plus both `core:network` and `core:database` - `core:domain` itself must
 * never import Ktor/Room/DTO/Entity types.
 *
 * [status] and [gender] are modeled as small, fixed-value enums ([CharacterStatus],
 * [CharacterGender]) rather than raw strings, since the API only ever returns a handful of known
 * values (plus `"unknown"`) for them. [species] is deliberately kept as a plain [String]: the show
 * has hundreds of species and the API imposes no fixed set, so an enum would be the wrong tool there.
 * [origin] and [location] are flattened to their display name only (dropping the DTO's `url`
 * reference), matching what [com.gimenes.alex.rickandmorty.core.database.CharacterEntity] already
 * stores for offline use.
 */
data class Character(
    val id: Int,
    val name: String,
    val status: CharacterStatus,
    val species: String,
    val type: String,
    val gender: CharacterGender,
    val origin: String,
    val location: String,
    val image: String,
    val episode: List<String>,
    val url: String,
    val created: String,
)
