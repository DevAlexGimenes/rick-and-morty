package com.gimenes.alex.rickandmorty.core.domain.repository

import com.gimenes.alex.rickandmorty.core.domain.model.QuizResult
import com.gimenes.alex.rickandmorty.core.domain.model.Streak
import kotlinx.coroutines.flow.Flow

/**
 * Domain-facing contract for local game-state persistence (issue #9): quiz result history/best
 * score, plus the guess-character streak.
 *
 * A single interface covers both concerns rather than splitting into `QuizResultRepository`/
 * `StreakRepository`: they're both purely-local "game state" with no network dimension (unlike
 * [CharacterRepository], this repository has zero relation to the network layer or to
 * `CharacterRepository` itself), and issue #13's use case is expected to want both together anyway.
 * If a future issue reveals these two concerns actually evolve independently, splitting the
 * interface then is a mechanical refactor - not a reason to preemptively split it now.
 *
 * Like [CharacterRepository], this interface knows nothing about *how* the data is persisted: it
 * must not reference `core:database`'s `QuizResultEntity`/`StreakEntity`/DAOs - `core:domain` has
 * zero dependency on `core:database`. The implementation living in `core:data`
 * (`GameStateRepositoryImpl`) is what actually wraps `QuizResultDao`/`StreakDao` (issue #6).
 *
 * ### Where the streak's increment/reset/best-update rules live
 * `StreakDao.upsert` (issue #6) is deliberately dumb/mechanical - it persists whatever
 * `StreakEntity` it's given, with no "only raise `bestStreak` if greater" guard and no notion of
 * "correct guess" vs "incorrect guess". Issue #6's kdoc originally pointed at the guess-character
 * *use case* (issue #13) as the place that business rule would live. This repository sits one layer
 * above the DAO and is a more natural, and more reusable, home for it: [recordCorrectGuess] and
 * [recordIncorrectGuess] read the current [Streak] via the DAO, compute the next `current`/`best`
 * values (increment-and-raise-best-if-exceeded, or reset-current-while-preserving-best), and persist
 * the result through the still-dumb DAO. Issue #13's use case should therefore be a thin
 * orchestration layer that calls [recordCorrectGuess]/[recordIncorrectGuess] directly rather than
 * reimplementing this arithmetic itself - the state-transition logic belongs here, once, not
 * duplicated at the use-case layer or pushed down into the DAO.
 */
interface GameStateRepository {

    /** Persists a newly completed quiz run as a new history entry. */
    suspend fun recordQuizResult(score: Int, total: Int)

    /** Full quiz history, most recently completed first (matches `QuizResultDao.getHistory()`). */
    fun getQuizHistory(): Flow<List<QuizResult>>

    /** The single highest-scoring quiz run, or `null` if none has been completed yet. */
    suspend fun getBestScore(): QuizResult?

    /** Current streak state, or `null` if no round has ever been recorded. */
    fun getStreak(): Flow<Streak?>

    /**
     * Records a correct guess: increments the current streak by 1, raising the best streak if the
     * new current value exceeds it, and persists both.
     */
    suspend fun recordCorrectGuess()

    /**
     * Records an incorrect guess: resets the current streak to 0 while leaving the best streak
     * untouched, and persists both.
     */
    suspend fun recordIncorrectGuess()
}
