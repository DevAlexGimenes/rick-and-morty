package com.gimenes.alex.rickandmorty.core.data.fake

import com.gimenes.alex.rickandmorty.core.database.QuizResultDao
import com.gimenes.alex.rickandmorty.core.database.QuizResultEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Hand-rolled in-memory test double for [QuizResultDao] (issue #9), same rationale as
 * [FakeCharacterDao] (issue #8): `core:data`'s `commonTest` can't spin up a real Room instance, and
 * `QuizResultDao`'s real SQL behavior is already covered by `core:database`'s instrumented tests
 * (issue #6). This fake only needs to faithfully reproduce [insert]'s append-only semantics and
 * [getHistory]'s most-recent-first ordering contract.
 *
 * Auto-generated ids are simulated with a simple incrementing counter, mirroring Room's
 * `@PrimaryKey(autoGenerate = true)`.
 */
class FakeQuizResultDao(seed: List<QuizResultEntity> = emptyList()) : QuizResultDao {

    private val state = MutableStateFlow(seed)
    private var nextId = (seed.maxOfOrNull { it.id } ?: 0L) + 1L

    override suspend fun insert(result: QuizResultEntity) {
        state.update { current -> current + result.copy(id = nextId++) }
    }

    override fun getHistory(): Flow<List<QuizResultEntity>> =
        state.map { list -> list.sortedByDescending { it.completedAt } }

    override suspend fun getBestScore(): QuizResultEntity? =
        state.value.sortedWith(
            compareByDescending<QuizResultEntity> { it.score }.thenByDescending { it.completedAt }
        ).firstOrNull()
}
