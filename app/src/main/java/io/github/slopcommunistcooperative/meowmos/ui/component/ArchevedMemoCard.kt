package io.github.slopcommunistcooperative.meowmos.ui.component

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skydoves.sandwich.suspendOnSuccess
import kotlinx.coroutines.launch
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.local.entity.MemoEntity
import io.github.slopcommunistcooperative.meowmos.ext.string
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalArchivedMemos
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalMemos

@Composable
fun ArchivedMemoCard(
    memo: MemoEntity
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = meowmosCardColors(),
        border = meowmosCardBorder(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    DateUtils.getRelativeTimeSpanString(memo.date.toEpochMilli(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                ArchivedMemosCardActionButton(memo)
            }

            MemoContent(memo, previewMode = false)
        }
    }
}

@Composable
fun ArchivedMemosCardActionButton(
    memo: MemoEntity
) {
    val scope = rememberCoroutineScope()
    val archivedMemoListViewModel = LocalArchivedMemos.current
    val memosViewModel = LocalMemos.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    MemoOverflowMenu(onDelete = { showDeleteDialog = true }) {
        listOf(MemoMenuAction(R.string.restore, R.drawable.symbol_unarchive) {
            scope.launch {
                archivedMemoListViewModel.restoreMemo(memo.identifier).suspendOnSuccess {
                    memosViewModel.loadMemos()
                }
            }
        })
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(R.string.delete_this_memo.string) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            archivedMemoListViewModel.deleteMemo(memo.identifier).suspendOnSuccess {
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
