package io.github.slopcommunistcooperative.meowmos.data.repository

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.StatusCode
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import com.skydoves.sandwich.retrofit.statusCode
import io.github.slopcommunistcooperative.meowmos.data.api.MemosApiPathInterceptor
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV1Api
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility
import io.github.slopcommunistcooperative.meowmos.data.model.MemosAccount
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.ConcurrentLinkedQueue

class MemosV1CommentsTest {
    @Test fun calendarParentFieldPaginatesAndResolvesCreators() = runTest {
        val wire = Wire("parent-field-comments", "26.11.2") { request ->
            when (request.url.encodedPath) {
                "/notes/api/memos" -> when (request.url.queryParameter("pageToken")) {
                    null -> 200 to """{
                        "memos":[
                            {"name":"memos/first","creator":"users/writer","content":"first **reply**","parent":"memos/note"},
                            {"name":"memos/second","creator":"users/writer","parent":"memos/note"}
                        ],
                        "nextPageToken":"page+2/="
                    }"""
                    "page+2/=" -> 200 to "{}"
                    else -> 400 to "{}"
                }
                "/notes/api/users/writer" -> 200 to """{"name":"users/writer","username":"writer","displayName":"Writer"}"""
                else -> 404 to "{}"
            }
        }

        val (comments, next) = wire.repository.listComments("memos/note", 2).getOrThrow()
        assertEquals(listOf("memos/first", "memos/second"), comments.map { it.remoteId })
        assertEquals(listOf("Writer", "Writer"), comments.map { it.creator?.name })
        assertEquals("first **reply**", comments.first().content)
        assertEquals("page+2/=", next)
        val (last, end) = wire.repository.listComments("memos/note", 2, next).getOrThrow()
        assertTrue(last.isEmpty())
        assertNull(end)

        val lists = wire.requests.filter { it.url.encodedPath == "/notes/api/memos" }
        assertEquals(listOf(null, "page+2/="), lists.map { it.url.queryParameter("pageToken") })
        assertEquals(listOf("2", "2"), lists.map { it.url.queryParameter("pageSize") })
        assertEquals(List(2) { "parent == \"memos/note\"" }, lists.map { it.url.queryParameter("filter") })
        assertEquals(1, wire.requests.count { it.url.encodedPath == "/notes/api/users/writer" })
        // The interceptor probes the unversioned path once, then the repository reads and caches the profile.
        assertEquals(2, wire.requests.count { it.url.encodedPath == "/notes/api/instance/profile" })
    }

    @Test fun nestedEndpointsUseTheServerPrefixAndCommentListKey() = runTest {
        for (version in listOf("0.27.0", "0.31.0", "26.10.0", "26.11.1")) {
            val calendar = version.startsWith("26.")
            val prefix = if (calendar) "/notes/api" else "/notes/api/v1"
            val route = "$prefix/memos/note/comments"
            val listKey = if (calendar) "comments" else "memos"
            val wire = Wire("nested-comments-$version", version) { request ->
                when {
                    request.url.encodedPath != route -> 404 to "{}"
                    request.method == "POST" -> 200 to """{"name":"memos/created","visibility":"PROTECTED"}"""
                    request.url.queryParameter("pageToken") == null -> 200 to
                        """{"$listKey":[{"name":"memos/first","parent":"memos/note"}],"nextPageToken":"more"}"""
                    else -> 200 to """{"nextPageToken":""}"""
                }
            }

            val (comments, next) = wire.repository.listComments("memos/note", 1).getOrThrow()
            assertEquals("memos/first", comments.single().remoteId)
            assertEquals("more", next)
            val (last, end) = wire.repository.listComments("memos/note", 1, next).getOrThrow()
            assertTrue(last.isEmpty())
            assertNull(end)
            assertEquals("memos/created", wire.repository.createComment(
                "memos/note", "nested reply", MemoVisibility.PROTECTED, null,
            ).getOrThrow().remoteId)

            val apiCalls = wire.requests.filterNot { it.url.encodedPath.endsWith("/instance/profile") }
            assertEquals(List(3) { route }, apiCalls.map { it.url.encodedPath })
            assertEquals(listOf("GET", "GET", "POST"), apiCalls.map { it.method })
            assertEquals(listOf(null, "more"), apiCalls.take(2).map { it.url.queryParameter("pageToken") })
            assertEquals(listOf("1", "1"), apiCalls.take(2).map { it.url.queryParameter("pageSize") })
            assertEquals(Json.parseToJsonElement("""{"content":"nested reply","visibility":"PROTECTED"}"""), body(apiCalls.last()))
        }
    }

