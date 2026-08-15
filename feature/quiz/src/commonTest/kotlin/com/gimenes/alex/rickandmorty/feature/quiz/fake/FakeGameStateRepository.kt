package com.gimenes.alex.rickandmorty.feature.quiz.fake

import com.gimenes.alex.rickandmorty.core.domain.model.QuizResult
import com.gimenes.alex.rickandmorty.core.domain.model.Streak
import com.gimenes.alex.rickandmorty.core.domain.repository.GameStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Hand-rolled in-memory fake of [GameStateRepository] for testing [com.gimenes.alex.rickandmorty.feature.quiz.QuizViewModel]
 * (issue #15) - mirrors `core:domain`'s own `FakeGameStateRepository` test fixture (issue #13),
 * duplicated locally because commonTest sources aren't shared across modules in this project's
 * Gradle setup. Streak methods are stubbed no-ops: Trivia Quiz never touches the guess-character
 * streak (see [GameStateRepository]'s kdoc), so this fake only needs to behave correctly for
 * [recordQuizResult]/[getQuizHistory]/[getBestScore].
 */
class FakeGameStateRepository : GameStateRepository {

    private val historyState = MutableStateFlow<List<QuizResult>>(emptyList())
    private var tick = 0L

    /** Test-only convenience accessor for the current history snapshot (most recent first). */
    val history: List<QuizResult> get() = historyState.value

    override suspend fun recordQuizResult(score: Int, total: Int) {
        val result = QuizResult(score = score, total = total, completedAt = ++tick)
        historyState.update { history -> listOf(result) + history }
    }

    override fun getQuizHistory(): Flow<List<QuizResult>> = historyState

    override suspend fun getBestScore(): QuizResult? = historyState.value.maxByOrNull { it.score }

    override fun getStreak(): Flow<Streak?> = MutableStateFlow(null)

    override suspend fun recordCorrectGuess() = Unit

    override suspend fun recordIncorrectGuess() = Unit
}
