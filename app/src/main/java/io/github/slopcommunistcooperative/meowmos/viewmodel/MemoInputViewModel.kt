package io.github.slopcommunistcooperative.meowmos.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import io.github.slopcommunistcooperative.meowmos.data.local.entity.MemoEntity
import io.github.slopcommunistcooperative.meowmos.data.local.entity.ResourceEntity
import io.github.slopcommunistcooperative.meowmos.data.model.MemoLocation
import io.github.slopcommunistcooperative.meowmos.data.model.MemoLocationChange
import io.github.slopcommunistcooperative.meowmos.data.model.MemoSpace
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility
import io.github.slopcommunistcooperative.meowmos.data.model.UserSettings
import io.github.slopcommunistcooperative.meowmos.data.repository.MemosV1Repository
import io.github.slopcommunistcooperative.meowmos.data.service.AccountService
import io.github.slopcommunistcooperative.meowmos.data.service.MemoService
import io.github.slopcommunistcooperative.meowmos.ext.settingsDataStore
import io.github.slopcommunistcooperative.meowmos.ext.updateUserSettings
import io.github.slopcommunistcooperative.meowmos.widget.WidgetUpdater
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MemoInputViewModel @Inject constructor(
    application: Application,
    private val memoService: MemoService,
    private val accountService: AccountService,
) : AndroidViewModel(application) {
    private val context: Application get() = getApplication()
    val currentAccount = accountService.currentAccount
    val draft = context.settingsDataStore.data.map { settings ->
        settings.usersList.firstOrNull { it.accountKey == settings.currentUser }?.settings?.draft
    }
    val draftLocation = context.settingsDataStore.data.map { settings ->
        settings.usersList.firstOrNull { it.accountKey == settings.currentUser }?.settings?.draftLocation
    }
    val autosaveEnabled = context.settingsDataStore.data.map { settings ->
        settings.usersList.firstOrNull { it.accountKey == settings.currentUser }?.settings?.autosave ?: false
    }

    suspend fun lastMemoSpace(accountKey: String): MemoSpace? =
        context.settingsDataStore.data.first().usersList.firstOrNull { it.accountKey == accountKey }?.settings?.lastMemoSpace

    fun rememberMemoSpace(accountKey: String, space: MemoSpace?) {
        updateUserSettings(accountKey) { it.copy(lastMemoSpace = space) }
    }

    suspend fun listSpaces(): List<MemoSpace>? =
        (accountService.getRemoteRepository() as? MemosV1Repository)?.listSpaces()?.getOrNull()
    var uploadResources = mutableStateListOf<ResourceEntity>()

    // Identifier of the memo row that autosave writes to; null until the first autosave creates it.
    @Volatile
    var autosaveIdentifier: String? = null
    private val autosaveMutex = Mutex()

    suspend fun autosave(content: String, visibility: MemoVisibility, tags: List<String>, clearDraftOnCreate: Boolean = false, location: MemoLocation? = null, space: String? = null): ApiResponse<MemoEntity> = autosaveMutex.withLock {
        withContext(NonCancellable) {
            val repository = memoService.getRepository()
            val identifier = autosaveIdentifier
            if (identifier == null) {
                repository.createMemo(content, visibility, uploadResources, tags, deferPush = true, location = location, space = space).also { response ->
                    response.suspendOnSuccess {
                        autosaveIdentifier = data.identifier
                        if (clearDraftOnCreate) {
                            updateDraft("")
                        }
                    }
                }
            } else {
                repository.updateMemo(identifier, content, null, visibility, tags, deferPush = true, location = MemoLocationChange(location), space = space)
            }
        }
    }

    suspend fun flushAutosave(content: String, visibility: MemoVisibility, tags: List<String>, clearDraftOnCreate: Boolean = false, location: MemoLocation? = null, space: String? = null): ApiResponse<MemoEntity> {
        val response = autosave(content, visibility, tags, clearDraftOnCreate, location, space)
        response.suspendOnSuccess {
            // Not cancellable: flush drops the deferred push before enqueueing the real one
            withContext(NonCancellable) {
                autosaveIdentifier?.let { memoService.getRepository().flushPendingPush(it) }
            }
            WidgetUpdater.updateWidgets(getApplication())
        }
        return response
    }

    // In viewModelScope, so it outlives the page's composition (leaving the app, app lock).
    fun flushAutosaveInBackground(content: String, visibility: MemoVisibility, tags: List<String>, clearDraftOnCreate: Boolean = false, location: MemoLocation? = null, space: String? = null) {
        viewModelScope.launch {
            flushAutosave(content, visibility, tags, clearDraftOnCreate, location, space)
        }
    }

    suspend fun discardEmptyAutosave() = autosaveMutex.withLock {
        withContext(NonCancellable) {
            val identifier = autosaveIdentifier ?: return@withContext
            memoService.getRepository().deleteMemo(identifier)
            autosaveIdentifier = null
        }
    }

    suspend fun loadMemo(identifier: String): MemoEntity? = memoService.getRepository().getMemo(identifier)

    suspend fun createMemo(content: String, visibility: MemoVisibility, tags: List<String>, location: MemoLocation? = null, space: String? = null): ApiResponse<MemoEntity> = withContext(viewModelScope.coroutineContext) {
        val response = memoService.getRepository().createMemo(content, visibility, uploadResources, tags, location = location, space = space)
        // Update widgets when a new memo is created
        response.suspendOnSuccess {
            WidgetUpdater.updateWidgets(getApplication())
        }
        response
    }

    suspend fun editMemo(identifier: String, content: String, visibility: MemoVisibility, tags: List<String>, location: MemoLocation? = null, space: String? = null): ApiResponse<MemoEntity> = withContext(viewModelScope.coroutineContext) {
        val response = memoService.getRepository().updateMemo(identifier, content, uploadResources, visibility, tags, location = MemoLocationChange(location), space = space)
        // Update widgets when a memo is edited
        response.suspendOnSuccess {
            WidgetUpdater.updateWidgets(getApplication())
        }
        response
    }

    fun updateDraft(content: String, location: MemoLocation? = null) {
        updateUserSettings { it.copy(draft = content, draftLocation = location) }
    }

    private fun updateUserSettings(accountKey: String? = null, transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            context.updateUserSettings(accountKey, transform)
        }
    }

    suspend fun upload(uri: Uri, memoIdentifier: String?): ApiResponse<ResourceEntity> = withContext(Dispatchers.IO) {
        // Held so an autosave cannot create the memo row while the file is being copied, which
        // would leave this resource unlinked (autosave updates never re-send resources).
        autosaveMutex.withLock {
            try {
                val mimeType = context.contentResolver.getType(uri)
                val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                val filename = queryDisplayName(uri)
                    ?: ("attachment_${UUID.randomUUID()}" + if (extension.isNullOrBlank()) "" else ".$extension")

                memoService.getRepository()
                    .createResource(filename, mimeType?.toMediaTypeOrNull(), uri, memoIdentifier ?: autosaveIdentifier)
                    .suspendOnSuccess {
                        uploadResources.add(data)
                    }
            } catch (e: Exception) {
                ApiResponse.Failure.Exception(e)
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use null
                }
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index == -1) {
                    null
                } else {
                    cursor.getString(index)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun deleteResource(resourceIdentifier: String) = viewModelScope.launch {
        memoService.getRepository().deleteResource(resourceIdentifier).suspendOnSuccess {
            uploadResources.removeIf { it.identifier == resourceIdentifier }
        }
    }
}
