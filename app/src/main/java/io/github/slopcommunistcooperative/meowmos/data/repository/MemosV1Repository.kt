package io.github.slopcommunistcooperative.meowmos.data.repository

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.StatusCode
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.mapSuccess
import com.skydoves.sandwich.onSuccess
import com.skydoves.sandwich.retrofit.statusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.github.slopcommunistcooperative.meowmos.data.api.ListMemosResponse
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV1Api
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV1CreateMemoRequest
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV1Memo
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV1Resource
import io.github.slopcommunistcooperative.meowmos.data.api.MemosV1State
import io.github.slopcommunistcooperative.meowmos.data.api.MemosVisibility
import io.github.slopcommunistcooperative.meowmos.data.api.UpdateMemoRequest
import io.github.slopcommunistcooperative.meowmos.data.constant.MeowmosException
import io.github.slopcommunistcooperative.meowmos.data.constant.MemosVersionSupport.MEMOS_CALVER_MIN_VERSION
import io.github.slopcommunistcooperative.meowmos.data.constant.MemosVersionSupport.MEMOS_COMMENT_PARENT_MIN_VERSION
import io.github.slopcommunistcooperative.meowmos.data.constant.MemosVersionSupport.MEMOS_V1_MAX_VERSION
import io.github.slopcommunistcooperative.meowmos.data.constant.MemosVersionSupport.MEMOS_V1_MIN_VERSION
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.data.model.Memo
import io.github.slopcommunistcooperative.meowmos.data.model.MemoLocation
import io.github.slopcommunistcooperative.meowmos.data.model.MemoLocationChange
import io.github.slopcommunistcooperative.meowmos.data.model.MemoSpace
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility
import io.github.slopcommunistcooperative.meowmos.data.model.Resource
import io.github.slopcommunistcooperative.meowmos.data.model.User
import net.swiftzer.semver.SemVer
import okhttp3.MediaType
import java.time.Instant

private const val PAGE_SIZE = 200
private val COMMENT_PARENT_NAME = Regex("memos/[A-Za-z0-9_-]+")

