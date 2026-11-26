package io.github.slopcommunistcooperative.meowmos.ui.page.memos

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.local.entity.MemoEntity
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.data.model.MemoEditGesture
import io.github.slopcommunistcooperative.meowmos.data.model.Settings
import io.github.slopcommunistcooperative.meowmos.ext.settingsDataStore
import io.github.slopcommunistcooperative.meowmos.ext.string
import io.github.slopcommunistcooperative.meowmos.ui.component.ArchivedMemoCard
import io.github.slopcommunistcooperative.meowmos.ui.component.MeowmosEmptyState
import io.github.slopcommunistcooperative.meowmos.ui.component.WayfindingSectionHeader
import io.github.slopcommunistcooperative.meowmos.ui.component.MemosCard
import io.github.slopcommunistcooperative.meowmos.ui.page.common.LocalRootNavController
import io.github.slopcommunistcooperative.meowmos.ui.util.edgeToEdgeContentPadding
import io.github.slopcommunistcooperative.meowmos.ui.page.common.RouteName
import io.github.slopcommunistcooperative.meowmos.util.hasCustomTag
import io.github.slopcommunistcooperative.meowmos.util.memoMatchesSearch
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalMemos
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState
import io.github.slopcommunistcooperative.meowmos.viewmodel.ManualSyncResult
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemosList(
    contentPadding: PaddingValues,
    lazyListState: LazyListState = rememberLazyListState(),
    tag: String? = null,
    searchString: String? = null,
    additionalBottomPadding: Dp = 16.dp,
    onRefresh: (suspend () -> Unit)? = null,
    onTagClick: ((String) -> Unit)? = null,
    onMemoClick: ((String) -> Unit)? = null,
    archivedResults: List<MemoEntity> = emptyList(),
    showComments: Boolean = false,
    headerContent: (LazyListScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val navController = LocalRootNavController.current
    val viewModel = LocalMemos.current
    val userStateViewModel = LocalUserState.current
    val currentAccount by userStateViewModel.currentAccount.collectAsStateWithLifecycle()
    val settings by context.settingsDataStore.data.collectAsStateWithLifecycle(initialValue = Settings())
    val editGesture = settings.usersList
        .firstOrNull { it.accountKey == settings.currentUser }
        ?.settings
        ?.editGesture
    val refreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var commentRefresh by remember { mutableIntStateOf(0) }
    var syncAlert by remember { mutableStateOf<PullRefreshSyncAlert?>(null) }
    val filteredMemos = remember(viewModel.memos.toList(), tag, searchString) {
        val pinned = viewModel.memos.filter { it.pinned }
        val nonPinned = viewModel.memos.filter { !it.pinned }
        var fullList = pinned + nonPinned

        tag?.let { tag ->
            fullList = fullList.filter { memo ->
                hasCustomTag(memo.content, tag)
            }
        }

        searchString?.let { searchString ->
            if (searchString.isNotEmpty()) {
                fullList = fullList.filter { memo ->
                    memoMatchesSearch(memo.content, searchString)
                }
            }
        }

        fullList
    }
    var listTopId: String? by rememberSaveable {
        mutableStateOf(null)
    }
    val listContentPadding = edgeToEdgeContentPadding(
        contentPadding,
        additionalBottomPadding
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                if (onRefresh != null) {
                    onRefresh()
                } else {
                    when (val result = viewModel.refreshMemos()) {
                        ManualSyncResult.Completed -> Unit
                        is ManualSyncResult.Blocked -> {
                            syncAlert = PullRefreshSyncAlert.Blocked(result.message)
                        }
                        is ManualSyncResult.RequiresConfirmation -> {
                            syncAlert = PullRefreshSyncAlert.RequiresConfirmation(result.version, result.message)
                        }
                        is ManualSyncResult.Failed -> {
                            syncAlert = PullRefreshSyncAlert.Failed(result.message)
                        }
                    }
                }
                if (showComments) commentRefresh++
                isRefreshing = false
            }
        },
        state = refreshState,
        modifier = if (showComments) Modifier.fillMaxSize().imePadding() else Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(contentPadding),
            state = lazyListState,
            contentPadding = listContentPadding
        ) {
            headerContent?.invoke(this)
            if (filteredMemos.isEmpty() && archivedResults.isEmpty()) {
                item(key = "empty") {
                    MeowmosEmptyState(
                        icon = Icons.AutoMirrored.Outlined.StickyNote2,
                        title = stringResource(R.string.no_memos),
                    )
                }
            }
            items(filteredMemos, key = { it.identifier }) { memo ->
                val openMemo = {
                    if (onMemoClick != null) {
                        onMemoClick(memo.identifier)
                    } else navController.navigate(
                        "${RouteName.MEMO_DETAIL}?memoId=${Uri.encode(memo.identifier)}"
                    )
                }
                MemosCard(
                    memo = memo,
                    onClick = { openMemo() },
                    editGesture = editGesture ?: MemoEditGesture.NONE,
                    previewMode = true,
                    showSyncStatus = currentAccount !is Account.Local,
                    onTagClick = onTagClick,
                    footer = {
                        if (showComments) MemoComments(
                            memo, memo.accountKey, memo.space, onViewAll = openMemo, refresh = commentRefresh,
                        )
                    },
                )
            }
            if (archivedResults.isNotEmpty()) {
                item(key = "archived-header") {
                    WayfindingSectionHeader(
                        title = stringResource(R.string.archived),
                    )
                }
                items(archivedResults, key = { "archived-${it.identifier}" }) { memo ->
                    ArchivedMemoCard(memo)
                }
            }
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Timber.d(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadMemos()
    }

    LaunchedEffect(filteredMemos.firstOrNull()?.identifier) {
        if (listTopId != null && filteredMemos.isNotEmpty() && listTopId != filteredMemos.first().identifier) {
            lazyListState.scrollToItem(0)
        }

        listTopId = filteredMemos.firstOrNull()?.identifier
    }

    when (val alert = syncAlert) {
        null -> Unit
        is PullRefreshSyncAlert.Blocked -> {
            AlertDialog(
                onDismissRequest = { syncAlert = null },
                title = { Text(R.string.unsupported_memos_version_title.string) },
                text = { Text(alert.message) },
                confirmButton = {
                    TextButton(onClick = { syncAlert = null }) {
                        Text(R.string.close.string)
                    }
                }
            )
        }
        is PullRefreshSyncAlert.RequiresConfirmation -> {
            AlertDialog(
                onDismissRequest = { syncAlert = null },
                title = { Text(R.string.unsupported_memos_version_title.string) },
                text = { Text(alert.message) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            syncAlert = null
                            scope.launch {
                                when (val result = viewModel.refreshMemos(alert.version)) {
                                    ManualSyncResult.Completed -> Unit
                                    is ManualSyncResult.Blocked -> {
                                        syncAlert = PullRefreshSyncAlert.Blocked(result.message)
                                    }
                                    is ManualSyncResult.RequiresConfirmation -> {
                                        syncAlert = PullRefreshSyncAlert.RequiresConfirmation(result.version, result.message)
                                    }
                                    is ManualSyncResult.Failed -> {
                                        syncAlert = PullRefreshSyncAlert.Failed(result.message)
                                    }
                                }
                            }
                        }
                    ) {
                        Text(R.string.still_sync.string)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { syncAlert = null }) {
                        Text(R.string.cancel.string)
                    }
                }
            )
        }
        is PullRefreshSyncAlert.Failed -> {
            AlertDialog(
                onDismissRequest = { syncAlert = null },
                title = { Text(R.string.sync_failed.string) },
                text = { Text(alert.message) },
                confirmButton = {
                    TextButton(onClick = { syncAlert = null }) {
                        Text(R.string.close.string)
                    }
                }
            )
        }
    }
}

private sealed class PullRefreshSyncAlert {
    data class Blocked(val message: String) : PullRefreshSyncAlert()
    data class RequiresConfirmation(val version: String, val message: String) : PullRefreshSyncAlert()
    data class Failed(val message: String) : PullRefreshSyncAlert()
}
