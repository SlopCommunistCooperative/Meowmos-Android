package io.github.slopcommunistcooperative.meowmos.data.repository

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV0Api
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility
import io.github.slopcommunistcooperative.meowmos.data.model.MemosAccount
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.ConcurrentLinkedQueue

class MemosV0CommentsTest {
    @Test fun v021ListsOnlyActiveCommentsTheAccountCanRead() = runTest {
        val wire = Wire { request ->
            when (request.url.encodedPath) {
                "/notes/api/v2/memos/12/comments" -> 200 to """{"memos":[
                    {"name":"memos/31","creator":"users/2","rowStatus":"ACTIVE","visibility":"PRIVATE","content":"not ours"},
                    {"name":"memos/32","creator":"users/1","rowStatus":"ACTIVE","visibility":"PRIVATE","content":"my comment"},
                    {"name":"memos/33","creator":"users/3","rowStatus":"ACTIVE","visibility":"PROTECTED","content":"shared comment"},
                    {"name":"memos/34","creator":"users/2","rowStatus":"ARCHIVED","visibility":"PUBLIC","content":"archived"}
                ]}"""
                "/notes/api/v2/users/1" -> 200 to """{"user":{"username":"writer","nickname":"Writer"}}"""
                "/notes/api/v2/users/3" -> 200 to """{"user":{"username":"member"}}"""
                else -> 404 to "{}"
            }
        }
        val (comments, next) = wire.repository.listComments("12", 3).getOrThrow()
        assertEquals(listOf("my comment", "shared comment"), comments.map { it.content })
        assertEquals(listOf("Writer", "member"), comments.map { it.creator?.name })
        assertNull(next)
        assertTrue(wire.requests.all { it.method == "GET" && it.url.query == null })
    }

    @Test fun v021PostsTextAndAudienceAsNestedFormFields() = runTest {
        val wire = Wire { request ->
            when (request.method to request.url.encodedPath) {
                "GET" to "/notes/api/v2/memos/12" -> 200 to """{"memo":{"name":"memos/12","rowStatus":"ACTIVE"}}"""
                "POST" to "/notes/api/v2/memos/12/comments" ->
                    200 to """{"memo":{"name":"memos/35","creator":"users/1","visibility":"PROTECTED"}}"""
                else -> 404 to "{}"
            }
        }
        val text = "你好 + &=\n**Reply**"
        val created = wire.repository.createComment("12", text, MemoVisibility.PROTECTED, null).getOrThrow()
        assertEquals("35", created.remoteId)
        assertEquals(MemoVisibility.PROTECTED, created.visibility)
        val request = wire.requests.single { it.method == "POST" }
        val form = request.body as FormBody
        assertEquals(mapOf("comment.content" to text, "comment.visibility" to "PROTECTED"),
            (0 until form.size).associate { form.name(it) to form.value(it) })
        assertNull(request.url.query)
    }

    @Test fun v021DoesNotPostToAnUnavailableParent() = runTest {
        val wire = Wire { 404 to "{}" }
        assertTrue(wire.repository.createComment("12", "reply", MemoVisibility.PRIVATE, null) is ApiResponse.Failure<*>)
        assertEquals(listOf("GET"), wire.requests.map { it.method })
    }

    @Test fun v021KeepsRepliesOutOfNoteFeedsWithoutEndingPaginationEarly() = runTest {
        val posts = """[
            {"id":12,"createdTs":1,"updatedTs":1,"creatorId":1,"content":"parent","pinned":false,
              "relationList":[{"memoId":35,"relatedMemoId":12,"type":"COMMENT"}]},
            {"id":35,"createdTs":2,"updatedTs":2,"creatorId":1,"content":"reply","pinned":false,
              "relationList":[{"memoId":35,"relatedMemoId":12,"type":"COMMENT"}]}
        ]"""
        val wire = Wire { request ->
            when (request.url.encodedPath) {
                "/notes/api/v1/memo", "/notes/api/v1/memo/all" -> 200 to posts
                else -> 404 to "{}"
            }
        }
        assertEquals(listOf("parent"), wire.repository.listMemos().getOrThrow().map { it.content })
        val (memos, next) = wire.repository.listWorkspaceMemos(2, null).getOrThrow()
        assertEquals("parent", memos.single().content)
        assertEquals(wire.repository.accountKey, memos.single().accountKey)
        assertEquals("2", next)
    }

    private class Wire(respond: (Request) -> Pair<Int, String>) {
        val requests = ConcurrentLinkedQueue<Request>()
        private val url = "https://legacy.test/notes/"
        private val mediaType = "application/json".toMediaType()
        private val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests.add(request)
            val (code, body) = respond(request)
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(code).message("test").body(body.toResponseBody(mediaType)).build()
        }.build()
        private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        private val api = Retrofit.Builder().baseUrl(url).client(client)
            .addConverterFactory(json.asConverterFactory(mediaType))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .build().create(MemosV0Api::class.java)
        val repository = MemosV0Repository(api, Account.MemosV0(MemosAccount(host = url, remoteIdentifier = "1")))
    }
}
