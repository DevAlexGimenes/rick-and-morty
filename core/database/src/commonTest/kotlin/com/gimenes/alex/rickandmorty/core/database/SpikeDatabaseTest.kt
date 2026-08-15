package com.gimenes.alex.rickandmorty.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Room KMP spike: proves that a trivial entity + DAO can write and read back through the
 * Bundled SQLite driver on the host platform the test is compiled/run for. This is the
 * validation gate before building out the real schema in this module.
 */
class SpikeDatabaseTest {

    private fun inMemoryDatabase(): SpikeDatabase =
        Room.inMemoryDatabaseBuilder<SpikeDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()

    @Test
    fun insertAndReadBack() = runTest {
        val database = inMemoryDatabase()
        val dao = database.spikeItemDao()

        dao.insert(SpikeItemEntity(name = "Pickle Rick"))
        dao.insert(SpikeItemEntity(name = "Mr. Meeseeks"))

        val all = dao.getAll()

        assertEquals(2, all.size)
        assertEquals(listOf("Pickle Rick", "Mr. Meeseeks"), all.map { it.name })

        database.close()
    }
}
