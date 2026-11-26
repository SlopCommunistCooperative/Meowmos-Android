package io.github.slopcommunistcooperative.meowmos.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skydoves.sandwich.suspendOnSuccess
import kotlinx.coroutines.launch
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.local.entity.MemoEntity
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.data.model.MemoEditGesture
import io.github.slopcommunistcooperative.meowmos.ext.icon
import io.github.slopcommunistcooperative.meowmos.ext.navigateToMemoEditor
import io.github.slopcommunistcooperative.meowmos.ext.string
import io.github.slopcommunistcooperative.meowmos.ext.titleResource
import io.github.slopcommunistcooperative.meowmos.ui.page.common.LocalRootNavController
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalMemos
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState

@Composable
fun MemosCard(
    memo: MemoEntity,
    onClick: (MemoEntity) -> Unit,
    editGesture: MemoEditGesture = MemoEditGesture.NONE,
    previewMode: Boolean = false,
    showSyncStatus: Boolean = false,
    onTagClick: ((String) -> Unit)? = null,
    footer: @Composable () -> Unit = {},
) {
    val memosViewModel = LocalMemos.current
    val rootNavController = LocalRootNavController.current
    val scope = rememberCoroutineScope()

    val cardModifier = Modifier
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                if (editGesture == MemoEditGesture.SINGLE) {
                    rootNavController.navigateToMemoEditor(memo.identifier)
                } else {
                    onClick(memo)
                }
            },
            onLongClick = if (editGesture == MemoEditGesture.LONG) {
                {
                    rootNavController.navigateToMemoEditor(memo.identifier)
                }
            } else {
                null
            },
            onDoubleClick = if (editGesture == MemoEditGesture.DOUBLE) {
                {
                    rootNavController.navigateToMemoEditor(memo.identifier)
                }
            } else {
                null
            }
        )

    val spaceLabel = memosViewModel.spaceTitle(memo.space)

    Card(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.medium,
        colors = meowmosCardColors(),
        border = meowmosCardBorder(pinned = memo.pinned),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        DateUtils.getRelativeTimeSpanString(
                            memo.date.toEpochMilli(),
                            System.currentTimeMillis(),
                            DateUtils.SECOND_IN_MILLIS
                        ).toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (memo.pinned) {
                        WayfindingBadge(
                            text = stringResource(R.string.pinned),
                            icon = ImageVector.vectorResource(R.drawable.symbol_push_pin),
                            tone = WayfindingTone.Signal,
                        )
                    }
                    if (spaceLabel != null) {
                        WayfindingBadge(
                            text = spaceLabel,
                            tone = WayfindingTone.Beacon,
                            modifier = Modifier.widthIn(max = 140.dp),
                        )
                    }
                    if (showSyncStatus && memo.needsSync) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.symbol_cloud_off),
                            contentDescription = R.string.memo_sync_pending.string,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (LocalUserState.current.currentUser?.defaultVisibility != memo.visibility) {
                        Icon(
                            memo.visibility.icon,
                            contentDescription = stringResource(memo.visibility.titleResource),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                MemosCardActionButton(memo)
            }
            MemoContent(
                memo,
                previewMode = previewMode,
                checkboxChange = { checked, startOffset, endOffset ->
                    scope.launch {
                        var text = memo.content.substring(startOffset, endOffset)
                        text = if (checked) {
                            text.replace("[ ]", "[x]")
                        } else {
                            text.replace("[x]", "[ ]")
                        }
                        memosViewModel.editMemo(
                            memo.identifier,
                            memo.content.replaceRange(startOffset, endOffset, text),
                            memo.resources,
                            memo.visibility
                        )
                    }
                },
                onViewMore = {
                    onClick(memo)
                },
                onTagClick = onTagClick
            )
            footer()
        }
    }
}

@Composable
fun MemosCardActionButton(
    memo: MemoEntity,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(ClipboardManager::class.java)
    val memosViewModel = LocalMemos.current
    val userStateViewModel = LocalUserState.current
    val currentAccount by userStateViewModel.currentAccount.collectAsStateWithLifecycle()
    val rootNavController = LocalRootNavController.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val memoLabel = stringResource(R.string.memo)

    MemoOverflowMenu(onDelete = { showDeleteDialog = true }) {
        buildList {
            add(
                MemoMenuAction(
                    if (memo.pinned) R.string.unpin else R.string.pin,
                    if (memo.pinned) R.drawable.symbol_pin_drop else R.drawable.symbol_push_pin,
                ) {
                    scope.launch { memosViewModel.updateMemoPinned(memo.identifier, !memo.pinned) }
                }
            )
            add(MemoMenuAction(R.string.edit, R.drawable.symbol_edit) {
                rootNavController.navigateToMemoEditor(memo.identifier)
            })
            add(MemoMenuAction(R.string.share, R.drawable.symbol_share) {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, memo.content)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, null))
            })
            add(MemoMenuAction(R.string.copy, R.drawable.symbol_content_copy) {
                clipboardManager?.setPrimaryClip(ClipData.newPlainText(memoLabel, memo.content))
            })
            if (currentAccount !is Account.Local) {
                add(MemoMenuAction(R.string.copy_link, R.drawable.symbol_link) {
                    memosViewModel.host.value?.let { host ->
                        val memoUrl = "$host/${memo.remoteId ?: memo.identifier}"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, memoUrl)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }
                })
            }
            add(MemoMenuAction(R.string.archive, R.drawable.symbol_archive) {
                scope.launch { memosViewModel.archiveMemo(memo.identifier) }
            })
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(R.string.delete_this_memo.string) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            memosViewModel.deleteMemo(memo.identifier).suspendOnSuccess {
                                showDeleteDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(R.string.confirm.string)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text(R.string.cancel.string)
                }
            }
        )
    }
}
