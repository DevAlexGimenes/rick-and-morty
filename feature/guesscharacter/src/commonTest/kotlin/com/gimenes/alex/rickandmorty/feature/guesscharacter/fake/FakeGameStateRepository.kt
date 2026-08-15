package com.gimenes.alex.rickandmorty.feature.guesscharacter.fake

import com.gimenes.alex.rickandmorty.core.domain.model.QuizResult
import com.gimenes.alex.rickandmorty.core.domain.model.Streak
import com.gimenes.alex.rickandmorty.core.domain.repository.GameStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Hand-rolled in-memory fake of [GameStateRepository] for testing
 * [com.gimenes.alex.rickandmorty.feature.guesscharacter.GuessCharacterViewModel] (issue #16) -
 * mirrors `core:domain`'s own `FakeGameStateRepository` test fixture (issue #13), duplicated
 * locally because commonTest sources aren't shared across modules in this project's Gradle setup.
 *
 * Unlike `feature:quiz`'s local fake (which stubs the streak methods as no-ops, since Trivia never
 * touches the streak), this fake *does* reimplement the increment/reset/best-update streak
 * arithmetic that [GameStateRepository]'s kdoc assigns to the repository layer - Guess the
 * Character is exactly the feature that exercises it, so a no-op stub here would make every streak
 * test meaningless. Quiz-result history is, conversely, stubbed as unused since this feature never
 * touches it.
 */
class FakeGameStateRepository(
    initialStreak: Streak? = null,
) : GameStateRepository {

    private val streakState = MutableStateFlow(initialStreak)
    private var tick = 0L

    /** Test-only convenience accessor for the current streak snapshot. */
    val currentStreak: Streak? get() = streakState.value

    override suspend fun recordQuizResult(score: Int, total: Int) = Unit

    override fun getQuizHistory(): Flow<List<QuizResult>> = MutableStateFlow(emptyList())

    override suspend fun getBestScore(): QuizResult? = null

    override fun getStreak(): Flow<Streak?> = streakState

    override suspend fun recordCorrectGuess() {
        val current = streakState.value
        val nextCurrent = (current?.current ?: 0) + 1
        val nextBest = maxOf(current?.best ?: 0, nextCurrent)
        streakState.value = Streak(current = nextCurrent, best = nextBest, updatedAt = ++tick)
    }

    override suspend fun recordIncorrectGuess() {
        val current = streakState.value
        streakState.value = Streak(current = 0, best = current?.best ?: 0, updatedAt = ++tick)
    }
}
