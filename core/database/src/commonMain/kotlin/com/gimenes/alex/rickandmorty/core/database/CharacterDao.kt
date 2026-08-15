package com.gimenes.alex.rickandmorty.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the character cache ([CharacterEntity], issue #5).
 *
 * Writes use [Upsert] (insert-or-replace keyed on the primary key, `id`) rather than `@Insert`, so
 * refetching/recaching a character that's already stored updates the existing row in place instead
 * of failing or duplicating it - the acceptance criteria for this issue is explicit that upserting
 * an already-cached id "does not duplicate rows".
 */
@Dao
interface CharacterDao {

    @Upsert
    suspend fun upsert(character: CharacterEntity)

    @Upsert
    suspend fun upsertAll(characters: List<CharacterEntity>)

    @Query("SELECT * FROM character ORDER BY id ASC")
    fun getAll(): Flow<List<CharacterEntity>>

    /**
     * Filters by [status] and/or [species]; either (or both) may be left `null` to skip that
     * filter, per the `WHERE (:status IS NULL OR status = :status) AND (...)` pattern - the
     * standard way to express optional filter params in a single Room query.
     */
    @Query(
        "SELECT * FROM character " +
            "WHERE (:status IS NULL OR status = :status) " +
            "AND (:species IS NULL OR species = :species) " +
            "ORDER BY id ASC"
    )
    fun getFiltered(status: String? = null, species: String? = null): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM character WHERE id = :id")
    suspend fun getById(id: Int): CharacterEntity?
}
