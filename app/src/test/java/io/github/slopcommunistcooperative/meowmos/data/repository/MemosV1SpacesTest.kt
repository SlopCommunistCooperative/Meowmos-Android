package io.github.slopcommunistcooperative.meowmos.data.repository

import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import io.github.slopcommunistcooperative.meowmos.data.api.MemosApiPathInterceptor
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV1Api
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.data.model.MemoSpace
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility
import io.github.slopcommunistcooperative.meowmos.data.model.MemosAccount
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class MemosV1SpacesTest {
    private val family = MemoSpace("spaces/family", "Family")
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test fun listsEveryPageOfSpacesOnCalendarVersionServers() = runTest {
        val repository = repository("modern-spaces") { request ->
            when (request.url.encodedPath) {
                "/notes/api/instance/profile" -> 200 to """{"version":"26.11.2"}"""
                "/notes/api/spaces" -> when (request.url.queryParameter("pageToken")) {
                    null -> 200 to """{"spaces":[{"name":"spaces/family","title":"Family"}],"nextPageToken":"second"}"""
                    "second" -> 200 to """{"spaces":[{"name":"spaces/friends","title":"Friends"}]}"""
                    else -> 400 to "{}"
                }
                else -> 404 to "{}"
            }
        }

        assertEquals(listOf(family, MemoSpace("spaces/friends", "Friends")), repository.listSpaces().getOrThrow())
    }

    @Test fun olderServerWithoutSpacesStillAllowsPersonalMemos() = runTest {
        val requests = mutableListOf<Request>()
        val repository = repository("legacy-spaces") { request ->
            requests.add(request)
            when (request.url.encodedPath) {
                "/notes/api/v1/memos" -> 200 to """{"name":"memos/note","visibility":"PRIVATE"}"""
                else -> 404 to "{}"
            }
        }

        assertTrue(repository.listSpaces().getOrThrow().isEmpty())
        val memo = repository.createMemo("personal note", MemoVisibility.PRIVATE, emptyList()).getOrThrow()
        assertNull(memo.space)
        assertFalse("space" in body(requests.last()))
    }

    @Test fun postingAndUnassigningSendPlacementAndAudienceTogether() = runTest {
        val requests = mutableListOf<Request>()
        val repository = repository("posting-spaces") { request ->
            requests.add(request)
            when (request.method) {
                "POST" -> 200 to """{"name":"memos/note","space":"spaces/family","visibility":"SPACE"}"""
                "PATCH" -> 200 to """{"name":"memos/note","visibility":"PRIVATE"}"""
                else -> 404 to "{}"
            }
        }

        val created = repository.createMemo("family note", MemoVisibility.SPACE, emptyList(), space = family.name).getOrThrow()
        assertEquals(family.name, created.space)
        val create = body(requests.last())
        assertEquals(family.name, create.getValue("space").jsonPrimitive.content)
        assertEquals("SPACE", create.getValue("visibility").jsonPrimitive.content)

        val removed = repository.updateMemo(created.remoteId, visibility = MemoVisibility.PRIVATE, space = "").getOrThrow()
        assertNull(removed.space)
        val update = requests.last()
        assertEquals(setOf("visibility", "update_time", "space"), update.url.queryParameter("updateMask")?.split(',')?.toSet())
        assertFalse("space" in body(update))
        assertEquals("PRIVATE", body(update).getValue("visibility").jsonPrimitive.content)
    }

    private fun body(request: Request) = Buffer().use { buffer ->
        requireNotNull(request.body).writeTo(buffer)
        Json.parseToJsonElement(buffer.readUtf8()).jsonObject
    }

    private fun repository(host: String, respond: (Request) -> Pair<Int, String>): MemosV1Repository {
        val url = "https://$host.test/notes/"
        val mediaType = "application/json".toMediaType()
        val client = OkHttpClient.Builder()
            .addInterceptor(MemosApiPathInterceptor(url))
            .addInterceptor { chain ->
                val request = chain.request()
                val (code, body) = respond(request)
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                    .code(code).message("test").header("Content-Type", mediaType.toString())
                    .body(body.toResponseBody(mediaType)).build()
            }.build()
        val api = Retrofit.Builder().baseUrl(url).client(client)
            .addConverterFactory(json.asConverterFactory(mediaType))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .build().create(MemosV1Api::class.java)
        return MemosV1Repository(api, Account.MemosV1(MemosAccount(host = url, remoteIdentifier = "users/a")))
    }
}
