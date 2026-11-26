package io.github.slopcommunistcooperative.meowmos.ui.page.memos

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
import io.github.slopcommunistcooperative.meowmos.ui.page.memoinput.EditorSendButton
import io.github.slopcommunistcooperative.meowmos.ui.page.memoinput.MemoEditorTextField
import io.github.slopcommunistcooperative.meowmos.ui.page.memoinput.MemoVisibilityPicker
import io.github.slopcommunistcooperative.meowmos.viewmodel.COMMENT_PREVIEW_SIZE
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalMemos
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val THREAD_PAGE_SIZE = 20

private data class CommentPageRequest(val token: String? = null, val retry: Int = 0)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MemoComments(
    memo: MemoRepresentable,
    accountKey: String,
    space: String?,
    onViewAll: (() -> Unit)? = null,
) {
    val account by LocalUserState.current.currentAccount.collectAsStateWithLifecycle()
    val parent = memo.remoteId ?: return
    if (account == null || account is Account.Local || account?.accountKey() != accountKey) return

    key(accountKey, parent) {
        val viewModel = LocalMemos.current
        val revision = viewModel.commentRevision(accountKey, parent)
        val preview = onViewAll != null
        val refresh = if (preview) viewModel.commentPreviews.generation else 0
        val pageSize = if (preview) COMMENT_PREVIEW_SIZE else THREAD_PAGE_SIZE
        var request by remember(revision, refresh, pageSize) { mutableStateOf(CommentPageRequest()) }
        var response by remember(revision, refresh, pageSize, request) {
            mutableStateOf<ApiResponse<Pair<List<Memo>, String?>>?>(null)
        }
        var comments by remember {
            mutableStateOf(if (preview) viewModel.commentPreviews.peek(accountKey, parent) else emptyList())
        }
        var editing by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(revision, refresh, pageSize, request) {
            val result = if (preview) {
                viewModel.commentPreviews.load(accountKey, parent)
            } else {
                viewModel.listMemoComments(accountKey, parent, pageSize, request.token)
            }
            if (result is ApiResponse.Success) {
                comments = if (request.token == null) result.data.first else comments + result.data.first
            }
            response = result
        }
        val nextPage = (response as? ApiResponse.Success)?.data?.second

        Column(Modifier.fillMaxWidth().padding(top = if (preview) 4.dp else 16.dp)) {
            if (preview) {
                comments.take(COMMENT_PREVIEW_SIZE).forEach { comment -> CommentPreview(comment) }
                FlowRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    if (!memo.archived && !editing) {
                        TextButton(
                            onClick = { editing = true },
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.symbol_chat_bubble),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.memo_comment_reply))
                        }
                    }
                    if (comments.isNotEmpty()) {
                        TextButton(onClick = onViewAll, shapes = ButtonDefaults.shapes()) {
                            Text(stringResource(R.string.memo_comment_view_all))
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.memo_comments),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                    )
                    if (!memo.archived && !editing) {
                        FilledTonalButton(onClick = { editing = true }, shapes = ButtonDefaults.shapes()) {
                            Text(stringResource(R.string.memo_comment_write))
                        }
                    }
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
                    response == null -> Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LoadingIndicator(modifier = Modifier.size(24.dp))
                        Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodySmall)
                    }
                    response is ApiResponse.Failure -> Row(
                        Modifier.padding(horizontal = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.memo_comment_load_failed), Modifier.weight(1f))
                        TextButton(
                            onClick = { request = request.copy(retry = request.retry + 1) },
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                    nextPage != null -> TextButton(
                        onClick = { request = CommentPageRequest(nextPage) },
                        shapes = ButtonDefaults.shapes(),
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
    val author = comment.creator?.name?.takeIf { it.isNotBlank() }
    val time = DateUtils.getRelativeTimeSpanString(
        comment.date.toEpochMilli(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS,
    ).toString()
    ListItem(
        trailingContent = author?.let {
            { Text(time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
    ) {
        Text(
            author ?: time,
            style = if (author == null) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLargeEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
            errorMessage = if (failed) stringResource(R.string.memo_comment_post_failed) else null,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f)) {
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
                    showLabel = true,
                )
            }
            ActionIconButton(stringResource(R.string.cancel), onClick = onClose, enabled = !posting) {
                Icon(ImageVector.vectorResource(R.drawable.symbol_close), contentDescription = null)
            }
            EditorSendButton(
                label = stringResource(R.string.memo_comment_post),
                enabled = settings != null && text.text.isNotBlank() && !posting,
                posting = posting,
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
            )
        }
    }
}
