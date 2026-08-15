package com.gimenes.alex.rickandmorty.core.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * Proves the game-state schema (issue #6: [QuizResultEntity]/[QuizResultDao] and
 * [StreakEntity]/[StreakDao]) survives process death, per this issue's acceptance criteria.
 *
 * An instrumented test can't literally kill and restart the app process. The accepted way to
 * verify Room persistence survives process death is instead to: write data, [close][
 * androidx.room.RoomDatabase.close] that database connection (releasing all in-memory state),
 * then open a *new*, independent [RickAndMortyDatabase] instance pointing at the same on-disk
 * database file/name, and confirm the data is still readable through it. A fresh connection to
 * the same file has no access to the previous instance's memory - the only way the data can
 * still be there is if it was actually durably written to disk, which is the real guarantee that
 * matters for process death. This is why this test deliberately builds the database with
 * [getRickAndMortyDatabaseBuilder] (the same on-disk, file-backed builder the app uses via
 * [com.gimenes.alex.rickandmorty.core.database.di.platformDatabaseModule]) rather than
 * [androidx.room.Room.inMemoryDatabaseBuilder] like the other DAO instrumented tests in this
 * module use for speed/isolation - an in-memory database is destroyed on `close()` and would
 * NOT survive this test, so using it here would make the test pass without proving anything.
 */
@RunWith(AndroidJUnit4::class)
class GameStatePersistenceInstrumentedTest {

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        // Start from a clean on-disk file so this test is isolated from any other test/run that
        // touched the same fixed database name/path on the device.
        context.deleteDatabase(RICK_AND_MORTY_DB_FILE_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(RICK_AND_MORTY_DB_FILE_NAME)
    }

    private fun openDatabase(): RickAndMortyDatabase =
        getRickAndMortyDatabaseBuilder(context).withDefaults()

    @Test
    fun quizResultHistory_survivesClosingAndReopeningTheDatabase() = runTest {
        val firstConnection = openDatabase()
        firstConnection.quizResultDao().insert(
            QuizResultEntity(category = "character", score = 8, total = 10, completedAt = 1_000L)
        )
        // Closes the connection entirely - no shared in-memory state carries over to the next
        // Room instance opened below.
        firstConnection.close()

        val secondConnection = openDatabase()
        val history = secondConnection.quizResultDao().getHistory().first()
        secondConnection.close()

        assertEquals(1, history.size)
        assertEquals(8, history.single().score)
        assertEquals(10, history.single().total)
        assertEquals(1_000L, history.single().completedAt)
    }

    @Test
    fun streakState_survivesClosingAndReopeningTheDatabase() = runTest {
        val firstConnection = openDatabase()
        firstConnection.streakDao().upsert(
            StreakEntity(currentStreak = 6, bestStreak = 9, updatedAt = 2_000L)
        )
        firstConnection.close()

        val secondConnection = openDatabase()
        val streak = secondConnection.streakDao().getStreak().first()
        secondConnection.close()

        assertEquals(6, streak?.currentStreak)
        assertEquals(9, streak?.bestStreak)
        assertEquals(2_000L, streak?.updatedAt)
    }
}
