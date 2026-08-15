package com.gimenes.alex.rickandmorty.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

const val RICK_AND_MORTY_DB_FILE_NAME = "rickandmorty.db"

/**
 * The app's real Room database. Started as infrastructure-only (issue #4): DB instantiation,
 * driver setup, and DI wiring, with [SpikeItemEntity]/[SpikeItemDao] kept here purely as a
 * placeholder because Room's KSP processor requires `@Database.entities` to be non-empty
 * ("@Database annotation must specify list of entities", verified by attempting an empty list
 * before settling on this). Issue #5 replaced that placeholder with the real character cache
 * schema ([CharacterEntity]/[CharacterDao]). Issue #6 adds the local game-state schema alongside
 * it: [QuizResultEntity]/[QuizResultDao] (quiz result history) and [StreakEntity]/[StreakDao]
 * (guess-character streak state) - bumped to version 3 for this schema change. There's no shipped
 * data to migrate yet (app not yet released), so no `Migration` is defined for either version
 * bump.
 */
@Database(
    entities = [CharacterEntity::class, QuizResultEntity::class, StreakEntity::class],
    version = 3,
    exportSchema = true
)
@ConstructedBy(RickAndMortyDatabaseConstructor::class)
abstract class RickAndMortyDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun quizResultDao(): QuizResultDao
    abstract fun streakDao(): StreakDao
}

/**
 * Room's KSP compiler generates the `actual` implementation of this object for each
 * compiled target (Android, iOS). Nothing to implement here manually.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object RickAndMortyDatabaseConstructor : RoomDatabaseConstructor<RickAndMortyDatabase> {
    override fun initialize(): RickAndMortyDatabase
}

fun RoomDatabase.Builder<RickAndMortyDatabase>.withDefaults(
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO
): RickAndMortyDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryDispatcher)
        .build()
