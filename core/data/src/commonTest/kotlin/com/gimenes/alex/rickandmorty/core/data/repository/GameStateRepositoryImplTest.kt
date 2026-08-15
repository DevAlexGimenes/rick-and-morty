package com.gimenes.alex.rickandmorty.core.data.repository

import com.gimenes.alex.rickandmorty.core.data.fake.FakeQuizResultDao
import com.gimenes.alex.rickandmorty.core.data.fake.FakeStreakDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [GameStateRepositoryImpl] tests (issue #9): [FakeQuizResultDao]/[FakeStreakDao] stand in for
 * the Room DAOs (see their kdocs for why a real Room instance isn't used here). Covers the
 * acceptance criteria - streak increments on consecutive correct rounds and resets on incorrect,
 * quiz history is retrievable in insertion order, best score reflects the highest recorded score -
 * plus confirms the increment/reset/best-update arithmetic actually lives in this repository (not
 * the DAO, not deferred to issue #13).
 */
class GameStateRepositoryImplTest {

    private var fakeNowMillis = 1_000L

    private val fixedClock: Clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(fakeNowMillis)
    }

    private fun repository(
        quizResultDao: FakeQuizResultDao = FakeQuizResultDao(),
        streakDao: FakeStreakDao = FakeStreakDao(),
    ): GameStateRepositoryImpl = GameStateRepositoryImpl(quizResultDao, streakDao, fixedClock)

    // ---- quiz history / best score -------------------------------------------------------------

    @Test
    fun `recording a quiz result adds it to history`() = runTest {
        val repo = repository()

        repo.recordQuizResult(score = 7, total = 10)

        val history = repo.getQuizHistory().first()
        assertEquals(1, history.size)
        assertEquals(7, history[0].score)
        assertEquals(10, history[0].total)
    }

    @Test
    fun `quiz history is returned most recently completed first`() = runTest {
        val repo = repository()

        fakeNowMillis = 1_000L
        repo.recordQuizResult(score = 3, total = 10)
        fakeNowMillis = 2_000L
        repo.recordQuizResult(score = 5, total = 10)
        fakeNowMillis = 3_000L
        repo.recordQuizResult(score = 9, total = 10)

        val history = repo.getQuizHistory().first()

        assertEquals(listOf(9, 5, 3), history.map { it.score })
        assertEquals(listOf(3_000L, 2_000L, 1_000L), history.map { it.completedAt })
    }

    @Test
    fun `best score reflects the highest recorded score across multiple results`() = runTest {
        val repo = repository()

        repo.recordQuizResult(score = 4, total = 10)
        repo.recordQuizResult(score = 9, total = 10)
        repo.recordQuizResult(score = 6, total = 10)

        val best = repo.getBestScore()

        assertEquals(9, best?.score)
    }

    @Test
    fun `best score is null when no quiz has been completed`() = runTest {
        val repo = repository()

        assertNull(repo.getBestScore())
    }

    // ---- streak ---------------------------------------------------------------------------------

    @Test
    fun `getStreak returns null when no round has ever been recorded`() = runTest {
        val repo = repository()

        assertNull(repo.getStreak().first())
    }

    @Test
    fun `recordCorrectGuess repeatedly increments current streak`() = runTest {
        val repo = repository()

        repo.recordCorrectGuess()
        assertEquals(1, repo.getStreak().first()?.current)

        repo.recordCorrectGuess()
        assertEquals(2, repo.getStreak().first()?.current)

        repo.recordCorrectGuess()
        assertEquals(3, repo.getStreak().first()?.current)
    }

    @Test
    fun `recordCorrectGuess raises best streak once current exceeds the prior best`() = runTest {
        val repo = repository()

        repo.recordCorrectGuess() // current=1, best=1
        repo.recordCorrectGuess() // current=2, best=2
        repo.recordIncorrectGuess() // current=0, best=2 (unchanged)
        repo.recordCorrectGuess() // current=1, best=2 (unchanged, below prior best)

        var streak = repo.getStreak().first()
        assertEquals(1, streak?.current)
        assertEquals(2, streak?.best)

        repo.recordCorrectGuess() // current=2, best=2 (ties, not yet exceeded)
        repo.recordCorrectGuess() // current=3, best=3 (exceeds prior best)

        streak = repo.getStreak().first()
        assertEquals(3, streak?.current)
        assertEquals(3, streak?.best)
    }

    @Test
    fun `recordIncorrectGuess resets current streak while preserving best streak`() = runTest {
        val repo = repository()

        repo.recordCorrectGuess()
        repo.recordCorrectGuess()
        repo.recordCorrectGuess() // current=3, best=3

        repo.recordIncorrectGuess()

        val streak = repo.getStreak().first()
        assertEquals(0, streak?.current)
        assertEquals(3, streak?.best)
    }
}
