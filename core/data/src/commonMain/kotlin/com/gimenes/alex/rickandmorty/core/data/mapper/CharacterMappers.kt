package com.gimenes.alex.rickandmorty.core.data.mapper

import com.gimenes.alex.rickandmorty.core.database.CharacterEntity
import com.gimenes.alex.rickandmorty.core.domain.model.Character
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterGender
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterStatus
import com.gimenes.alex.rickandmorty.core.network.dto.CharacterDto

/**
 * DTO -> domain and Entity <-> domain mappers for [Character] (issue #7).
 *
 * These live in `core:data` rather than `core:domain` or `core:network`/`core:database`
 * deliberately: `core:domain` must have zero Ktor/Room/DTO/Entity imports (see [Character]'s kdoc),
 * so it can't host a mapper that references [CharacterDto] or [CharacterEntity]. `core:data` is the
 * module that already depends on `core:domain` plus both `core:network` and `core:database`, so it's
 * the natural home for translating between the outer-layer shapes and the domain model.
 *
 * [status]/[gender] parsing (both directions) is delegated to [CharacterStatus.fromRaw]/
 * [CharacterGender.fromRaw] and their `apiValue` property, which live on the enums themselves in
 * `core:domain` - keeping the string<->enum contract defined in exactly one place rather than
 * duplicated here.
 */

/**
 * Maps the network wire shape to the domain model. [CharacterDto.origin]/[CharacterDto.location]
 * are flattened from their `{name, url}` shape down to just the display name, matching what the
 * domain model (and the offline cache) need.
 */
fun CharacterDto.toDomain(): Character = Character(
    id = id,
    name = name,
    status = CharacterStatus.fromRaw(status),
    species = species,
    type = type,
    gender = CharacterGender.fromRaw(gender),
    origin = origin.name,
    location = location.name,
    image = image,
    episode = episode,
    url = url,
    created = created,
)

/**
 * Maps a cached row back to the domain model.
 */
fun CharacterEntity.toDomain(): Character = Character(
    id = id,
    name = name,
    status = CharacterStatus.fromRaw(status),
    species = species,
    type = type,
    gender = CharacterGender.fromRaw(gender),
    origin = originName,
    location = locationName,
    image = imageUrl,
    episode = emptyList(),
    url = "",
    created = "",
)

/**
 * Maps the domain model to the persistence shape for writing/refreshing the cache. [cachedAt] is
 * supplied by the caller (the repository, typically `System.currentTimeMillis()`/
 * `Clock.System.now()` at write time) rather than being derivable from the domain model itself,
 * since [Character] has no notion of cache freshness.
 *
 * [status]/[gender] are serialized back via [CharacterStatus.apiValue]/[CharacterGender.apiValue] -
 * i.e. the original API casing ("Alive", "Dead", "unknown", ...) rather than the Kotlin enum name -
 * for consistency with what's already cached and with what the DTO layer produces, since both
 * DTO -> entity (issue #8) and domain -> entity writes should agree on the same on-disk string
 * representation.
 *
 * [episode]/[url]/[created] are intentionally not persisted: [CharacterEntity] is a deliberately
 * flattened offline-display shape (see its kdoc) that doesn't carry those fields, so they're dropped
 * here rather than added to the entity.
 */
fun Character.toEntity(cachedAt: Long): CharacterEntity = CharacterEntity(
    id = id,
    name = name,
    status = status.apiValue,
    species = species,
    type = type,
    gender = gender.apiValue,
    originName = origin,
    locationName = location,
    imageUrl = image,
    cachedAt = cachedAt,
)
