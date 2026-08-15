package com.gimenes.alex.rickandmorty.core.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * Room KMP spike (Android side): proves that a trivial entity + DAO can write and read back
 * through the Bundled SQLite driver on a real Android device/emulator (this is the
 * validation gate before building out the real schema in this module).
 *
 * This must run as an instrumented test (`connectedAndroidTest` / `connectedDebugAndroidTest`),
 * not a host-JVM unit test: the Bundled SQLite driver's Android-target `NativeLibraryLoader`
 * relies on the native `.so` being extracted into the app's real jniLibs directory at install
 * time, which only happens on-device (Robolectric does not extract/emulate this, so this test
 * would fail under Robolectric with `UnsatisfiedLinkError: no sqliteJni in java.library.path`
 * despite the code being correct - see FORGE's scaffolding report for the isolated JVM-target
 * proof used to validate this mechanism in an environment without a device/emulator).
 */
@RunWith(AndroidJUnit4::class)
class SpikeDatabaseInstrumentedTest {

    private lateinit var database: SpikeDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = getSpikeDatabaseBuilder(context).withDefaults()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndReadBack() = runTest {
        val dao = database.spikeItemDao()

        dao.insert(SpikeItemEntity(name = "Pickle Rick"))
        dao.insert(SpikeItemEntity(name = "Mr. Meeseeks"))

        val all = dao.getAll()

        assertEquals(2, all.size)
        assertEquals(listOf("Pickle Rick", "Mr. Meeseeks"), all.map { it.name })
    }
}
