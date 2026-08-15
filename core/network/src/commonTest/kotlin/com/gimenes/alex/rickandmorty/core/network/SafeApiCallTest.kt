package com.gimenes.alex.rickandmorty.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Endpoint-agnostic stand-in payload used only to exercise [safeApiCall] against a [MockEngine].
 * Deliberately not modeled on any real Rick and Morty API response - this module is transport
 * layer only.
 */
@Serializable
private data class TestPayload(val id: Int, val name: String)

class SafeApiCallTest {

    private fun clientRespondingWith(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        contentType: String = "application/json",
    ): HttpClient {
        val engine = MockEngine {
            respond(
                content = content,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, contentType),
            )
        }
        return HttpClient(engine) { applyRickAndMortyDefaults() }
    }

    @Test
    fun successfulJsonResponseDecodesIntoTypedSuccess() = runTest {
        val client = clientRespondingWith(content = """{"id":1,"name":"Rick Sanchez"}""")

        val result = safeApiCall {
            client.get("https://example.test/item/1").body<TestPayload>()
        }

        assertIs<NetworkResult.Success<TestPayload>>(result)
        assertEquals(TestPayload(id = 1, name = "Rick Sanchez"), result.data)
    }

    @Test
    fun httpErrorStatusMapsToTypedHttpError() = runTest {
        val client = clientRespondingWith(
            content = """{"error":"not found"}""",
            status = HttpStatusCode.NotFound,
        )

        val result = safeApiCall {
            client.get("https://example.test/item/999").body<TestPayload>()
        }

        assertIs<NetworkResult.Failure>(result)
        val error = result.error
        assertIs<NetworkError.Http>(error)
        assertEquals(404, error.statusCode)
    }

    @Test
    fun malformedResponseMapsToTypedParsingError() = runTest {
        val client = clientRespondingWith(content = "this is not valid json")

        val result = safeApiCall {
            client.get("https://example.test/item/1").body<TestPayload>()
        }

        assertIs<NetworkResult.Failure>(result)
        assertIs<NetworkError.Parsing>(result.error)
    }
}
