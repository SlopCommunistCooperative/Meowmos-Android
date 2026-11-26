package io.github.slopcommunistcooperative.meowmos.data.repository

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.mapSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import io.github.slopcommunistcooperative.meowmos.data.api.MemosRowStatus
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV0Api
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV0CommentMemo
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV0CreateMemoInput
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV0Memo
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV0PatchMemoInput
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV0Resource
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV0UpdateMemoOrganizerInput
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV0UpdateTagInput
import io.github.slopcommunistcooperative.meowmos.data.api.MemosVisibility
import io.github.slopcommunistcooperative.meowmos.data.constant.MeowmosException
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.data.model.Memo
import io.github.slopcommunistcooperative.meowmos.data.model.MemoLocation
import io.github.slopcommunistcooperative.meowmos.data.model.MemoLocationChange
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility
import io.github.slopcommunistcooperative.meowmos.data.model.Resource
import io.github.slopcommunistcooperative.meowmos.data.model.User
import okhttp3.MediaType
import okhttp3.MultipartBody
import java.time.Instant

class MemosV0Repository (
    private val memosApi: MemosV0Api,
    private val account: Account.MemosV0,
) : RemoteRepository() {
    internal override val accountKey = account.accountKey()
    private val remoteUserName = "users/${account.info.remoteIdentifier}"

    private fun convertResource(resource: MemosV0Resource): Resource {
        return Resource(
            remoteId = resource.id.toString(),
            date = Instant.ofEpochSecond(resource.createdTs),
            filename = resource.filename,
            uri = resource.uri(account.info.host).toString(),
            mimeType = resource.type
        )
    }

    private fun convertMemo(memo: MemosV0Memo): Memo {
        return Memo(
            remoteId = memo.id.toString(),
            content = memo.content,
            date = Instant.ofEpochSecond(memo.createdTs),
            pinned = memo.pinned,
            visibility = memo.visibility.toMemoVisibility(),
            resources = memo.resourceList?.map { convertResource(it) } ?: emptyList(),
            tags = emptyList(),
            creator = memo.creatorName?.let { User(memo.creatorId.toString(), it) },
            archived = memo.rowStatus == MemosRowStatus.ARCHIVED,
            updatedAt = Instant.ofEpochSecond(memo.updatedTs)
        )
    }

    private fun MemosV0Memo.isComment() = relationList.orEmpty().any { it.type == "COMMENT" && it.memoId == id }

    private fun convertComment(comment: MemosV0CommentMemo, creator: User? = null) = Memo(
        remoteId = comment.name.substringAfterLast('/'),
        content = comment.content,
        date = comment.createTime ?: Instant.EPOCH,
        pinned = false,
        visibility = comment.visibility.toMemoVisibility(),
        resources = comment.resources.map {
            Resource(
                remoteId = it.name,
                date = it.createTime ?: Instant.EPOCH,
                filename = it.filename,
                mimeType = it.type,
                uri = it.uri(account.info.host).toString(),
            )
        },
        tags = emptyList(),
        creator = creator,
    )

    internal override suspend fun listComments(
        parent: String,
        pageSize: Int,
        pageToken: String?,
    ): ApiResponse<Pair<List<Memo>, String?>> {
        val id = parent.toLongOrNull() ?: return ApiResponse.exception(MeowmosException.invalidParameter)
        val response = memosApi.listComments(id)
        if (response !is ApiResponse.Success) return response.mapSuccess { emptyList<Memo>() to null }

        val readable = response.data.memos.filter {
            it.rowStatus != MemosRowStatus.ARCHIVED && when (it.visibility.toMemoVisibility()) {
                MemoVisibility.PRIVATE -> it.creator == remoteUserName
                MemoVisibility.PROTECTED, MemoVisibility.PUBLIC -> true
                MemoVisibility.SPACE -> false
            }
        }
        val authors = coroutineScope {
            readable.map { it.creator }.toSet().mapNotNull { creator ->
                val authorId = creator.removePrefix("users/").toLongOrNull() ?: return@mapNotNull null
                async {
                    val user = memosApi.getCommentUser(authorId).getOrNull()?.user
                    creator to user?.let { User(creator, it.nickname?.takeIf(String::isNotBlank) ?: it.username) }
                }
            }.awaitAll().toMap()
        }
        return ApiResponse.Success(readable.map { convertComment(it, authors[it.creator]) } to null)
    }

    internal override suspend fun createComment(
        parent: String,
        content: String,
        visibility: MemoVisibility,
        space: String?,
    ): ApiResponse<Memo> {
        val id = parent.toLongOrNull() ?: return ApiResponse.exception(MeowmosException.invalidParameter)
        if (space != null || visibility == MemoVisibility.SPACE) {
            return ApiResponse.exception(MeowmosException.invalidParameter)
        }
        val readableParent = memosApi.getCommentParent(id)
        if (readableParent !is ApiResponse.Success) {
            return readableParent.mapSuccess { convertComment(memo) }
        }
        if (readableParent.data.memo.rowStatus == MemosRowStatus.ARCHIVED) {
            return ApiResponse.exception(MeowmosException.invalidParameter)
        }
        return memosApi.createComment(id, content, MemosVisibility.fromMemoVisibility(visibility).name)
            .mapSuccess { convertComment(memo) }
    }

    override suspend fun listMemos(): ApiResponse<List<Memo>> {
        return memosApi.listMemo(rowStatus = MemosRowStatus.NORMAL).mapSuccess {
            filterNot { it.isComment() }.map { convertMemo(it) }
        }
    }

    override suspend fun listArchivedMemos(): ApiResponse<List<Memo>> {
        return memosApi.listMemo(rowStatus = MemosRowStatus.ARCHIVED).mapSuccess {
            filterNot { it.isComment() }.map { convertMemo(it) }
        }
    }

    override suspend fun createMemo(
        content: String,
        visibility: MemoVisibility,
        resourceRemoteIds: List<String>,
        tags: List<String>?,
        createdAt: Instant?,
        location: MemoLocation?,
        space: String?,
    ): ApiResponse<Memo> {
        val result = memosApi.createMemo(
            MemosV0CreateMemoInput(
                content = content,
                resourceIdList = resourceRemoteIds.map { it.toLong() },
                visibility = MemosVisibility.fromMemoVisibility(visibility),
                createdTs = createdAt?.epochSecond
            )
        ).mapSuccess {
            convertMemo(this)
        }
        tags?.forEach { tag ->
            memosApi.updateTag(MemosV0UpdateTagInput(tag))
        }
        return result
    }

    override suspend fun updateMemo(
        remoteId: String,
        content: String?,
        resourceRemoteIds: List<String>?,
        visibility: MemoVisibility?,
        tags: List<String>?,
        pinned: Boolean?,
        archived: Boolean?,
        location: MemoLocationChange?,
        space: String?,
    ): ApiResponse<Memo> {
        var touched = false
        var result: ApiResponse<Memo> = ApiResponse.exception(MeowmosException.invalidParameter)
        pinned?.let {
            touched = true
            result = memosApi.updateMemoOrganizer(remoteId.toLong(), MemosV0UpdateMemoOrganizerInput(pinned = it)).mapSuccess {
                convertMemo(this)
            }
            if (result !is ApiResponse.Success<Memo>) {
                return result
            }
        }
        if (content != null || visibility != null || resourceRemoteIds != null || archived != null) {
            touched = true
            val memosVisibility = visibility?.let { v -> MemosVisibility.fromMemoVisibility(v) }
            val resourceIdList = resourceRemoteIds?.map { it.toLong() }
            val rowStatus = archived?.let { isArchived ->
                if (isArchived) MemosRowStatus.ARCHIVED else MemosRowStatus.NORMAL
            }
            result = memosApi.patchMemo(
                remoteId.toLong(),
                MemosV0PatchMemoInput(
                    id = remoteId.toLong(),
                    content = content,
                    visibility = memosVisibility,
                    resourceIdList = resourceIdList,
                    rowStatus = rowStatus
                )
            ).mapSuccess {
                convertMemo(this)
            }
        }
        tags?.forEach { tag ->
            memosApi.updateTag(MemosV0UpdateTagInput(tag))
        }
        return if (touched) result else ApiResponse.exception(MeowmosException.invalidParameter)
    }

    override suspend fun deleteMemo(remoteId: String): ApiResponse<Unit> {
        return memosApi.deleteMemo(remoteId.toLong())
    }

    override suspend fun listResources(): ApiResponse<List<Resource>> {
        return memosApi.getResources().mapSuccess {
            this.map { convertResource(it) }
        }
    }

    override suspend fun createResource(
        filename: String,
        type: MediaType?,
        contentLength: Long?,
        openInputStream: () -> java.io.InputStream,
        memoRemoteId: String?
    ): ApiResponse<Resource> {
        val file = MultipartBody.Part.createFormData(
            "file",
            filename,
            InputStreamRequestBody(type, contentLength, openInputStream)
        )
        return memosApi.uploadResource(file).mapSuccess {
            convertResource(this)
        }
    }

    override suspend fun deleteResource(remoteId: String): ApiResponse<Unit> {
        return memosApi.deleteResource(remoteId.toLong())
    }

    override suspend fun listWorkspaceMemos(
        pageSize: Int,
        pageToken: String?
    ): ApiResponse<Pair<List<Memo>, String?>> {
        return memosApi.listAllMemo(limit = pageSize, offset = pageToken?.toIntOrNull()).mapSuccess {
            val memos = filterNot { it.isComment() }.map { convertMemo(it).copy(accountKey = accountKey) }
            val currentOffset = pageToken?.toIntOrNull() ?: 0
            val nextPageToken = if (size < pageSize) {
                null
            } else {
                (currentOffset + pageSize).toString()
            }
            memos to nextPageToken
        }
    }

    override suspend fun getCurrentUser(): ApiResponse<User> {
        return memosApi.me().mapSuccess {
            toUser()
        }
    }
}
