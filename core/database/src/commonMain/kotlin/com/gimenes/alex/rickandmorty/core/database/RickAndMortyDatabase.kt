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
 * The app's real Room database. This is infrastructure-only for now (issue #4): DB
 * instantiation, driver setup, and DI wiring. It has no real schema yet - [SpikeItemEntity] /
 * [SpikeItemDao] are kept here purely as a placeholder because Room's KSP processor requires
 * `@Database.entities` to be non-empty ("@Database annotation must specify list of entities",
 * verified by attempting an empty list before settling on this). The character cache schema
 * (issue #5) and game-state schema (issue #6) will replace this placeholder with real
 * `@Entity`/`@Dao` declarations.
 */
@Database(entities = [SpikeItemEntity::class], version = 1, exportSchema = true)
@ConstructedBy(RickAndMortyDatabaseConstructor::class)
abstract class RickAndMortyDatabase : RoomDatabase() {
    abstract fun spikeItemDao(): SpikeItemDao
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
