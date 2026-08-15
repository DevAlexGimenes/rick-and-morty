package com.gimenes.alex.rickandmorty.core.data.fake

import com.gimenes.alex.rickandmorty.core.database.CharacterDao
import com.gimenes.alex.rickandmorty.core.database.CharacterEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Hand-rolled in-memory test double for [CharacterDao], used by `core:data`'s repository tests
 * (issue #8) in place of a real Room instance.
 *
 * `core:data`'s `commonTest` source set can't spin up a real Android-only Bundled-SQLite Room
 * database - that requires the instrumented-test setup `core:database` already uses for exactly
 * that reason (issue #5's `CharacterDaoInstrumentedTest`). Real DAO/SQL behavior (upsert semantics,
 * the `getFiltered` query, etc.) is already covered there; what these repository tests need to
 * verify is `CharacterRepositoryImpl`'s network-first/cache-fallback *orchestration*, for which a
 * simple backing [MutableStateFlow]-based fake is sufficient and far cheaper to run.
 *
 * Upserts are keyed by [CharacterEntity.id], mirroring the real `@Upsert` DAO's insert-or-replace
 * semantics (no duplicate rows for an id that's written twice).
 */
class FakeCharacterDao(seed: List<CharacterEntity> = emptyList()) : CharacterDao {

    private val state = MutableStateFlow(seed.sortedBy { it.id })

    /** Snapshot of everything currently "persisted", for tests to assert cache writes against. */
    val entities: List<CharacterEntity> get() = state.value

    override suspend fun upsert(character: CharacterEntity) {
        upsertAll(listOf(character))
    }

    override suspend fun upsertAll(characters: List<CharacterEntity>) {
        state.update { current ->
            val byId = current.associateByTo(LinkedHashMap()) { it.id }
            characters.forEach { byId[it.id] = it }
            byId.values.sortedBy { it.id }
        }
    }

    override fun getAll(): Flow<List<CharacterEntity>> = state.map { it.sortedBy { entity -> entity.id } }

    override fun getFiltered(status: String?, species: String?): Flow<List<CharacterEntity>> =
        state.map { list ->
            list.filter { entity ->
                (status == null || entity.status == status) &&
                    (species == null || entity.species == species)
            }.sortedBy { it.id }
        }

    override suspend fun getById(id: Int): CharacterEntity? = state.value.firstOrNull { it.id == id }
}
