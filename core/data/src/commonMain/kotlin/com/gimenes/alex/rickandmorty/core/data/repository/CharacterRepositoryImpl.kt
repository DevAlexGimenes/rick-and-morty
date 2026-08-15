package com.gimenes.alex.rickandmorty.core.data.repository

import com.gimenes.alex.rickandmorty.core.data.mapper.toDomain
import com.gimenes.alex.rickandmorty.core.data.mapper.toEntity
import com.gimenes.alex.rickandmorty.core.database.CharacterDao
import com.gimenes.alex.rickandmorty.core.domain.model.Character
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterGender
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterStatus
import com.gimenes.alex.rickandmorty.core.domain.repository.CharacterRepository
import com.gimenes.alex.rickandmorty.core.network.NetworkResult
import com.gimenes.alex.rickandmorty.core.network.api.CharacterApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

/**
 * Network-first, cache-backed [CharacterRepository] (issue #8): combines [CharacterApiService]
 * (issues #2/#3) with [CharacterDao] (issue #5) so callers get an offline-first read path without
 * having to know either layer exists.
 *
 * ### Contract per call
 * - **Fetch succeeds:** the DTOs are mapped to [Character], written through to the cache
 *   ([CharacterDao.upsert]/[CharacterDao.upsertAll], keyed by id so re-fetching never duplicates
 *   rows - see [CharacterDao]'s kdoc), and the freshly-fetched data is what's returned/emitted.
 *   The cache is updated as a side effect of a successful fetch; it is not the source of truth for
 *   the value returned in this branch.
 * - **Fetch fails** (any [com.gimenes.alex.rickandmorty.core.network.NetworkError] - connectivity,
 *   HTTP, parsing, unknown): the failure is swallowed here rather than propagated. The repository
 *   falls back to whatever's currently cached and returns/emits that instead - per the acceptance
 *   criteria, this must not throw. If the cache is also empty, that's the legitimate "offline
 *   without cache" case and the result is simply empty/`null`, not an exception.
 *
 * [clock] defaults to [Clock.System] and exists purely so tests can inject a fixed/fake clock
 * instead of asserting against wall-clock time when checking [com.gimenes.alex.rickandmorty.core.database.CharacterEntity.cachedAt].
 */
class CharacterRepositoryImpl(
    private val api: CharacterApiService,
    private val dao: CharacterDao,
    private val clock: Clock = Clock.System,
) : CharacterRepository {

    override fun getCharacters(
        name: String?,
        status: CharacterStatus?,
        species: String?,
        type: String?,
        gender: CharacterGender?,
    ): Flow<List<Character>> = flow {
        when (
            val result = api.getCharacters(
                name = name,
                status = status?.apiValue,
                species = species,
                type = type,
                gender = gender?.apiValue,
            )
        ) {
            is NetworkResult.Success -> {
                val characters = result.data.results.map { it.toDomain() }
                if (characters.isNotEmpty()) {
                    dao.upsertAll(characters.map { it.toEntity(cachedAt = nowMillis()) })
                }
                emit(characters)
            }

            is NetworkResult.Failure -> {
                // Offline fallback: the DAO only supports filtering by status/species (issue #5),
                // so name/type/gender filters can't be honored against the cache - documented on
                // CharacterRepository's kdoc.
                val cached = dao.getFiltered(status?.apiValue, species).first()
                emit(cached.map { it.toDomain() })
            }
        }
    }

    override suspend fun getCharacterById(id: Int): Character? =
        when (val result = api.getCharacterById(id)) {
            is NetworkResult.Success -> {
                val character = result.data.toDomain()
                dao.upsert(character.toEntity(cachedAt = nowMillis()))
                character
            }

            is NetworkResult.Failure -> dao.getById(id)?.toDomain()
        }

    private fun nowMillis(): Long = clock.now().toEpochMilliseconds()
}
