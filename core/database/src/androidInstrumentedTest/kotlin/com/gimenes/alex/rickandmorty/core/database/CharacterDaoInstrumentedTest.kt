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
 * Exercises [CharacterDao] against a real, in-memory [RickAndMortyDatabase] built directly with
 * [Room.inMemoryDatabaseBuilder] (rather than through Koin/[getRickAndMortyDatabaseBuilder] like
 * [RickAndMortyDatabaseInstrumentedTest]) - an in-memory DB is discarded automatically once
 * closed, which keeps each test isolated without needing to manage an on-disk file/context
 * plumbing for tests that don't otherwise need DI. Still an instrumented test, not a Robolectric
 * one: the Bundled SQLite driver's native library only extracts on-device (see
 * [SpikeDatabaseInstrumentedTest]).
 *
 * Covers this issue's (#5) acceptance criteria: insert+query round-trip, filter-by-status/species
 * (individually and combined), and upsert-on-existing-id not duplicating rows.
 */
@RunWith(AndroidJUnit4::class)
class CharacterDaoInstrumentedTest {

    private lateinit var database: RickAndMortyDatabase
    private lateinit var dao: CharacterDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder<RickAndMortyDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.characterDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryAll_roundTripsAllFields() = runTest {
        val rick = character(id = 1, name = "Rick Sanchez", cachedAt = 1_000L)
        val morty = character(id = 2, name = "Morty Smith", cachedAt = 2_000L)

        dao.upsertAll(listOf(rick, morty))

        val all = dao.getAll().first()

        assertEquals(2, all.size)
        assertEquals(setOf(rick, morty), all.toSet())
    }

    @Test
    fun getFiltered_byStatusOnly_returnsMatchingSubset() = runTest {
        seedMixedFixtures()

        val alive = dao.getFiltered(status = "Alive").first()

        assertEquals(setOf(1, 3), alive.map { it.id }.toSet())
        assertEquals(setOf("Alive"), alive.map { it.status }.toSet())
    }

    @Test
    fun getFiltered_bySpeciesOnly_returnsMatchingSubset() = runTest {
        seedMixedFixtures()

        val humans = dao.getFiltered(species = "Human").first()

        assertEquals(setOf(1, 2), humans.map { it.id }.toSet())
        assertEquals(setOf("Human"), humans.map { it.species }.toSet())
    }

    @Test
    fun getFiltered_byStatusAndSpecies_returnsIntersection() = runTest {
        seedMixedFixtures()

        val aliveHumans = dao.getFiltered(status = "Alive", species = "Human").first()

        assertEquals(listOf(1), aliveHumans.map { it.id })
    }

    @Test
    fun getFiltered_withNoFilters_returnsEverything() = runTest {
        seedMixedFixtures()

        val all = dao.getFiltered().first()

        assertEquals(4, all.size)
    }

    @Test
    fun upsert_withExistingId_updatesInPlaceWithoutDuplicating() = runTest {
        dao.upsert(character(id = 1, name = "Rick Sanchez", status = "Alive", cachedAt = 1_000L))

        val updated = character(id = 1, name = "Rick Sanchez (C-137)", status = "Dead", cachedAt = 5_000L)
        dao.upsert(updated)

        val all = dao.getAll().first()

        assertEquals(1, all.size)
        assertEquals(updated, all.single())
    }

    @Test
    fun upsertAll_withMixOfNewAndExistingIds_updatesExistingAndInsertsNew() = runTest {
        dao.upsertAll(
            listOf(
                character(id = 1, name = "Rick Sanchez"),
                character(id = 2, name = "Morty Smith")
            )
        )

        dao.upsertAll(
            listOf(
                character(id = 1, name = "Rick Sanchez (updated)"),
                character(id = 3, name = "Summer Smith")
            )
        )

        val all = dao.getAll().first()

        assertEquals(3, all.size)
        assertEquals(
            setOf("Rick Sanchez (updated)", "Morty Smith", "Summer Smith"),
            all.map { it.name }.toSet()
        )
    }

    @Test
    fun getById_returnsMatchingRow() = runTest {
        dao.upsert(character(id = 42, name = "Birdperson"))

        val result = dao.getById(42)

        assertEquals("Birdperson", result?.name)
    }

    @Test
    fun getById_withNonExistentId_returnsNull() = runTest {
        dao.upsert(character(id = 1, name = "Rick Sanchez"))

        val result = dao.getById(999)

        assertNull(result)
    }

    /**
     * Mixed fixture set used by the filter tests: two humans (one alive, one dead), one alive
     * alien, one dead alien - enough combinations to distinguish status-only, species-only, and
     * combined filtering.
     */
    private suspend fun seedMixedFixtures() {
        dao.upsertAll(
            listOf(
                character(id = 1, name = "Rick Sanchez", status = "Alive", species = "Human"),
                character(id = 2, name = "Dead Human", status = "Dead", species = "Human"),
                character(id = 3, name = "Alive Alien", status = "Alive", species = "Alien"),
                character(id = 4, name = "Dead Alien", status = "Dead", species = "Alien")
            )
        )
    }

    private fun character(
        id: Int,
        name: String,
        status: String = "Alive",
        species: String = "Human",
        type: String = "",
        gender: String = "Male",
        originName: String = "Earth (C-137)",
        locationName: String = "Citadel of Ricks",
        imageUrl: String = "https://rickandmortyapi.com/api/character/avatar/$id.jpeg",
        cachedAt: Long = 0L
    ) = CharacterEntity(
        id = id,
        name = name,
        status = status,
        species = species,
        type = type,
        gender = gender,
        originName = originName,
        locationName = locationName,
        imageUrl = imageUrl,
        cachedAt = cachedAt
    )
}
