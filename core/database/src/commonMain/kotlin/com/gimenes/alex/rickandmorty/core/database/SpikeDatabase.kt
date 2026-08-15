package com.gimenes.alex.rickandmorty.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

const val SPIKE_DB_FILE_NAME = "rickandmorty_spike.db"

@Database(entities = [SpikeItemEntity::class], version = 1, exportSchema = true)
@ConstructedBy(SpikeDatabaseConstructor::class)
abstract class SpikeDatabase : RoomDatabase() {
    abstract fun spikeItemDao(): SpikeItemDao
}

/**
 * Room's KSP compiler generates the `actual` implementation of this object for each
 * compiled target (Android, iOS). Nothing to implement here manually.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SpikeDatabaseConstructor : RoomDatabaseConstructor<SpikeDatabase> {
    override fun initialize(): SpikeDatabase
}

fun RoomDatabase.Builder<SpikeDatabase>.withDefaults(
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO
): SpikeDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryDispatcher)
        .build()
