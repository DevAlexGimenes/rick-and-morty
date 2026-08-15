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
 * Exercises [StreakDao] against a real, in-memory [RickAndMortyDatabase] - same pattern as
 * [CharacterDaoInstrumentedTest].
 *
 * Covers this issue's (#6) acceptance criteria for the streak side: initial (no row yet) state
 * is handled without crashing, upsert + read-back of the single-row state, updating current
 * streak, and a mechanical round-trip proving [StreakDao.upsert] persists whatever it's given
 * verbatim (the "only update best if greater" business rule intentionally lives in the #13 use
 * case, not this DAO - see [StreakDao] kdoc).
 */
@RunWith(AndroidJUnit4::class)
class StreakDaoInstrumentedTest {

    private lateinit var database: RickAndMortyDatabase
    private lateinit var dao: StreakDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder<RickAndMortyDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.streakDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getStreak_withNoRowYet_emitsNullWithoutCrashing() = runTest {
        assertNull(dao.getStreak().first())
    }

    @Test
    fun upsertAndGetStreak_roundTripsFields() = runTest {
        dao.upsert(streak(currentStreak = 3, bestStreak = 5, updatedAt = 1_000L))

        val result = dao.getStreak().first()

        assertEquals(3, result?.currentStreak)
        assertEquals(5, result?.bestStreak)
        assertEquals(1_000L, result?.updatedAt)
    }

    @Test
    fun upsert_calledAgain_updatesTheSameSingletonRowRatherThanAddingAnother() = runTest {
        dao.upsert(streak(currentStreak = 1, bestStreak = 1, updatedAt = 1_000L))
        dao.upsert(streak(currentStreak = 4, bestStreak = 4, updatedAt = 2_000L))

        val result = dao.getStreak().first()

        assertEquals(4, result?.currentStreak)
        assertEquals(4, result?.bestStreak)
        assertEquals(2_000L, result?.updatedAt)
    }

    @Test
    fun upsert_isMechanicalAndDoesNotGuardBestStreakAgainstDecreasing() = runTest {
        // Documents the deliberate design choice: the DAO does not enforce "best only rises" -
        // it persists exactly what it's given, verbatim. If this ever starts failing because a
        // guard was added to the DAO, that's a real design change that should be called out,
        // not incidental.
        dao.upsert(streak(currentStreak = 10, bestStreak = 10, updatedAt = 1_000L))
        dao.upsert(streak(currentStreak = 0, bestStreak = 2, updatedAt = 2_000L))

        val result = dao.getStreak().first()

        assertEquals(0, result?.currentStreak)
        assertEquals(2, result?.bestStreak)
    }

    private fun streak(currentStreak: Int, bestStreak: Int, updatedAt: Long) = StreakEntity(
        currentStreak = currentStreak,
        bestStreak = bestStreak,
        updatedAt = updatedAt
    )
}
