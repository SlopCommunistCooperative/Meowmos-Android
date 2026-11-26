package io.github.slopcommunistcooperative.meowmos.ui.page.memos

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.sandwich.ApiResponse
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.data.model.Memo
import io.github.slopcommunistcooperative.meowmos.data.model.MemoRepresentable
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility
import io.github.slopcommunistcooperative.meowmos.data.model.UserSettings
import io.github.slopcommunistcooperative.meowmos.data.model.commentVisibilityFor
import io.github.slopcommunistcooperative.meowmos.ext.titleResource
import io.github.slopcommunistcooperative.meowmos.ui.component.ActionIconButton
import io.github.slopcommunistcooperative.meowmos.ui.component.MemoContent
import io.github.slopcommunistcooperative.meowmos.ui.page.memoinput.MemoEditorTextField
import io.github.slopcommunistcooperative.meowmos.ui.page.memoinput.MemoVisibilityPicker
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalMemos
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val PREVIEW_SIZE = 3
private const val THREAD_PAGE_SIZE = 20

private data class CommentPageRequest(val token: String? = null, val retry: Int = 0)

@Composable
internal fun MemoComments(
    memo: MemoRepresentable,
    accountKey: String,
    space: String?,
    onViewAll: (() -> Unit)? = null,
    refresh: Int = 0,
) {
    val account by LocalUserState.current.currentAccount.collectAsStateWithLifecycle()
    val parent = memo.remoteId ?: return
    if (account == null || account is Account.Local || account?.accountKey() != accountKey) return

    key(accountKey, parent) {
        val viewModel = LocalMemos.current
        val revision = viewModel.commentRevision(accountKey, parent)
        val preview = onViewAll != null
        val pageSize = if (preview) PREVIEW_SIZE else THREAD_PAGE_SIZE
        var request by remember(revision, refresh, pageSize) { mutableStateOf(CommentPageRequest()) }
        var response by remember(revision, refresh, pageSize, request) {
            mutableStateOf<ApiResponse<Pair<List<Memo>, String?>>?>(null)
        }
        var comments by remember { mutableStateOf(emptyList<Memo>()) }
        var editing by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(revision, refresh, pageSize, request) {
            val result = viewModel.listMemoComments(accountKey, parent, pageSize, request.token)
            if (result is ApiResponse.Success) {
                comments = if (request.token == null) result.data.first else comments + result.data.first
            }
            response = result
        }
        val nextPage = (response as? ApiResponse.Success)?.data?.second

        Column(Modifier.fillMaxWidth().padding(top = if (preview) 0.dp else 12.dp)) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            )
            if (preview) {
                comments.take(PREVIEW_SIZE).forEach { comment -> CommentPreview(comment) }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!preview) {
                    Text(
                        stringResource(R.string.memo_comments),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (!memo.archived && !editing) {
                    val label = stringResource(if (preview) R.string.memo_comment_reply else R.string.memo_comment_write)
                    if (preview) {
                        ActionIconButton(label, onClick = { editing = true }) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null)
                        }
                    } else {
                        TextButton(onClick = { editing = true }) { Text(label) }
                    }
                }
                if (onViewAll != null && comments.isNotEmpty()) {
                    TextButton(onClick = onViewAll) { Text(stringResource(R.string.memo_comment_view_all)) }
                }
            }
            if (editing && !memo.archived) {
                CommentComposer(accountKey, parent, memo.visibility, space, onClose = { editing = false })
            }
            if (!preview) {
                comments.forEach { comment ->
                    key(comment.remoteId) { CommentBody(comment) }
                }
                when {
                    response == null -> Text(
                        stringResource(R.string.loading),
                        Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    response is ApiResponse.Failure -> Row(
                        Modifier.padding(horizontal = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.memo_comment_load_failed), Modifier.weight(1f))
                        TextButton(onClick = { request = request.copy(retry = request.retry + 1) }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                    nextPage != null -> TextButton(
                        onClick = { request = CommentPageRequest(nextPage) },
                        modifier = Modifier.padding(horizontal = 15.dp),
                    ) { Text(stringResource(R.string.view_more)) }
                }
            }
        }
    }
}

@Composable
private fun CommentPreview(comment: Memo) {
    val content = comment.content.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
        ?: stringResource(R.string.attachment)
    val author = comment.creator?.name?.takeIf { it.isNotBlank() }
    Text(
        if (author == null) content else "$author: $content",
        Modifier.fillMaxWidth().padding(start = 15.dp, top = 4.dp, end = 15.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun CommentBody(comment: Memo) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            comment.creator?.name.orEmpty(),
            Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            DateUtils.getRelativeTimeSpanString(
                comment.date.toEpochMilli(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS,
            ).toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    MemoContent(comment, selectable = true)
}

@Composable
private fun CommentComposer(
    accountKey: String,
    parent: String,
    parentVisibility: MemoVisibility,
    parentSpace: String?,
    onClose: () -> Unit,
) {
    val viewModel = LocalMemos.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val settings by produceState<UserSettings?>(null, accountKey) {
        value = viewModel.commentSettings(accountKey).first()
    }
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var selected by rememberSaveable { mutableStateOf<MemoVisibility?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var posting by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val space = parentSpace?.takeIf { it.isNotEmpty() }
    val visibility = selected?.takeUnless { it == MemoVisibility.SPACE && space == null }
        ?: settings?.commentVisibilityFor(parentVisibility, space != null)
        ?: MemoVisibility.PRIVATE
    val audience = stringResource(visibility.titleResource)
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(Modifier.padding(horizontal = 15.dp, vertical = 8.dp)) {
        MemoEditorTextField(
            modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp).focusRequester(focusRequester),
            text = text,
            onTextChange = { text = it },
            label = stringResource(R.string.memo_comment_write),
            enabled = !posting,
        )
        if (failed) {
            Text(
                stringResource(R.string.memo_comment_post_failed),
                Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            MemoVisibilityPicker(
                currentVisibility = visibility,
                showSpaceVisibility = space != null,
                expanded = expanded,
                onExpandedChange = { expanded = it },
                onVisibilitySelected = {
                    selected = it
                    viewModel.rememberCommentVisibility(accountKey, it)
                },
                enabled = settings != null && !posting,
                label = stringResource(R.string.memo_comment_visibility, audience),
            )
            Text(
                audience,
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ActionIconButton(stringResource(R.string.cancel), onClick = onClose, enabled = !posting) {
                Icon(Icons.Outlined.Close, contentDescription = null)
            }
            ActionIconButton(
                label = stringResource(R.string.memo_comment_post),
                enabled = settings != null && text.text.isNotBlank() && !posting,
                onClick = {
                    if (!posting) {
                        posting = true
                        failed = false
                        scope.launch {
                            val result = viewModel.postMemoComment(accountKey, parent, text.text, visibility, space)
                            posting = false
                            if (result is ApiResponse.Success) onClose() else failed = true
                        }
                    }
                },
            ) {
                if (posting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            }
        }
    }
}
