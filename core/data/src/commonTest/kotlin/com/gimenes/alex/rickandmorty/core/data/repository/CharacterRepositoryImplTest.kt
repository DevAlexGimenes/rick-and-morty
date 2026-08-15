package com.gimenes.alex.rickandmorty.core.data.repository

import com.gimenes.alex.rickandmorty.core.data.fake.FakeCharacterDao
import com.gimenes.alex.rickandmorty.core.database.CharacterEntity
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterGender
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterStatus
import com.gimenes.alex.rickandmorty.core.network.api.CharacterApiService
import com.gimenes.alex.rickandmorty.core.network.applyRickAndMortyDefaults
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [CharacterRepositoryImpl] tests (issue #8): a Ktor [MockEngine] stands in for the network layer
 * (same pattern as `core:network`'s own tests) and [FakeCharacterDao] stands in for the Room cache
 * (see its kdoc for why a real Room instance isn't used here). Covers the three acceptance-criteria
 * scenarios - online success, offline with cache, offline without cache - for both [getCharacters]
 * and [getCharacterById].
 */
class CharacterRepositoryImplTest {

    private val fixedClock: Clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(FIXED_NOW_MILLIS)
    }

    private val listFixture = """
        {
          "info": { "count": 2, "pages": 1, "next": null, "prev": null },
          "results": [
            {
              "id": 1,
              "name": "Rick Sanchez",
              "status": "Alive",
              "species": "Human",
              "type": "",
              "gender": "Male",
              "origin": { "name": "Earth (C-137)", "url": "https://rickandmortyapi.com/api/location/1" },
              "location": { "name": "Citadel of Ricks", "url": "https://rickandmortyapi.com/api/location/3" },
              "image": "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
              "episode": ["https://rickandmortyapi.com/api/episode/1"],
              "url": "https://rickandmortyapi.com/api/character/1",
              "created": "2017-11-04T18:48:46.250Z"
            },
            {
              "id": 2,
              "name": "Morty Smith",
              "status": "Alive",
              "species": "Human",
              "type": "",
              "gender": "Male",
              "origin": { "name": "unknown", "url": "" },
              "location": { "name": "Citadel of Ricks", "url": "https://rickandmortyapi.com/api/location/3" },
              "image": "https://rickandmortyapi.com/api/character/avatar/2.jpeg",
              "episode": ["https://rickandmortyapi.com/api/episode/1"],
              "url": "https://rickandmortyapi.com/api/character/2",
              "created": "2017-11-04T18:50:21.651Z"
            }
          ]
        }
    """.trimIndent()

    private val singleCharacterFixture = """
        {
          "id": 1,
          "name": "Rick Sanchez",
          "status": "Alive",
          "species": "Human",
          "type": "",
          "gender": "Male",
          "origin": { "name": "Earth (C-137)", "url": "https://rickandmortyapi.com/api/location/1" },
          "location": { "name": "Citadel of Ricks", "url": "https://rickandmortyapi.com/api/location/3" },
          "image": "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
          "episode": ["https://rickandmortyapi.com/api/episode/1"],
          "url": "https://rickandmortyapi.com/api/character/1",
          "created": "2017-11-04T18:48:46.250Z"
        }
    """.trimIndent()

    private fun cachedRick(cachedAt: Long = 500L) = CharacterEntity(
        id = 1,
        name = "Rick Sanchez (cached)",
        status = "Alive",
        species = "Human",
        type = "",
        gender = "Male",
        originName = "Earth (C-137)",
        locationName = "Citadel of Ricks",
        imageUrl = "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
        cachedAt = cachedAt,
    )

    private fun MockRequestHandleScope.jsonResponse(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpResponseData = respond(
        content = content,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    private fun repositoryRespondingWith(
        dao: FakeCharacterDao = FakeCharacterDao(),
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): Pair<CharacterRepositoryImpl, FakeCharacterDao> {
        val engine = MockEngine(handler)
        val client = HttpClient(engine) { applyRickAndMortyDefaults() }
        val service = CharacterApiService(client)
        return CharacterRepositoryImpl(service, dao, fixedClock) to dao
    }

    // ---- getCharacters -----------------------------------------------------------------------

    @Test
    fun `online success returns fresh network data and writes through to cache`() = runTest {
        val (repository, dao) = repositoryRespondingWith { jsonResponse(listFixture) }

        val result = repository.getCharacters().first()

        assertEquals(2, result.size)
        assertEquals("Rick Sanchez", result[0].name)
        assertEquals(CharacterStatus.ALIVE, result[0].status)
        assertEquals("Morty Smith", result[1].name)

        assertEquals(2, dao.entities.size)
        assertEquals("Rick Sanchez", dao.entities[0].name)
        assertEquals(FIXED_NOW_MILLIS, dao.entities[0].cachedAt)
        assertEquals("Morty Smith", dao.entities[1].name)
    }

    @Test
    fun `offline with cache returns cached data without throwing`() = runTest {
        val seededDao = FakeCharacterDao(seed = listOf(cachedRick()))
        val (repository, _) = repositoryRespondingWith(dao = seededDao) {
            jsonResponse(content = "", status = HttpStatusCode.ServiceUnavailable)
        }

        val result = repository.getCharacters().first()

        assertEquals(1, result.size)
        assertEquals("Rick Sanchez (cached)", result[0].name)
        assertEquals(CharacterGender.MALE, result[0].gender)
    }

    @Test
    fun `offline without cache returns empty list without throwing`() = runTest {
        val (repository, dao) = repositoryRespondingWith {
            jsonResponse(content = "", status = HttpStatusCode.ServiceUnavailable)
        }

        val result = repository.getCharacters().first()

        assertTrue(result.isEmpty())
        assertTrue(dao.entities.isEmpty())
    }

    @Test
    fun `offline fallback filters the cache by status and species`() = runTest {
        val alive = cachedRick(cachedAt = 1L)
        val dead = cachedRick(cachedAt = 2L).copy(id = 2, name = "Dead Guy", status = "Dead")
        val seededDao = FakeCharacterDao(seed = listOf(alive, dead))
        val (repository, _) = repositoryRespondingWith(dao = seededDao) {
            jsonResponse(content = "", status = HttpStatusCode.ServiceUnavailable)
        }

        val result = repository.getCharacters(status = CharacterStatus.ALIVE).first()

        assertEquals(1, result.size)
        assertEquals("Rick Sanchez (cached)", result[0].name)
    }

    // ---- getCharacterById ---------------------------------------------------------------------

    @Test
    fun `getCharacterById online success returns fresh data and writes through to cache`() = runTest {
        val (repository, dao) = repositoryRespondingWith { jsonResponse(singleCharacterFixture) }

        val result = repository.getCharacterById(1)

        assertEquals("Rick Sanchez", result?.name)
        assertEquals(1, dao.entities.size)
        assertEquals(FIXED_NOW_MILLIS, dao.entities.single().cachedAt)
    }

    @Test
    fun `getCharacterById offline with cache hit returns cached data without throwing`() = runTest {
        val seededDao = FakeCharacterDao(seed = listOf(cachedRick()))
        val (repository, _) = repositoryRespondingWith(dao = seededDao) {
            jsonResponse(content = "", status = HttpStatusCode.ServiceUnavailable)
        }

        val result = repository.getCharacterById(1)

        assertEquals("Rick Sanchez (cached)", result?.name)
    }

    @Test
    fun `getCharacterById offline without cache miss returns null without throwing`() = runTest {
        val (repository, _) = repositoryRespondingWith {
            jsonResponse(content = "", status = HttpStatusCode.ServiceUnavailable)
        }

        val result = repository.getCharacterById(999)

        assertNull(result)
    }

    private companion object {
        const val FIXED_NOW_MILLIS = 1_700_000_000_000L
    }
}