    @Test fun calendarParentFieldPostsParentSpaceAndVisibilityTogether() = runTest {
        val wire = Wire("parent-field-publish", "26.11.2") { request ->
            if (request.method == "POST" && request.url.encodedPath == "/notes/api/memos") {
                200 to """{"name":"memos/created","parent":"memos/note","space":"spaces/team","visibility":"SPACE"}"""
            } else {
                404 to "{}"
            }
        }

        val memo = wire.repository.createComment("memos/note", "space reply", MemoVisibility.SPACE, "spaces/team").getOrThrow()
        assertEquals("memos/created", memo.remoteId)
        assertEquals("spaces/team", memo.space)
        assertEquals(MemoVisibility.SPACE, memo.visibility)
        assertEquals(
            Json.parseToJsonElement("""{"parent":"memos/note","content":"space reply","visibility":"SPACE","space":"spaces/team"}"""),
            body(wire.requests.single { it.method == "POST" }),
        )
    }

    @Test fun rejectedCommentPostNeverTriesTheOtherRoute() = runTest {
        for ((version, route) in listOf(
            "26.11.1" to "/notes/api/memos/note/comments",
            "26.11.2" to "/notes/api/memos",
        )) {
            val wire = Wire("failed-comment-$version", version) { 404 to "{}" }

            val response = wire.repository.createComment("memos/note", "reply", MemoVisibility.PRIVATE, null)
            assertEquals(StatusCode.NotFound, (response as ApiResponse.Failure.Error).statusCode)
            assertEquals(listOf(route), wire.requests.filter { it.method == "POST" }.map { it.url.encodedPath })
        }
    }

    @Test fun missingOrUnknownProfileNeverSendsACommentRequest() = runTest {
        for (version in listOf(null, "dev", "0.26.0")) {
            val wire = Wire("unknown-comment-$version", version) { 200 to """{"name":"memos/unsafe"}""" }

            assertTrue(wire.repository.createComment("memos/note", "reply", MemoVisibility.PRIVATE, null) is ApiResponse.Failure<*>)
            assertTrue(wire.repository.listComments("memos/note", 1) is ApiResponse.Failure<*>)
            assertTrue(wire.requests.isNotEmpty())
            assertTrue(wire.requests.all { it.method == "GET" && it.url.encodedPath.endsWith("/instance/profile") })
        }
    }

    @Test fun workspacePageStampsAccountAndExcludesComments() = runTest {
        val wire = Wire("workspace-comment-account", "26.11.2") { request ->
            if (request.url.encodedPath == "/notes/api/memos") {
                200 to """{"memos":[{"name":"memos/note"},{"name":"memos/reply","parent":"memos/note"}]}"""
            } else {
                404 to "{}"
            }
        }

        val memo = wire.repository.listWorkspaceMemos(2, null).getOrThrow().first.single()
        assertEquals("memos/note", memo.remoteId)
        assertEquals(wire.repository.accountKey, memo.accountKey)
    }

    private fun body(request: Request) = Buffer().use { buffer ->
        requireNotNull(request.body).writeTo(buffer)
        Json.parseToJsonElement(buffer.readUtf8())
    }

    private class Wire(host: String, version: String?, respond: (Request) -> Pair<Int, String>) {
        val requests = ConcurrentLinkedQueue<Request>()
        val repository: MemosV1Repository

        init {
            val url = "https://$host.test/notes/"
            val calendar = version?.startsWith("26.") == true
            val mediaType = "application/json".toMediaType()
            val client = OkHttpClient.Builder()
                .addInterceptor(MemosApiPathInterceptor(url))
                .addInterceptor { chain ->
                    val request = chain.request()
                    requests.add(request)
                    val (code, body) = when (request.url.encodedPath) {
                        "/notes/api/instance/profile" -> if (calendar) 200 to """{"version":"$version"}""" else 404 to "{}"
                        "/notes/api/v1/instance/profile" -> if (version != null && !calendar) 200 to """{"version":"$version"}""" else 404 to "{}"
                        else -> respond(request)
                    }
                    Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                        .code(code).message("test").header("Content-Type", mediaType.toString())
                        .body(body.toResponseBody(mediaType)).build()
                }.build()
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
            val api = Retrofit.Builder().baseUrl(url).client(client)
                .addConverterFactory(json.asConverterFactory(mediaType))
                .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
                .build().create(MemosV1Api::class.java)
            repository = MemosV1Repository(api, Account.MemosV1(MemosAccount(host = url, remoteIdentifier = "users/a")))
        }
    }
}
