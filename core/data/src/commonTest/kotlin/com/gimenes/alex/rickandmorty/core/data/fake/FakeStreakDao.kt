package com.gimenes.alex.rickandmorty.core.data.fake

import com.gimenes.alex.rickandmorty.core.database.StreakDao
import com.gimenes.alex.rickandmorty.core.database.StreakEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Hand-rolled in-memory test double for [StreakDao] (issue #9), same rationale as
 * [FakeCharacterDao]/[FakeQuizResultDao]. Deliberately as dumb/mechanical as the real DAO (see
 * [StreakDao]'s kdoc): [upsert] simply replaces whatever single row is held, with no
 * increment/reset/best-update logic of its own - that logic is what
 * `GameStateRepositoryImpl`'s tests are actually exercising against this fake.
 */
class FakeStreakDao(seed: StreakEntity? = null) : StreakDao {

    private val state = MutableStateFlow(seed)

    override suspend fun upsert(streak: StreakEntity) {
        state.value = streak
    }

    override fun getStreak(): Flow<StreakEntity?> = state
}
