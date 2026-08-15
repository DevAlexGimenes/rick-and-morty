package com.gimenes.alex.rickandmorty.core.database

import androidx.test.core.app.ApplicationProvider
import com.gimenes.alex.rickandmorty.core.database.di.databaseModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import kotlin.test.assertEquals

/**
 * Proves that [RickAndMortyDatabase] both instantiates and is retrievable through Koin DI on
 * Android. Originally a smoke test over the issue #4 placeholder schema; now exercises the real
 * character cache schema ([CharacterEntity]/[CharacterDao], issue #5) instead, since the
 * placeholder was removed from [RickAndMortyDatabase]. The more detailed DAO behavior (round-trip
 * field fidelity, filtering, upsert-not-duplicate semantics) lives in
 * [CharacterDaoInstrumentedTest]; this file stays focused on "the real DB instantiates and is
 * injectable end-to-end".
 *
 * Uses a standalone [koinApplication] instance rather than the global `startKoin`/`stopKoin`
 * so this test doesn't depend on / interfere with app-wide Koin state. See
 * [SpikeDatabaseInstrumentedTest] for why this must run as an instrumented test rather than a
 * Robolectric unit test (the Bundled SQLite driver's native library only extracts on-device).
 */
@RunWith(AndroidJUnit4::class)
class RickAndMortyDatabaseInstrumentedTest {

    private lateinit var koinApp: KoinApplication
    private lateinit var database: RickAndMortyDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Each test starts from a clean database file - the previous test's teardown closes
        // the connection, but the on-disk file otherwise persists across test methods since
        // they all resolve to the same fixed database name/path on the device.
        context.deleteDatabase(RICK_AND_MORTY_DB_FILE_NAME)

        koinApp = koinApplication {
            androidContext(context)
            modules(databaseModule)
        }
        database = koinApp.koin.get()
    }

    @After
    fun tearDown() {
        database.close()
        koinApp.close()
    }

    @Test
    fun databaseIsInjectableAndUsable() = runTest {
        // Retrieved via the database instance directly.
        val dao = database.characterDao()
        dao.upsert(smokeTestCharacter(id = 1, name = "Injected Rick"))
        dao.upsert(smokeTestCharacter(id = 2, name = "Injected Morty"))

        val all = dao.getAll().first()

        assertEquals(2, all.size)
        assertEquals(listOf("Injected Rick", "Injected Morty"), all.map { it.name })
    }

    @Test
    fun daoBindingIsAlsoInjectableDirectlyFromKoin() = runTest {
        // Proves the per-DAO Koin binding in databaseModule resolves independently of manually
        // fetching RickAndMortyDatabase and calling the DAO accessor on it.
        val dao: CharacterDao = koinApp.koin.get()

        dao.upsert(smokeTestCharacter(id = 1, name = "Directly Injected Summer"))

        assertEquals(listOf("Directly Injected Summer"), dao.getAll().first().map { it.name })
    }

    private fun smokeTestCharacter(id: Int, name: String) = CharacterEntity(
        id = id,
        name = name,
        status = "Alive",
        species = "Human",
        type = "",
        gender = "Male",
        originName = "Earth",
        locationName = "Earth",
        imageUrl = "https://example.com/$id.png",
        cachedAt = 0L
    )
}
