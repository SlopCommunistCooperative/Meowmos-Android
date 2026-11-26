package io.github.slopcommunistcooperative.meowmos.data.repository

import com.skydoves.sandwich.ApiResponse
import io.github.slopcommunistcooperative.meowmos.data.model.Memo
import io.github.slopcommunistcooperative.meowmos.data.model.MemoLocation
import io.github.slopcommunistcooperative.meowmos.data.model.MemoLocationChange
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility
import io.github.slopcommunistcooperative.meowmos.data.model.Resource
import io.github.slopcommunistcooperative.meowmos.data.model.User
import okhttp3.MediaType
import java.io.InputStream
import java.time.Instant

abstract class RemoteRepository {
    internal abstract val accountKey: String

    internal abstract suspend fun listComments(
        parent: String,
        pageSize: Int,
        pageToken: String? = null,
    ): ApiResponse<Pair<List<Memo>, String?>>

    internal abstract suspend fun createComment(
        parent: String,
        content: String,
        visibility: MemoVisibility,
        space: String?,
    ): ApiResponse<Memo>

    abstract suspend fun listMemos(): ApiResponse<List<Memo>>
    abstract suspend fun listArchivedMemos(): ApiResponse<List<Memo>>
    abstract suspend fun listWorkspaceMemos(pageSize: Int, pageToken: String?): ApiResponse<Pair<List<Memo>, String?>>

    abstract suspend fun createMemo(
        content: String,
        visibility: MemoVisibility,
        resourceRemoteIds: List<String>,
        tags: List<String>? = null,
        createdAt: Instant? = null,
        location: MemoLocation? = null,
        space: String? = null,
    ): ApiResponse<Memo>

    abstract suspend fun updateMemo(
        remoteId: String,
        content: String? = null,
        resourceRemoteIds: List<String>? = null,
        visibility: MemoVisibility? = null,
        tags: List<String>? = null,
        pinned: Boolean? = null,
        archived: Boolean? = null,
        location: MemoLocationChange? = null,
        space: String? = null,
    ): ApiResponse<Memo>

    abstract suspend fun deleteMemo(remoteId: String): ApiResponse<Unit>

    abstract suspend fun listResources(): ApiResponse<List<Resource>>

    abstract suspend fun createResource(
        filename: String,
        type: MediaType?,
        contentLength: Long?,
        openInputStream: () -> InputStream,
        memoRemoteId: String? = null
    ): ApiResponse<Resource>

    abstract suspend fun deleteResource(remoteId: String): ApiResponse<Unit>
    abstract suspend fun getCurrentUser(): ApiResponse<User>
}
