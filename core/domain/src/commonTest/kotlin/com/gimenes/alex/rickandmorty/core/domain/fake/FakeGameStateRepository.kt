package com.gimenes.alex.rickandmorty.core.domain.fake

import com.gimenes.alex.rickandmorty.core.domain.model.QuizResult
import com.gimenes.alex.rickandmorty.core.domain.model.Streak
import com.gimenes.alex.rickandmorty.core.domain.repository.GameStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Hand-rolled in-memory fake of [GameStateRepository] for testing use cases that depend on it
 * (issue #13). Fakes the domain contract directly - not `core:data`'s `FakeQuizResultDao`/
 * `FakeStreakDao`, which fake Room DAOs one layer below the interface `core:domain` actually depends
 * on, and which `core:domain` must not reference (no dependency on `core:data`/`core:database`).
 *
 * Unlike those two DAO fakes (deliberately dumb, mirroring the real DAOs - see their kdocs), this
 * fake *does* reimplement the increment/reset/best-update streak arithmetic that
 * [GameStateRepository]'s kdoc assigns to the repository layer (mirroring `GameStateRepositoryImpl`).
 * That small duplication is deliberate: it lets tests exercise a realistic correct/incorrect
 * sequence end-to-end through [com.gimenes.alex.rickandmorty.core.domain.usecase.RecordGuessOutcomeUseCase]
 * and assert on the resulting current/best streak, directly demonstrating "best streak updates only
 * when current exceeds prior best" at the use-case layer - not merely that the use case delegated to
 * the right repository method.
 *
 * A simple incrementing counter stands in for a wall clock (each recorded event gets the next tick),
 * matching the "most recently completed first" / "last write wins" ordering the real implementation
 * derives from actual timestamps, without pulling `kotlinx-datetime`'s `Clock` into this fake.
 */
class FakeGameStateRepository : GameStateRepository {

    private val historyState = MutableStateFlow<List<QuizResult>>(emptyList())
    private val streakState = MutableStateFlow<Streak?>(null)
    private var tick = 0L

    override suspend fun recordQuizResult(score: Int, total: Int) {
        val result = QuizResult(score = score, total = total, completedAt = nextTick())
        historyState.update { history -> listOf(result) + history }
    }

    override fun getQuizHistory(): Flow<List<QuizResult>> = historyState

    override suspend fun getBestScore(): QuizResult? = historyState.value.maxByOrNull { it.score }

    override fun getStreak(): Flow<Streak?> = streakState

    override suspend fun recordCorrectGuess() {
        val current = streakState.value
        val nextCurrent = (current?.current ?: 0) + 1
        val nextBest = maxOf(current?.best ?: 0, nextCurrent)
        streakState.value = Streak(current = nextCurrent, best = nextBest, updatedAt = nextTick())
    }

    override suspend fun recordIncorrectGuess() {
        val current = streakState.value
        streakState.value = Streak(current = 0, best = current?.best ?: 0, updatedAt = nextTick())
    }

    private fun nextTick(): Long = ++tick
}
