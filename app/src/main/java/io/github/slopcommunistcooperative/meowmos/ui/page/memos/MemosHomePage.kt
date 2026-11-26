package io.github.slopcommunistcooperative.meowmos.ui.page.memos

import android.net.Uri
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.ext.string
import io.github.slopcommunistcooperative.meowmos.ui.component.ActionIconButton
import io.github.slopcommunistcooperative.meowmos.ui.component.SyncStatusBadge
import io.github.slopcommunistcooperative.meowmos.ui.page.common.LocalRootNavController
import io.github.slopcommunistcooperative.meowmos.ui.page.common.RouteName
import io.github.slopcommunistcooperative.meowmos.ui.theme.MeowmosTheme
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalMemos
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState
import io.github.slopcommunistcooperative.meowmos.viewmodel.ManualSyncResult
import kotlinx.coroutines.launch

@Composable
fun MemosHomePage(
    showComposeFab: Boolean = true,
) {
    MemoBrowser { onMemoClick ->
        MemosHomePageContent(
            showComposeFab = showComposeFab,
            onMemoClick = onMemoClick,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemosHomePageContent(
    showComposeFab: Boolean,
    onMemoClick: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val rootNavController = LocalRootNavController.current
    val memosViewModel = LocalMemos.current
    val userStateViewModel = LocalUserState.current
    val currentAccount by userStateViewModel.currentAccount.collectAsStateWithLifecycle()
    val syncStatus by memosViewModel.syncStatus.collectAsStateWithLifecycle()

    val expandedFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }
    var syncAlert by remember { mutableStateOf<HomeSyncAlert?>(null) }

    suspend fun requestManualSync(allowHigherV1Version: String? = null) {
        when (val result = memosViewModel.refreshMemos(allowHigherV1Version)) {
            ManualSyncResult.Completed -> Unit
            is ManualSyncResult.Blocked -> {
                syncAlert = HomeSyncAlert.Blocked(result.message)
            }
            is ManualSyncResult.RequiresConfirmation -> {
                syncAlert = HomeSyncAlert.RequiresConfirmation(result.version, result.message)
            }
            is ManualSyncResult.Failed -> {
                syncAlert = HomeSyncAlert.Failed(result.message)
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.memos),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    if (currentAccount !is Account.Local) {
                        SyncStatusBadge(
                            syncing = syncStatus.syncing,
                            unsyncedCount = syncStatus.unsyncedCount,
                            onSync = {
                                scope.launch {
                                    requestManualSync()
                                }
                            },
                        )
                    }
                    ActionIconButton(
                        label = stringResource(R.string.search),
                        onClick = {
                            rootNavController.navigate(RouteName.SEARCH)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        floatingActionButton = {
            if (showComposeFab) {
                ExtendedFloatingActionButton(
                    onClick = {
                        rootNavController.navigate(RouteName.INPUT)
                    },
                    expanded = expandedFab,
                    shape = MaterialTheme.shapes.large,
                    containerColor = MeowmosTheme.colors.wayfindingContainer,
                    contentColor = MeowmosTheme.colors.onWayfindingContainer,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp,
                    ),
                    text = { Text(stringResource(R.string.new_memo)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.compose),
                        )
                    },
                )
            }
        },
        content = { innerPadding ->
            MemosList(
                onMemoClick = onMemoClick,
                lazyListState = listState,
                contentPadding = innerPadding,
                additionalBottomPadding = if (showComposeFab) MemoListFabAvoidancePadding else 20.dp,
                onRefresh = { requestManualSync() },
                showComments = true,
                onTagClick = { tag ->
                    rootNavController.navigate("${RouteName.TAG}/${Uri.encode(tag)}") {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    )

    when (val alert = syncAlert) {
        null -> Unit
        is HomeSyncAlert.Blocked -> {
            AlertDialog(
                onDismissRequest = { syncAlert = null },
                title = { Text(R.string.unsupported_memos_version_title.string) },
                text = { Text(alert.message) },
                confirmButton = {
                    TextButton(onClick = { syncAlert = null }) {
                        Text(R.string.close.string)
                    }
                },
            )
        }
        is HomeSyncAlert.RequiresConfirmation -> {
            AlertDialog(
                onDismissRequest = { syncAlert = null },
                title = { Text(R.string.unsupported_memos_version_title.string) },
                text = { Text(alert.message) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            syncAlert = null
                            scope.launch {
                                requestManualSync(allowHigherV1Version = alert.version)
                            }
                        },
                    ) {
                        Text(R.string.still_sync.string)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { syncAlert = null }) {
                        Text(R.string.cancel.string)
                    }
                },
            )
        }
        is HomeSyncAlert.Failed -> {
            AlertDialog(
                onDismissRequest = { syncAlert = null },
                title = { Text(R.string.sync_failed.string) },
                text = { Text(alert.message) },
                confirmButton = {
                    TextButton(onClick = { syncAlert = null }) {
                        Text(R.string.close.string)
                    }
                },
            )
        }
    }
}

private val MemoListFabAvoidancePadding = 96.dp

private sealed class HomeSyncAlert {
    data class Blocked(val message: String) : HomeSyncAlert()
    data class RequiresConfirmation(val version: String, val message: String) : HomeSyncAlert()
    data class Failed(val message: String) : HomeSyncAlert()
}