class MemosV1Repository(
    private val memosApi: MemosV1Api,
    private val account: Account.MemosV1
): RemoteRepository() {
    private val remoteUserIdentifier = account.info.remoteIdentifier
    internal override val accountKey = account.accountKey()

    private enum class CommentRoute { NESTED, PARENT_FIELD }

    private val commentRouteMutex = Mutex()
    private var cachedCommentRoute: ApiResponse.Success<CommentRoute>? = null

    private suspend fun resolveCommentRoute(): ApiResponse<CommentRoute> = commentRouteMutex.withLock {
        cachedCommentRoute?.let { return@withLock it }
        when (val profile = memosApi.getProfile()) {
            is ApiResponse.Success -> {
                val version = SemVer.parseOrNull(profile.data.version.trim().removePrefix("v"))
                val route = when {
                    version == null -> null
                    version >= MEMOS_COMMENT_PARENT_MIN_VERSION -> CommentRoute.PARENT_FIELD
                    version >= MEMOS_CALVER_MIN_VERSION -> CommentRoute.NESTED
                    version >= MEMOS_V1_MIN_VERSION && version.major == MEMOS_V1_MAX_VERSION.major &&
                        version.minor <= MEMOS_V1_MAX_VERSION.minor -> CommentRoute.NESTED
                    else -> null
                }
                if (route == null) {
                    ApiResponse.Failure.Exception(IllegalStateException("Unsupported Memos comments API version"))
                } else {
                    ApiResponse.Success(route).also { cachedCommentRoute = it }
                }
            }
            is ApiResponse.Failure.Error -> profile
            is ApiResponse.Failure.Exception -> profile
        }
    }

    private suspend inline fun <T> withCommentRoute(block: (CommentRoute) -> ApiResponse<T>): ApiResponse<T> {
        return when (val response = resolveCommentRoute()) {
            is ApiResponse.Success -> block(response.data)
            is ApiResponse.Failure.Error -> response
            is ApiResponse.Failure.Exception -> response
        }
    }

    private fun convertResource(resource: MemosV1Resource): Resource {
        return Resource(
            remoteId = requireNotNull(resource.name),
            date = resource.createTime ?: Instant.now(),
            filename = resource.filename ?: "",
            uri = resource.uri(account.info.host).toString(),
            mimeType = resource.type
        )
    }

    private fun convertMemo(memo: MemosV1Memo, creator: User? = null, accountKey: String? = null): Memo {
        return Memo(
            remoteId = memo.name,
            content = memo.content ?: "",
            date = memo.createTime ?: Instant.now(),
            pinned = memo.pinned ?: false,
            visibility = memo.visibility?.toMemoVisibility() ?: MemoVisibility.PRIVATE,
            resources = memo.attachments?.map { convertResource(it) } ?: emptyList(),
            tags = emptyList(),
            creator = creator,
            archived = memo.state == MemosV1State.ARCHIVED,
            updatedAt = memo.updateTime,
            location = memo.location,
            space = memo.space?.ifEmpty { null },
            accountKey = accountKey,
        )
    }

    private suspend fun listMemosByFilter(state: MemosV1State, filter: String): ApiResponse<List<Memo>> {
        var nextPageToken = ""
        val memos = arrayListOf<Memo>()

        do {
            val resp = memosApi.listMemos(PAGE_SIZE, nextPageToken, state, filter)
                .onSuccess { nextPageToken = data.nextPageToken.orEmpty() }
                .mapSuccess { this.memos.filter { it.parent.isNullOrEmpty() }.map { convertMemo(it) } }
            if (resp is ApiResponse.Success) {
                memos.addAll(resp.data)
            } else {
                return resp
            }
        } while (nextPageToken.isNotEmpty())
        return ApiResponse.Success(memos)
    }

    suspend fun listSpaces(): ApiResponse<List<MemoSpace>> {
        val spaces = mutableListOf<MemoSpace>()
        var nextPageToken: String? = null
        do {
            when (val response = memosApi.listSpaces(PAGE_SIZE, nextPageToken)) {
                is ApiResponse.Success -> {
                    spaces.addAll(response.data.spaces)
                    nextPageToken = response.data.nextPageToken?.ifEmpty { null }
                }
                else -> {
                    if (nextPageToken == null && response is ApiResponse.Failure.Error &&
                        response.statusCode == StatusCode.NotFound
                    ) {
                        return ApiResponse.Success(emptyList())
                    }
                    return response.mapSuccess { this.spaces }
                }
            }
        } while (nextPageToken != null)
        return ApiResponse.Success(spaces)
    }

    private fun getId(identifier: String): String {
        return identifier.substringBefore('|').substringAfterLast('/')
    }

    private fun getName(identifier: String): String {
        return identifier.substringBefore('|')
    }

    private suspend fun listCurrentUserMemos(state: MemosV1State): ApiResponse<List<Memo>> {
        return listMemosByFilter(state, "creator == \"$remoteUserIdentifier\"")
    }

    override suspend fun listMemos(): ApiResponse<List<Memo>> {
        return listCurrentUserMemos(MemosV1State.NORMAL)
    }

    override suspend fun listArchivedMemos(): ApiResponse<List<Memo>> {
        return listCurrentUserMemos(MemosV1State.ARCHIVED)
    }

    override suspend fun listWorkspaceMemos(
        pageSize: Int,
        pageToken: String?
    ): ApiResponse<Pair<List<Memo>, String?>> {
        return convertMemoPage(
            memosApi.listMemos(pageSize, pageToken, filter = "visibility in [\"PUBLIC\", \"PROTECTED\"]"),
            topLevelOnly = true,
        )
    }

    internal override suspend fun listComments(
        parent: String,
        pageSize: Int,
        pageToken: String?,
    ): ApiResponse<Pair<List<Memo>, String?>> {
        val parentName = getName(parent)
        if (!COMMENT_PARENT_NAME.matches(parentName)) {
            return ApiResponse.Failure.Exception(IllegalArgumentException("Invalid Memos comment parent"))
        }
        return withCommentRoute { route ->
            val response = when (route) {
                CommentRoute.NESTED -> memosApi.listComments(getId(parentName), pageSize, pageToken)
                CommentRoute.PARENT_FIELD -> memosApi.listMemos(pageSize, pageToken, filter = "parent == \"$parentName\"")
            }
            convertMemoPage(response)
        }
    }

    internal override suspend fun createComment(
        parent: String,
        content: String,
        visibility: MemoVisibility,
        space: String?,
    ): ApiResponse<Memo> {
        val parentName = getName(parent)
        if (!COMMENT_PARENT_NAME.matches(parentName)) {
            return ApiResponse.Failure.Exception(IllegalArgumentException("Invalid Memos comment parent"))
        }
        return withCommentRoute { route ->
            val request = MemosV1CreateMemoRequest(
                content = content,
                visibility = MemosVisibility.fromMemoVisibility(visibility),
                attachments = null,
                space = space,
                parent = if (route == CommentRoute.PARENT_FIELD) parentName else null,
            )
            when (route) {
                CommentRoute.NESTED -> memosApi.createComment(getId(parentName), request)
                CommentRoute.PARENT_FIELD -> memosApi.createMemo(request)
            }.mapSuccess { convertMemo(this) }
        }
    }

    private suspend fun convertMemoPage(
        response: ApiResponse<ListMemosResponse>,
        topLevelOnly: Boolean = false,
    ): ApiResponse<Pair<List<Memo>, String?>> {
        if (response !is ApiResponse.Success) {
            return response.mapSuccess { emptyList<Memo>() to null }
        }
        val memos = response.data.memos.let { page ->
            if (topLevelOnly) page.filter { it.parent.isNullOrEmpty() } else page
        }
        val creators = memos.mapNotNullTo(mutableSetOf()) { it.creator?.ifEmpty { null } }
        val users = if (creators.isEmpty()) emptyMap() else coroutineScope {
            creators.map { creator ->
                async { memosApi.getUser(getId(creator)).getOrNull() }
            }.awaitAll().filterNotNull().associateBy(
                keySelector = { it.name },
                valueTransform = { User(it.name, it.displayName ?: it.username, it.createTime ?: Instant.now()) },
            )
        }

        val memoAccountKey = if (topLevelOnly) accountKey else null
        return response.mapSuccess {
            memos.map { convertMemo(it, it.creator?.let(users::get), memoAccountKey) } to nextPageToken?.ifEmpty { null }
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
        val resp = memosApi.createMemo(
            MemosV1CreateMemoRequest(
                content = content,
                visibility = MemosVisibility.fromMemoVisibility(visibility),
                attachments = resourceRemoteIds.map { MemosV1Resource(name = getName(it)) },
                createTime = createdAt,
                location = location,
                space = space,
            )
        )
            .mapSuccess { convertMemo(this) }
        return resp
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
        val request = UpdateMemoRequest(
            content = content,
            visibility = visibility?.let { MemosVisibility.fromMemoVisibility(it) },
            pinned = pinned,
            state = archived?.let { isArchived -> if (isArchived) MemosV1State.ARCHIVED else MemosV1State.NORMAL },
            updateTime = Instant.now(),
            attachments = resourceRemoteIds?.map { MemosV1Resource(name = getName(it)) },
            location = location?.value,
            space = space?.ifEmpty { null },
        )
        val resp = memosApi.updateMemo(
            getId(remoteId), request,
            updateMask = if (location != null || space != null) {
                request.updateMask(includeLocation = location != null, includeSpace = space != null)
            } else null
        ).mapSuccess { convertMemo(this) }
        return resp
    }

    override suspend fun deleteMemo(remoteId: String): ApiResponse<Unit> {
        return memosApi.deleteMemo(getId(remoteId))
    }

    override suspend fun listResources(): ApiResponse<List<Resource>> {
        return memosApi.listResources().mapSuccess { this.attachments.map { convertResource(it) } }
    }

    override suspend fun createResource(
        filename: String,
        type: MediaType?,
        contentLength: Long?,
        openInputStream: () -> java.io.InputStream,
        memoRemoteId: String?
    ): ApiResponse<Resource> {
        val requestBody = StreamingBase64JsonRequestBody(
            filename = filename,
            type = type?.toString() ?: "application/octet-stream",
            memo = memoRemoteId?.let { getName(it) },
            contentLength = contentLength,
            openInputStream = openInputStream
        )
        return memosApi.createResource(requestBody).mapSuccess { convertResource(this) }
    }

    override suspend fun deleteResource(remoteId: String): ApiResponse<Unit> {
        return memosApi.deleteResource(getId(remoteId))
    }

    override suspend fun getCurrentUser(): ApiResponse<User> {
        val resp = memosApi.getCurrentUser().mapSuccess {
            if (user == null) {
                throw MeowmosException.notLogin
            }
            User(
                user.name,
                user.displayName ?: user.username,
                user.createTime ?: Instant.now(),
                avatarUrl = user.avatarUrl
            )
        }
        if (resp !is ApiResponse.Success) {
            return resp
        }

        return memosApi.getUserSetting(getId(resp.data.identifier)).mapSuccess {
            resp.data.copy(
                defaultVisibility = generalSetting?.memoVisibility?.toMemoVisibility() ?: MemoVisibility.PRIVATE
            )
        }
    }
}
