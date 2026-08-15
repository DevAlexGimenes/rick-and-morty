package com.gimenes.alex.rickandmorty.core.network.api

import com.gimenes.alex.rickandmorty.core.network.NetworkError
import com.gimenes.alex.rickandmorty.core.network.NetworkResult
import com.gimenes.alex.rickandmorty.core.network.applyRickAndMortyDefaults
import com.gimenes.alex.rickandmorty.core.network.dto.CharacterDto
import com.gimenes.alex.rickandmorty.core.network.dto.LocationReferenceDto
import com.gimenes.alex.rickandmorty.core.network.dto.PagedResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [CharacterApiService] tests against a Ktor [MockEngine]: real-shaped fixture JSON decodes as
 * expected (including pagination metadata), filter query parameters are actually sent, an empty
 * result page decodes to an empty list rather than failing, and a malformed body maps through
 * [com.gimenes.alex.rickandmorty.core.network.safeApiCall] to [NetworkError.Parsing].
 */
class CharacterApiServiceTest {

    // Trimmed to the fields the app relies on, but shaped exactly like the real
    // https://rickandmortyapi.com/api/character response.
    private val listFixture = """
        {
          "info": {
            "count": 826,
            "pages": 42,
            "next": "https://rickandmortyapi.com/api/character?page=3",
            "prev": "https://rickandmortyapi.com/api/character?page=1"
          },
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
              "episode": [
                "https://rickandmortyapi.com/api/episode/1",
                "https://rickandmortyapi.com/api/episode/2"
              ],
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
              "episode": [
                "https://rickandmortyapi.com/api/episode/1"
              ],
              "url": "https://rickandmortyapi.com/api/character/2",
              "created": "2017-11-04T18:50:21.651Z"
            },
            {
              "id": 3,
              "name": "Summer Smith",
              "status": "Alive",
              "species": "Human",
              "type": "",
              "gender": "Female",
              "origin": { "name": "Earth (Replacement Dimension)", "url": "https://rickandmortyapi.com/api/location/20" },
              "location": { "name": "Earth (Replacement Dimension)", "url": "https://rickandmortyapi.com/api/location/20" },
              "image": "https://rickandmortyapi.com/api/character/avatar/3.jpeg",
              "episode": [
                "https://rickandmortyapi.com/api/episode/6"
              ],
              "url": "https://rickandmortyapi.com/api/character/3",
              "created": "2017-11-04T19:09:56.428Z"
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
          "episode": [
            "https://rickandmortyapi.com/api/episode/1"
          ],
          "url": "https://rickandmortyapi.com/api/character/1",
          "created": "2017-11-04T18:48:46.250Z"
        }
    """.trimIndent()

    private val emptyListFixture = """
        {
          "info": { "count": 0, "pages": 0, "next": null, "prev": null },
          "results": []
        }
    """.trimIndent()

    private fun serviceRespondingWith(
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): CharacterApiService {
        val engine = MockEngine(handler)
        val client = HttpClient(engine) { applyRickAndMortyDefaults() }
        return CharacterApiService(client)
    }

    private fun MockRequestHandleScope.jsonResponse(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpResponseData = respond(
        content = content,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    @Test
    fun getCharactersDecodesFixtureIncludingPaginationInfo() = runTest {
        val service = serviceRespondingWith { jsonResponse(listFixture) }

        val result = service.getCharacters()

        assertIs<NetworkResult.Success<PagedResponseDto<CharacterDto>>>(result)
        val page = result.data
        assertEquals(826, page.info.count)
        assertEquals(42, page.info.pages)
        assertEquals("https://rickandmortyapi.com/api/character?page=3", page.info.next)
        assertEquals("https://rickandmortyapi.com/api/character?page=1", page.info.prev)
        assertEquals(3, page.results.size)

        val rick = page.results[0]
        assertEquals(1, rick.id)
        assertEquals("Rick Sanchez", rick.name)
        assertEquals("Alive", rick.status)
        assertEquals("Human", rick.species)
        assertEquals("Male", rick.gender)
        assertEquals(
            LocationReferenceDto(name = "Earth (C-137)", url = "https://rickandmortyapi.com/api/location/1"),
            rick.origin,
        )
        assertEquals(2, rick.episode.size)

        val morty = page.results[1]
        assertEquals(LocationReferenceDto(name = "unknown", url = ""), morty.origin)
    }

    @Test
    fun getCharactersSendsProvidedFiltersAndPageAsQueryParams() = runTest {
        var capturedRequest: HttpRequestData? = null
        val service = serviceRespondingWith {
            capturedRequest = it
            jsonResponse(listFixture)
        }

        service.getCharacters(
            page = 2,
            name = "rick",
            status = "alive",
            species = "human",
            type = "clone",
            gender = "male",
        )

        val request = capturedRequest
        assertTrue(request != null)
        assertEquals("/api/character", request.url.encodedPath)
        val params = request.url.parameters
        assertEquals("2", params["page"])
        assertEquals("rick", params["name"])
        assertEquals("alive", params["status"])
        assertEquals("human", params["species"])
        assertEquals("clone", params["type"])
        assertEquals("male", params["gender"])
    }

    @Test
    fun getCharactersOmitsQueryParamsThatWereNotProvided() = runTest {
        var capturedRequest: HttpRequestData? = null
        val service = serviceRespondingWith {
            capturedRequest = it
            jsonResponse(listFixture)
        }

        service.getCharacters(name = "rick")

        val request = capturedRequest
        assertTrue(request != null)
        val params = request.url.parameters
        assertEquals("rick", params["name"])
        assertNull(params["page"])
        assertNull(params["status"])
        assertNull(params["species"])
        assertNull(params["type"])
        assertNull(params["gender"])
    }

    @Test
    fun getCharacterByIdDecodesSingleCharacter() = runTest {
        var capturedRequest: HttpRequestData? = null
        val service = serviceRespondingWith {
            capturedRequest = it
            jsonResponse(singleCharacterFixture)
        }

        val result = service.getCharacterById(1)

        assertIs<NetworkResult.Success<CharacterDto>>(result)
        assertEquals(1, result.data.id)
        assertEquals("Rick Sanchez", result.data.name)
        assertEquals("/api/character/1", capturedRequest?.url?.encodedPath)
    }

    @Test
    fun getCharactersEmptyResultSetDecodesToEmptyListWithoutThrowing() = runTest {
        val service = serviceRespondingWith { jsonResponse(emptyListFixture) }

        val result = service.getCharacters(name = "no-such-character")

        assertIs<NetworkResult.Success<PagedResponseDto<CharacterDto>>>(result)
        assertEquals(0, result.data.info.count)
        assertTrue(result.data.results.isEmpty())
    }

    @Test
    fun getCharactersMalformedResponseMapsToTypedParsingFailure() = runTest {
        val service = serviceRespondingWith { jsonResponse("this is not valid json") }

        val result = service.getCharacters()

        assertIs<NetworkResult.Failure>(result)
        assertIs<NetworkError.Parsing>(result.error)
    }
}
