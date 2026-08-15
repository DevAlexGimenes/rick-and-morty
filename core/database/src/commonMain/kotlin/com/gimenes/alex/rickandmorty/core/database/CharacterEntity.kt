package com.gimenes.alex.rickandmorty.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache row for a single Rick and Morty character (issue #5).
 *
 * This is the persistence-layer shape, not a mirror of the network DTO
 * ([com.gimenes.alex.rickandmorty.core.network.dto.CharacterDto] in `core:network`) - `core:database`
 * intentionally has no dependency on `core:network`, so it knows nothing about the wire format.
 * Mapping DTO -> entity is a `core:data` repository concern (issue #8), since that module is the
 * one that depends on both layers.
 *
 * Covers the fields the app needs to display/query offline: identity, the status/species/type/gender
 * classification fields (status and species are also the two supported filter columns, see
 * [CharacterDao]), a flattened origin/location name (the API's richer `{name, url}` reference is not
 * needed for offline display), the image URL, and [cachedAt] - an epoch-millis freshness timestamp
 * set by the repository each time a row is written, so callers can later decide whether the cached
 * data is stale enough to warrant a refetch.
 */
@Entity(tableName = "character")
data class CharacterEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val type: String,
    val gender: String,
    val originName: String,
    val locationName: String,
    val imageUrl: String,
    val cachedAt: Long
)
