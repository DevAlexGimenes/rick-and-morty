package com.gimenes.alex.rickandmorty.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Exercises [QuizResultDao] against a real, in-memory [RickAndMortyDatabase] - same pattern as
 * [CharacterDaoInstrumentedTest] (see its kdoc for why in-memory + instrumented, not
 * Robolectric).
 *
 * Covers this issue's (#6) acceptance criteria for the quiz side: inserting a result and reading
 * it back via history, history ordering (most recent first), and best-score selection from a
 * mixed set of results.
 */
@RunWith(AndroidJUnit4::class)
class QuizResultDaoInstrumentedTest {

    private lateinit var database: RickAndMortyDatabase
    private lateinit var dao: QuizResultDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder<RickAndMortyDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.quizResultDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getBestScore_withNoResults_returnsNull() = runTest {
        assertNull(dao.getBestScore())
    }

    @Test
    fun getHistory_withNoResults_returnsEmptyList() = runTest {
        assertEquals(emptyList(), dao.getHistory().first())
    }

    @Test
    fun insertAndGetHistory_roundTripsASingleResult() = runTest {
        dao.insert(quizResult(score = 7, total = 10, completedAt = 1_000L))

        val history = dao.getHistory().first()

        assertEquals(1, history.size)
        assertEquals(7, history.single().score)
        assertEquals(10, history.single().total)
        assertEquals("character", history.single().category)
    }

    @Test
    fun getHistory_withMultipleResults_isOrderedMostRecentFirst() = runTest {
        dao.insert(quizResult(score = 3, total = 10, completedAt = 1_000L))
        dao.insert(quizResult(score = 9, total = 10, completedAt = 3_000L))
        dao.insert(quizResult(score = 5, total = 10, completedAt = 2_000L))

        val history = dao.getHistory().first()

        assertEquals(listOf(3_000L, 2_000L, 1_000L), history.map { it.completedAt })
    }

    @Test
    fun getBestScore_fromMixedSet_returnsHighestRawScore() = runTest {
        dao.insert(quizResult(score = 3, total = 10, completedAt = 1_000L))
        dao.insert(quizResult(score = 9, total = 10, completedAt = 2_000L))
        // Higher ratio (5/5 == 100%) but lower raw score than the 9/10 run above - "best" is
        // defined as highest raw score (see QuizResultDao kdoc), so the 9/10 run should win.
        dao.insert(quizResult(score = 5, total = 5, completedAt = 3_000L))

        val best = dao.getBestScore()

        assertEquals(9, best?.score)
        assertEquals(10, best?.total)
    }

    @Test
    fun getBestScore_withTiedScores_breaksTieByMostRecent() = runTest {
        dao.insert(quizResult(score = 8, total = 10, completedAt = 1_000L))
        dao.insert(quizResult(score = 8, total = 10, completedAt = 5_000L))

        val best = dao.getBestScore()

        assertEquals(5_000L, best?.completedAt)
    }

    private fun quizResult(
        category: String = "character",
        score: Int,
        total: Int,
        completedAt: Long
    ) = QuizResultEntity(
        category = category,
        score = score,
        total = total,
        completedAt = completedAt
    )
}
