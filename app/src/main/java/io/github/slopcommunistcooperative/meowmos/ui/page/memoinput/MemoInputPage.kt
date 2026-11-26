package io.github.slopcommunistcooperative.meowmos.ui.page.memoinput

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.sandwich.suspendOnSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.github.slopcommunistcooperative.meowmos.MeowmosFileProvider
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.data.model.MemoLocation
import io.github.slopcommunistcooperative.meowmos.data.model.MemoLocationChange
import io.github.slopcommunistcooperative.meowmos.data.model.MemoSpace
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility
import io.github.slopcommunistcooperative.meowmos.data.model.ShareContent
import io.github.slopcommunistcooperative.meowmos.ext.popBackStackIfLifecycleIsResumed
import io.github.slopcommunistcooperative.meowmos.ext.suspendOnErrorMessage
import io.github.slopcommunistcooperative.meowmos.ui.page.common.LocalRootNavController
import io.github.slopcommunistcooperative.meowmos.util.extractCustomTags
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalMemos
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState
import io.github.slopcommunistcooperative.meowmos.viewmodel.MemoInputViewModel

private const val MaxSelectableImages = 100

@Composable
fun MemoInputPage(
    viewModel: MemoInputViewModel = hiltViewModel(),
    memoIdentifier: String? = null,
    shareContent: ShareContent? = null
) {
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }
    val navController = LocalRootNavController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val memosViewModel = LocalMemos.current
    val userStateViewModel = LocalUserState.current
    val currentAccount by userStateViewModel.currentAccount.collectAsStateWithLifecycle()
    val memo = remember { memosViewModel.memos.toList().find { it.identifier == memoIdentifier } }
    // What the editor was loaded from: `memo`, replaced by the database row once it is read (the list
    // copy above is not refreshed while a push or sync runs, so it can be older).
    var baseline by remember { mutableStateOf(memo) }
    val autosaveEnabled by viewModel.autosaveEnabled.collectAsStateWithLifecycle(initialValue = false)
    var autosaveIdentifier by rememberSaveable { mutableStateOf(memoIdentifier ?: memo?.identifier) }
    var editorRestored by rememberSaveable { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }
    var autosaveDirty by remember { mutableStateOf(false) }
    var exiting by remember { mutableStateOf(false) }
    var initialContent by remember { mutableStateOf(memo?.content ?: "") }
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(memo?.content ?: "", TextRange(memo?.content?.length ?: 0)))
    }
    var locationState by rememberSaveable(stateSaver = Saver<MemoLocationChange, String>(
        save = { Json.encodeToString(it.value) },
        restore = { MemoLocationChange(Json.decodeFromString<MemoLocation?>(it)) }
    )) { mutableStateOf(MemoLocationChange(memo?.location)) }
    var showLocationDialog by rememberSaveable { mutableStateOf(false) }
    var visibilityMenuExpanded by remember { mutableStateOf(false) }
    var tagMenuExpanded by remember { mutableStateOf(false) }
    var photoImageUri by remember { mutableStateOf<Uri?>(null) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    val defaultVisibility = userStateViewModel.currentUser?.defaultVisibility
        ?.takeUnless { it == MemoVisibility.SPACE } ?: MemoVisibility.PRIVATE
    var currentVisibility by rememberSaveable { mutableStateOf(memo?.visibility ?: defaultVisibility) }
    var initialVisibility by rememberSaveable { mutableStateOf(currentVisibility) }
    var currentSpace by rememberSaveable { mutableStateOf(memo?.space?.ifEmpty { null }) }
    var spaceEdited by rememberSaveable { mutableStateOf(false) }
    var rememberedSpace by remember { mutableStateOf<MemoSpace?>(null) }
    var spaces by remember { mutableStateOf<List<MemoSpace>>(emptyList()) }

    fun spaceForUpdate(): String? =
        if (spaceEdited || memoIdentifier == null) currentSpace.orEmpty() else null

    fun selectSpace(space: MemoSpace?) {
        if (currentSpace == space?.name) return
        currentSpace = space?.name
        spaceEdited = true
        currentVisibility = when {
            space != null -> MemoVisibility.SPACE
            currentVisibility == MemoVisibility.SPACE -> MemoVisibility.PRIVATE
            else -> currentVisibility
        }
        if (memoIdentifier == null) {
            initialVisibility = currentVisibility
            rememberedSpace = space
            currentAccount?.let { viewModel.rememberMemoSpace(it.accountKey(), space) }
        }
    }

    val validMimeTypePrefixes = remember {
        setOf("text/")
    }

    // An existing memo nothing was changed in. Autosave must not write it back on Send/Back: if the
    // text is older than the database (see `baseline`), writing it would revert newer changes.
    fun isUntouchedExistingMemo(): Boolean {
        val base = baseline ?: return false
        return !autosaveDirty &&
            text.text == base.content &&
            currentVisibility == base.visibility &&
            locationState.value == base.location &&
            currentSpace == base.space?.ifEmpty { null } &&
            viewModel.uploadResources.size == base.resources.size
    }

    fun submit() = coroutineScope.launch {
        val tags = extractCustomTags(text.text)

        if (autosaveEnabled && isUntouchedExistingMemo()) {
            exiting = true
            navController.popBackStack()
            return@launch
        }

        if (autosaveEnabled) {
            viewModel.flushAutosave(text.text, currentVisibility, tags.toList(), clearDraftOnCreate = shareContent == null, location = locationState.value, space = spaceForUpdate()).suspendOnSuccess {
                exiting = true
                memosViewModel.refreshLocalSnapshot()
                navController.popBackStack()
            }.suspendOnErrorMessage { message ->
                snackbarState.showSnackbar(message)
            }
            return@launch
        }

        baseline?.let {
            viewModel.editMemo(it.identifier, text.text, currentVisibility, tags.toList(), locationState.value, spaceForUpdate()).suspendOnSuccess {
                memosViewModel.refreshLocalSnapshot()
                navController.popBackStack()
            }.suspendOnErrorMessage { message ->
                snackbarState.showSnackbar(message)
            }
            return@launch
        }

        viewModel.createMemo(text.text, currentVisibility, tags.toList(), locationState.value, currentSpace).suspendOnSuccess {
            exiting = true
            text = TextFieldValue("")
            locationState = MemoLocationChange(null)
            viewModel.updateDraft("")
            memosViewModel.refreshLocalSnapshot()
            navController.popBackStack()
        }.suspendOnErrorMessage { message ->
            snackbarState.showSnackbar(message)
        }
    }

    fun handleExit() {
        if (autosaveEnabled) {
            coroutineScope.launch {
                // Only a row this editor created may be discarded. `memo` is also null when editing a
                // memo the list has not loaded (e.g. after process death); that memo must not be deleted.
                val autosaveRow = viewModel.autosaveIdentifier
                val ownsAutosaveRow = autosaveRow == null || autosaveRow != memoIdentifier
                if (ownsAutosaveRow && text.text.isEmpty() && viewModel.uploadResources.isEmpty()) {
                    viewModel.discardEmptyAutosave()
                } else if (!isUntouchedExistingMemo()) {
                    viewModel.flushAutosave(
                        text.text,
                        currentVisibility,
                        extractCustomTags(text.text).toList(),
                        clearDraftOnCreate = shareContent == null,
                        location = locationState.value,
                        space = spaceForUpdate(),
                    )
                }
                exiting = true
                memosViewModel.refreshLocalSnapshot()
                navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
            }
            return
        }
        if (text.text != initialContent || locationState.value != baseline?.location ||
            currentVisibility != (baseline?.visibility ?: initialVisibility) ||
            (memoIdentifier != null && currentSpace != baseline?.space?.ifEmpty { null }) ||
            viewModel.uploadResources.size != (baseline?.resources?.size ?: 0)
        ) {
            showExitConfirmation = true
        } else {
            navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
        }
    }

    fun uploadImages(uris: List<Uri>) = coroutineScope.launch {
        uris.take(MaxSelectableImages).forEach { uri ->
            viewModel.upload(uri, autosaveIdentifier ?: memo?.identifier).suspendOnErrorMessage { message ->
                snackbarState.showSnackbar(message)
            }
        }
        delay(300)
        focusRequester.requestFocus()
    }

    fun uploadImage(uri: Uri) {
        uploadImages(listOf(uri))
    }

    val pickImages = rememberLauncherForActivityResult(
        PickMultipleVisualMedia(MaxSelectableImages)
    ) { uris ->
        if (uris.isNotEmpty()) {
            uploadImages(uris)
        }
    }

    val takePhoto = rememberLauncherForActivityResult(TakePicture()) { success ->
        if (success) {
            photoImageUri?.let { uploadImage(it) }
        }
    }

    val pickAttachment = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri?.let {
            coroutineScope.launch {
                viewModel.upload(it, autosaveIdentifier ?: memo?.identifier).suspendOnErrorMessage { message ->
                    snackbarState.showSnackbar(message)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            MemoInputTopBar(
                isEditMode = memoIdentifier != null,
                initialized = initialized,
                canSubmit = text.text.isNotEmpty() || viewModel.uploadResources.isNotEmpty(),
                onClose = { handleExit() },
                onSubmit = { submit() },
                destination = {
                    if (initialized && currentAccount is Account.MemosV1 && (spaces.isNotEmpty() || currentSpace != null)) {
                        MemoSpacePicker(spaces, currentSpace, rememberedSpace, ::selectSpace)
                    }
                }
            )
        },
        bottomBar = {
            MemoInputBottomBar(
                currentAccount = currentAccount,
                currentVisibility = currentVisibility,
                showSpaceVisibility = currentSpace != null,
                visibilityMenuExpanded = visibilityMenuExpanded,
                onVisibilityExpandedChange = { visibilityMenuExpanded = it },
                onVisibilitySelected = { currentVisibility = it },
                tags = memosViewModel.tags.toList(),
                tagMenuExpanded = tagMenuExpanded,
                onTagExpandedChange = { tagMenuExpanded = it },
                onHashTagClick = {
                    text = replaceSelection(text, "#")
                },
                onTagSelected = { tag ->
                    text = replaceSelection(text, "#$tag ")
                },
                onToggleTodoItem = {
                    text = toggleTodoItemInText(text)
                },
                onPickImage = {
                    pickImages.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                },
                onPickAttachment = {
                    pickAttachment.launch(arrayOf("*/*"))
                },
                onTakePhoto = {
                    try {
                        val uri = MeowmosFileProvider.getImageUri(navController.context)
                        photoImageUri = uri
                        takePhoto.launch(uri)
                    } catch (e: ActivityNotFoundException) {
                        coroutineScope.launch {
                            snackbarState.showSnackbar(e.localizedMessage ?: navController.context.getString(R.string.unable_to_take_picture))
                        }
                    }
                },
                showLocation = initialized && (currentAccount is Account.Local || currentAccount is Account.MemosV1),
                onLocationClick = { showLocationDialog = true },
                onFormat = { format ->
                    text = applyMarkdownFormatToText(text, format)
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        }
    ) { innerPadding ->
        MemoInputEditor(
            modifier = Modifier.padding(innerPadding),
            text = text,
            onTextChange = { updated ->
                if (
                    text.text != updated.text &&
                    updated.selection.start == updated.selection.end &&
                    updated.text.length == text.text.length + 1 &&
                    updated.selection.start > 0 &&
                    updated.text[updated.selection.start - 1] == '\n'
                ) {
                    val handled = handleEnterInText(text)
                    if (handled != null) {
                        text = handled
                        return@MemoInputEditor
                    }
                }
                text = updated
            },
            focusRequester = focusRequester,
            validMimeTypePrefixes = validMimeTypePrefixes,
            onDroppedText = { droppedText ->
                text = text.copy(text = text.text + droppedText)
            },
            uploadResources = viewModel.uploadResources.toList(),
            inputViewModel = viewModel,
            location = locationState.value,
            onLocationClick = { showLocationDialog = true }
        )
    }

    if (showLocationDialog) {
        MemoLocationDialog(
            location = locationState.value,
            onConfirm = {
                locationState = MemoLocationChange(it)
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false }
        )
    }

    if (showExitConfirmation) {
        SaveChangesDialog(
            onSave = {
                showExitConfirmation = false
                submit()
            },
            onDiscard = {
                showExitConfirmation = false
                exiting = true
                text = TextFieldValue("")
                locationState = MemoLocationChange(null)
                if (memoIdentifier == null && shareContent == null) viewModel.updateDraft("")
                navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
            },
            onDismiss = {
                showExitConfirmation = false
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.autosaveIdentifier = autosaveIdentifier
        viewModel.uploadResources.clear()
        val initialRow = memo ?: (memoIdentifier ?: autosaveIdentifier)?.let { viewModel.loadMemo(it) }
        when {
            initialRow != null -> {
                viewModel.uploadResources.addAll(initialRow.resources)
                initialContent = initialRow.content
                // Adopt the database row if the list copy was stale and nothing was edited yet
                val fresh = viewModel.loadMemo(initialRow.identifier) ?: initialRow
                if (!editorRestored && (memo == null || isUntouchedExistingMemo())) {
                    baseline = fresh
                    initialContent = fresh.content
                    currentVisibility = fresh.visibility
                    currentSpace = fresh.space?.ifEmpty { null }
                    locationState = MemoLocationChange(fresh.location)
                    viewModel.uploadResources.clear()
                    viewModel.uploadResources.addAll(fresh.resources)
                    text = TextFieldValue(fresh.content, TextRange(fresh.content.length))
                }
                baseline = fresh
            }

            shareContent != null && !editorRestored -> {
                text = TextFieldValue(shareContent.text, TextRange(shareContent.text.length))
                for (item in shareContent.images) {
                    uploadImage(item)
                }
            }

            else -> {
                // After process death with an autosaved row, the restored text is newer than the draft
                if (autosaveIdentifier == null && !editorRestored) {
                    if (locationState.value == null) locationState = MemoLocationChange(viewModel.draftLocation.first())
                    viewModel.draft.first()?.let {
                        text = TextFieldValue(it, TextRange(it.length))
                    }
                }
            }
        }
        val account = viewModel.currentAccount.first()
        if (account is Account.MemosV1) {
            val accountKey = account.accountKey()
            val lastSpace = viewModel.lastMemoSpace(accountKey)
            rememberedSpace = lastSpace
            if (!editorRestored && memoIdentifier == null && initialRow == null && lastSpace != null) {
                currentSpace = lastSpace.name
                currentVisibility = MemoVisibility.SPACE
                val available = viewModel.listSpaces()
                if (available != null) {
                    spaces = available
                    if (available.none { it.name == lastSpace.name }) {
                        currentSpace = null
                        currentVisibility = MemoVisibility.PRIVATE
                        rememberedSpace = null
                        viewModel.rememberMemoSpace(accountKey, null)
                        launch {
                            snackbarState.showSnackbar(navController.context.getString(R.string.memo_space_unavailable))
                        }
                    }
                }
            } else {
                launch { spaces = viewModel.listSpaces().orEmpty() }
            }
        }
        if (!editorRestored) initialVisibility = currentVisibility
        editorRestored = true
        initialized = true
        delay(300)
        focusRequester.requestFocus()
    }

    // Restarts per change; a restart cancels a previous run still waiting for the autosave mutex,
    // so at most one write runs and one (the newest) waits.
    LaunchedEffect(initialized, autosaveEnabled, text.text, currentVisibility, currentSpace, locationState.value, viewModel.uploadResources.size) {
        if (!initialized || !autosaveEnabled) {
            return@LaunchedEffect
        }
        autosaveIdentifier = autosaveIdentifier ?: viewModel.autosaveIdentifier
        if (autosaveIdentifier == null && text.text.isEmpty() && viewModel.uploadResources.isEmpty()) {
            return@LaunchedEffect
        }
        if (isUntouchedExistingMemo()) {
            return@LaunchedEffect
        }
        autosaveDirty = true
        viewModel.autosave(
            text.text,
            currentVisibility,
            extractCustomTags(text.text).toList(),
            clearDraftOnCreate = shareContent == null,
            location = locationState.value,
            space = spaceForUpdate(),
        ).suspendOnSuccess {
            autosaveIdentifier = data.identifier
        }
    }

    // Leaving the app: rewrite the latest text and push it now. The deferred push only lives as long
    // as the process, and the app lock tears this page down on return without calling handleExit.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && initialized && autosaveEnabled && autosaveDirty && !exiting) {
                viewModel.flushAutosaveInBackground(
                    text.text,
                    currentVisibility,
                    extractCustomTags(text.text).toList(),
                    clearDraftOnCreate = shareContent == null,
                    location = locationState.value,
                    space = spaceForUpdate(),
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (autosaveEnabled) {
                // Already flushed by submit/handleExit; a system-initiated dispose leaves needsSync
                // set, so the deferred push or the next sync uploads the row.
                return@onDispose
            }
            if (memoIdentifier == null && baseline == null && shareContent == null && initialized && !exiting) {
                viewModel.updateDraft(text.text, locationState.value)
            }
        }
    }
}
