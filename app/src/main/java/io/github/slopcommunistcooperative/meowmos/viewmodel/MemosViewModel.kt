package io.github.slopcommunistcooperative.meowmos.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.slopcommunistcooperative.meowmos.data.constant.MemosVersionSupport
import io.github.slopcommunistcooperative.meowmos.data.constant.MeowmosException
import io.github.slopcommunistcooperative.meowmos.data.local.entity.MemoEntity
import io.github.slopcommunistcooperative.meowmos.data.local.entity.ResourceEntity
import io.github.slopcommunistcooperative.meowmos.data.model.DailyUsageStat
import io.github.slopcommunistcooperative.meowmos.data.model.Memo
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility
import io.github.slopcommunistcooperative.meowmos.data.model.SyncStatus
import io.github.slopcommunistcooperative.meowmos.data.model.UserSettings
import io.github.slopcommunistcooperative.meowmos.data.repository.MemosV1Repository
import io.github.slopcommunistcooperative.meowmos.data.repository.RemoteRepository
import io.github.slopcommunistcooperative.meowmos.data.service.AccountService
import io.github.slopcommunistcooperative.meowmos.data.service.MemoService
import io.github.slopcommunistcooperative.meowmos.ext.getErrorMessage
import io.github.slopcommunistcooperative.meowmos.ext.settingsDataStore
import io.github.slopcommunistcooperative.meowmos.ext.string
import io.github.slopcommunistcooperative.meowmos.ext.updateUserSettings
import io.github.slopcommunistcooperative.meowmos.widget.WidgetUpdater
import io.github.slopcommunistcooperative.meowmos.util.extractCustomTags
import timber.log.Timber
import java.io.IOException
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.inject.Inject

