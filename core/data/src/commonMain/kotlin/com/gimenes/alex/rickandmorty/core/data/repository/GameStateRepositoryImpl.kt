package com.gimenes.alex.rickandmorty.core.data.repository

import com.gimenes.alex.rickandmorty.core.data.mapper.toDomain
import com.gimenes.alex.rickandmorty.core.database.QuizResultDao
import com.gimenes.alex.rickandmorty.core.database.QuizResultEntity
import com.gimenes.alex.rickandmorty.core.database.StreakDao
import com.gimenes.alex.rickandmorty.core.database.StreakEntity
import com.gimenes.alex.rickandmorty.core.domain.model.QuizResult
import com.gimenes.alex.rickandmorty.core.domain.model.Streak
import com.gimenes.alex.rickandmorty.core.domain.repository.GameStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * [GameStateRepository] implementation (issue #9) wrapping [QuizResultDao]/[StreakDao] (issue #6).
 * Purely local persistence - no network dependency whatsoever, and no relation to
 * `CharacterRepositoryImpl` beyond following the same interface-in-domain/impl-in-data pattern.
 *
 * See [GameStateRepository]'s kdoc for the full reasoning on why the streak's increment/reset/
 * best-update rules are implemented *here* rather than in [StreakDao] (kept deliberately dumb per
 * issue #6) or deferred to issue #13's use case. [recordCorrectGuess]/[recordIncorrectGuess] are
 * the only two places that logic exists; nothing about the DAO changed.
 *
 * [category] defaults to `"character"`, the single live quiz category product scope currently
 * supports (see [QuizResultEntity]'s kdoc) - matching the value already used by
 * `core:database`'s instrumented tests for this column. [recordQuizResult]'s signature deliberately
 * doesn't expose a `category` parameter: nothing in this issue's contract needs a caller to choose
 * one yet, and adding the parameter is a non-breaking, mechanical change for whenever a second
 * category actually ships.
 *
 * [clock] defaults to [Clock.System], same rationale as `CharacterRepositoryImpl.clock`: lets tests
 * inject a fixed clock instead of asserting against wall-clock time.
 */
class GameStateRepositoryImpl(
    private val quizResultDao: QuizResultDao,
    private val streakDao: StreakDao,
    private val clock: Clock = Clock.System,
) : GameStateRepository {

    override suspend fun recordQuizResult(score: Int, total: Int) {
        quizResultDao.insert(
            QuizResultEntity(
                category = DEFAULT_CATEGORY,
                score = score,
                total = total,
                completedAt = nowMillis(),
            )
        )
    }

    override fun getQuizHistory(): Flow<List<QuizResult>> =
        quizResultDao.getHistory().map { history -> history.map { it.toDomain() } }

    override suspend fun getBestScore(): QuizResult? = quizResultDao.getBestScore()?.toDomain()

    override fun getStreak(): Flow<Streak?> = streakDao.getStreak().map { it?.toDomain() }

    override suspend fun recordCorrectGuess() {
        val current = streakDao.getStreak().first()
        val nextCurrent = (current?.currentStreak ?: 0) + 1
        val nextBest = maxOf(current?.bestStreak ?: 0, nextCurrent)
        streakDao.upsert(
            StreakEntity(
                currentStreak = nextCurrent,
                bestStreak = nextBest,
                updatedAt = nowMillis(),
            )
        )
    }

    override suspend fun recordIncorrectGuess() {
        val current = streakDao.getStreak().first()
        streakDao.upsert(
            StreakEntity(
                currentStreak = 0,
                bestStreak = current?.bestStreak ?: 0,
                updatedAt = nowMillis(),
            )
        )
    }

    private fun nowMillis(): Long = clock.now().toEpochMilliseconds()

    private companion object {
        const val DEFAULT_CATEGORY = "character"
    }
}
