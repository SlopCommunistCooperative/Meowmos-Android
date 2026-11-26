package io.github.slopcommunistcooperative.meowmos.viewmodel

import androidx.collection.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.skydoves.sandwich.ApiResponse
import io.github.slopcommunistcooperative.meowmos.data.model.Memo

internal const val COMMENT_PREVIEW_SIZE = 3

internal class CommentPreviewCache(
    private val read: suspend (accountKey: String, parent: String) -> ApiResponse<Pair<List<Memo>, String?>>,
) {
    private val pages = LruCache<Pair<String, String>, Pair<List<Memo>, String?>>(64)
    private var writeEpoch = 0
    var generation by mutableIntStateOf(0)
        private set

    fun peek(accountKey: String, parent: String): List<Memo> = pages[accountKey to parent]?.first.orEmpty()

    suspend fun load(accountKey: String, parent: String): ApiResponse<Pair<List<Memo>, String?>> {
        val key = accountKey to parent
        pages[key]?.let { return ApiResponse.Success(it) }
        val epoch = writeEpoch
        val response = read(accountKey, parent)
        if (response is ApiResponse.Success && epoch == writeEpoch) {
            pages.put(key, response.data)
        }
        return response
    }

    fun invalidate(accountKey: String, parent: String) {
        writeEpoch++
        pages.remove(accountKey to parent)
    }

    fun clear() {
        writeEpoch++
        pages.evictAll()
        generation++
    }
}