@HiltViewModel
class MemosViewModel @Inject constructor(
    private val memoService: MemoService,
    private val accountService: AccountService,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    var memos = mutableStateListOf<MemoEntity>()
        private set
    val tags: List<String> by derivedStateOf {
        memos.asSequence()
            .flatMap { extractCustomTags(it.content).asSequence() }
            .filter { it.isNotBlank() }
            .toSortedSet()
            .toList()
    }
    var errorMessage: String? by mutableStateOf(null)
        private set
    var matrix by mutableStateOf(DailyUsageStat.initialMatrix)
        private set

    val host: StateFlow<String?> =
        accountService.currentAccount
            .map { it?.getAccountInfo()?.host }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val syncStatus: StateFlow<SyncStatus> =
        memoService.syncStatus.stateIn(viewModelScope, SharingStarted.Eagerly, SyncStatus())

    private val initialLoad = MutableStateFlow(false)
    internal val initialLoadComplete = initialLoad.asStateFlow()
    internal var initialSyncCompletedFor: String? by mutableStateOf(null)
        private set
    private val commentRevisions = mutableStateMapOf<Pair<String, String>, Int>()
    internal val commentPreviews = CommentPreviewCache { accountKey, parent ->
        listMemoComments(accountKey, parent, COMMENT_PREVIEW_SIZE, null)
    }
    private val spaceTitles = mutableStateMapOf<String, String>()

    fun spaceTitle(spaceName: String?): String? =
        spaceName?.takeIf { it.isNotBlank() }?.let { spaceTitles[it] }

    internal fun commentRevision(accountKey: String, parent: String): Int =
        commentRevisions[accountKey to parent] ?: 0

    internal fun commentSettings(accountKey: String) = appContext.settingsDataStore.data
        .map { settings -> settings.usersList.firstOrNull { it.accountKey == accountKey }?.settings ?: UserSettings() }
        .distinctUntilChanged()

    internal fun rememberCommentVisibility(accountKey: String, visibility: MemoVisibility) {
        viewModelScope.launch {
            try {
                appContext.updateUserSettings(accountKey) { it.copy(lastCommentVisibility = visibility) }
            } catch (error: IOException) {
                Timber.w(error, "Could not remember comment visibility")
            }
        }
    }

    internal suspend fun listMemoComments(
        accountKey: String,
        parent: String,
        pageSize: Int,
        pageToken: String?,
    ): ApiResponse<Pair<List<Memo>, String?>> {
        val repository = commentRepository(accountKey)
            ?: return ApiResponse.Failure.Exception(MeowmosException.notLogin)
        return repository.listComments(parent, pageSize, pageToken)
    }

    internal suspend fun postMemoComment(
        accountKey: String,
        parent: String,
        content: String,
        visibility: MemoVisibility,
        space: String?,
    ): ApiResponse<Memo> = withContext(viewModelScope.coroutineContext) {
        val repository = commentRepository(accountKey)
            ?: return@withContext ApiResponse.Failure.Exception(MeowmosException.notLogin)
        repository.createComment(parent, content, visibility, space).suspendOnSuccess {
            commentPreviews.invalidate(accountKey, parent)
            commentRevisions[accountKey to parent] = commentRevision(accountKey, parent) + 1
        }
    }

    private suspend fun commentRepository(accountKey: String): RemoteRepository? =
        accountService.getRemoteRepository()?.takeIf { it.accountKey == accountKey }

    init {
        snapshotFlow { memos.toList() }
            .onEach { matrix = calculateMatrix() }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            try {
                loadMemosSnapshot()
            } finally {
                initialLoad.value = true
            }

            memoService.syncStatus
                .map { it.syncing }
                .distinctUntilChanged()
                .collectLatest { syncing ->
                    if (syncing) {
                        return@collectLatest
                    }
                    memoService.memos.collectLatest { latestMemos ->
                        applyMemos(latestMemos)
                    }
                }
        }

        viewModelScope.launch {
            accountService.currentAccount
                .map { it?.accountKey() }
                .distinctUntilChanged()
                .collectLatest {
                    commentPreviews.clear()
                    spaceTitles.clear()
                    val repository = accountService.getRemoteRepository() as? MemosV1Repository ?: return@collectLatest
                    val spaces = repository.listSpaces().getOrNull().orEmpty()
                    spaces.forEach { space ->
                        if (space.title.isNotBlank()) {
                            spaceTitles[space.name] = space.title
                        }
                    }
                }
        }

        viewModelScope.launch {
            accountService.currentAccount
                .map { it?.accountKey() }
                .distinctUntilChanged()
                .collectLatest { accountKey ->
                    initialSyncCompletedFor = null
                    if (accountKey != null) {
                        try {
                            syncMemosAutomatically()
                        } finally {
                            initialSyncCompletedFor = accountKey
                        }
                    }
                }
        }
    }

    private suspend fun loadMemosSnapshot() {
        when (val response = memoService.getRepository().listMemos()) {
            is ApiResponse.Success -> {
                applyMemos(response.data)
            }
            else -> {
                errorMessage = response.getErrorMessage()
            }
        }
    }

    suspend fun refreshLocalSnapshot() = withContext(viewModelScope.coroutineContext) {
        loadMemosSnapshot()
    }

    suspend fun awaitInitialLoad() {
        initialLoad.first { it }
    }

    private fun applyMemos(latestMemos: List<MemoEntity>) {
        memos.clear()
        memos.addAll(latestMemos)
        errorMessage = null
    }

    suspend fun loadMemos(syncAfterLoad: Boolean = true) = withContext(viewModelScope.coroutineContext) {
        if (syncAfterLoad) syncMemosAutomatically()
    }

    private suspend fun syncMemosAutomatically() {
        val compatibility = accountService.checkCurrentAccountSyncCompatibility(isAutomatic = true)
        if (compatibility !is AccountService.SyncCompatibility.Allowed) return

        val syncResult = memoService.sync(false)
        if (syncResult is ApiResponse.Success) {
            WidgetUpdater.updateWidgets(appContext)
        } else if (!syncResult.isAccessTokenInvalidFailure()) {
            errorMessage = syncResult.getErrorMessage()
        }
    }

    suspend fun refreshMemos(allowHigherV1Version: String? = null): ManualSyncResult = withContext(viewModelScope.coroutineContext) {
        when (val compatibility = accountService.checkCurrentAccountSyncCompatibility(
            isAutomatic = false,
            allowHigherV1Version = allowHigherV1Version
        )) {
            is AccountService.SyncCompatibility.Blocked -> {
                return@withContext ManualSyncResult.Blocked(
                    compatibility.message ?: MemosVersionSupport.supportedVersionsMessage(appContext)
                )
            }
            is AccountService.SyncCompatibility.RequiresConfirmation -> {
                return@withContext ManualSyncResult.RequiresConfirmation(
                    version = compatibility.version,
                    message = compatibility.message
                )
            }
            AccountService.SyncCompatibility.Allowed -> Unit
        }

        val syncResult = memoService.sync(true)
        if (syncResult is ApiResponse.Success) {
            if (allowHigherV1Version != null) {
                accountService.rememberAcceptedUnsupportedSyncVersion(allowHigherV1Version)
            }
            commentPreviews.clear()
            WidgetUpdater.updateWidgets(appContext)
        } else {
            val message = syncResult.getErrorMessage()
            errorMessage = message
            return@withContext ManualSyncResult.Failed(message)
        }
        ManualSyncResult.Completed
    }

    private fun ApiResponse<Unit>.isAccessTokenInvalidFailure(): Boolean {
        return this is ApiResponse.Failure.Exception && this.throwable == MeowmosException.accessTokenInvalid
    }

    suspend fun updateMemoPinned(memoIdentifier: String, pinned: Boolean) = withContext(viewModelScope.coroutineContext) {
        memoService.getRepository().updateMemo(memoIdentifier, pinned = pinned).suspendOnSuccess {
            updateMemo(data)
            // Update widgets after pinning/unpinning a memo
            WidgetUpdater.updateWidgets(appContext)
        }
    }

    suspend fun editMemo(memoIdentifier: String, content: String, resourceList: List<ResourceEntity>?, visibility: MemoVisibility): ApiResponse<MemoEntity> = withContext(viewModelScope.coroutineContext) {
        memoService.getRepository().updateMemo(memoIdentifier, content, resourceList, visibility).suspendOnSuccess {
            updateMemo(data)
            // Update widgets after editing a memo
            WidgetUpdater.updateWidgets(appContext)
        }
    }

    suspend fun archiveMemo(memoIdentifier: String) = withContext(viewModelScope.coroutineContext) {
        memoService.getRepository().archiveMemo(memoIdentifier).suspendOnSuccess {
            memos.removeIf { it.identifier == memoIdentifier }
            // Update widgets after archiving a memo
            WidgetUpdater.updateWidgets(appContext)
        }
    }

    suspend fun deleteMemo(memoIdentifier: String) = withContext(viewModelScope.coroutineContext) {
        memoService.getRepository().deleteMemo(memoIdentifier).suspendOnSuccess {
            memos.removeIf { it.identifier == memoIdentifier }
            // Update widgets after deleting a memo
            WidgetUpdater.updateWidgets(appContext)
        }
    }

    suspend fun cacheResourceFile(resourceIdentifier: String, downloadedUri: Uri): ApiResponse<Unit> = withContext(viewModelScope.coroutineContext) {
        memoService.getRepository().cacheResourceFile(resourceIdentifier, downloadedUri)
    }

    suspend fun getResourceById(resourceIdentifier: String): ResourceEntity? = withContext(viewModelScope.coroutineContext) {
        when (val response = memoService.getRepository().listResources()) {
            is ApiResponse.Success -> response.data.firstOrNull { it.identifier == resourceIdentifier }
            else -> null
        }
    }

    private fun updateMemo(memo: MemoEntity) {
        val index = memos.indexOfFirst { it.identifier == memo.identifier }
        if (index != -1) {
            memos[index] = memo
        }
    }

    private fun calculateMatrix(): List<DailyUsageStat> {
        val countMap = HashMap<LocalDate, Int>()

        for (memo in memos) {
            val date = memo.date.atZone(OffsetDateTime.now().offset).toLocalDate()
            countMap[date] = (countMap[date] ?: 0) + 1
        }

        return DailyUsageStat.initialMatrix.map {
            it.copy(count = countMap[it.date] ?: 0)
        }
    }
}

val LocalMemos =
    compositionLocalOf<MemosViewModel> { error(io.github.slopcommunistcooperative.meowmos.R.string.memos_view_model_not_found.string) }

sealed class ManualSyncResult {
    object Completed : ManualSyncResult()
    data class Blocked(val message: String) : ManualSyncResult()
    data class RequiresConfirmation(val version: String, val message: String) : ManualSyncResult()
    data class Failed(val message: String) : ManualSyncResult()
}
