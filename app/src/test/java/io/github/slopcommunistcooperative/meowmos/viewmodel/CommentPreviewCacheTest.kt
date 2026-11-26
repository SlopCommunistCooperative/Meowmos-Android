package io.github.slopcommunistcooperative.meowmos.viewmodel

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrThrow
import io.github.slopcommunistcooperative.meowmos.data.model.Memo
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class CommentPreviewCacheTest {
    @Test fun returningToAPreviewUsesTheCacheUntilExplicitRefresh() = runTest {
        val reads = mutableListOf<Pair<String, String>>()
        val cache = CommentPreviewCache { account, parent ->
            reads += account to parent
            preview(reads.size)
        }

        assertEquals("reply-1", cache.load("family", "memos/note").getOrThrow().first.single().content)
        assertEquals("reply-2", cache.load("personal", "memos/note").getOrThrow().first.single().content)
        assertEquals("reply-1", cache.load("family", "memos/note").getOrThrow().first.single().content)
        assertEquals(listOf("family" to "memos/note", "personal" to "memos/note"), reads)

        cache.clear()
        assertEquals("reply-3", cache.load("family", "memos/note").getOrThrow().first.single().content)
        assertEquals("reply-3", cache.load("family", "memos/note").getOrThrow().first.single().content)
        assertEquals(3, reads.size)
    }

    @Test fun aReadStartedBeforeInvalidationCannotReplaceTheNewPreview() = runTest {
        val finishOldRead = CompletableDeferred<Unit>()
        var reads = 0
        val cache = CommentPreviewCache { _, _ ->
            val readNumber = ++reads
            if (readNumber == 1) finishOldRead.await()
            preview(readNumber)
        }

        val old = async(start = CoroutineStart.UNDISPATCHED) { cache.load("family", "memos/note") }
        cache.invalidate("family", "memos/note")
        assertEquals("reply-2", cache.load("family", "memos/note").getOrThrow().first.single().content)
        finishOldRead.complete(Unit)
        old.await()
        assertEquals("reply-2", cache.load("family", "memos/note").getOrThrow().first.single().content)
        assertEquals(2, reads)
    }

    private fun preview(readNumber: Int): ApiResponse<Pair<List<Memo>, String?>> {
        val reply = Memo(
            remoteId = "memos/reply",
            content = "reply-$readNumber",
            date = Instant.EPOCH,
            pinned = false,
            visibility = MemoVisibility.PRIVATE,
            resources = emptyList(),
            tags = emptyList(),
        )
        return ApiResponse.Success(listOf(reply) to null)
    }
}
