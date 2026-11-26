package io.github.slopcommunistcooperative.meowmos.ui.page.memos

import android.net.Uri
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MemosHomePageContent(
    showComposeFab: Boolean,
    onMemoClick: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val rootNavController = LocalRootNavController.current
    val memosViewModel = LocalMemos.current
    val userStateViewModel = LocalUserState.current
    val currentAccount by userStateViewModel.currentAccount.collectAsStateWithLifecycle()
    val syncStatus by memosViewModel.syncStatus.collectAsStateWithLifecycle()
    val initialLoadComplete by memosViewModel.initialLoadComplete.collectAsStateWithLifecycle()
    val waitingForFirstMemos = !initialLoadComplete ||
        (currentAccount != null && currentAccount !is Account.Local &&
            (memosViewModel.initialSyncCompletedFor != currentAccount?.accountKey() || syncStatus.syncing))

    val expandedFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }
    val snackbar = remember { SnackbarHostState() }
    val retryLabel = stringResource(R.string.retry)
    var syncAlert by remember { mutableStateOf<ManualSyncResult?>(null) }
    var syncSource by remember { mutableStateOf<SyncSource?>(null) }
    val remoteAccount = currentAccount != null && currentAccount !is Account.Local
    val syncInProgress = syncSource != null || syncStatus.syncing

    suspend fun requestManualSync(source: SyncSource, allowHigherV1Version: String? = null) {
        if (syncSource != null || memosViewModel.syncStatus.value.syncing) return
        syncSource = source
        val result = try {
            memosViewModel.refreshMemos(allowHigherV1Version)
        } finally {
            syncSource = null
        }
        when (result) {
            ManualSyncResult.Completed -> Unit
            is ManualSyncResult.Blocked, is ManualSyncResult.RequiresConfirmation -> syncAlert = result
            is ManualSyncResult.Failed -> scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val action = snackbar.showSnackbar(result.message, retryLabel, duration = SnackbarDuration.Long)
                if (action == SnackbarResult.ActionPerformed) requestManualSync(SyncSource.AppBar, allowHigherV1Version)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.memos)) },
                actions = {
                    if (remoteAccount) {
                        SyncStatusBadge(
                            syncing = syncInProgress && syncSource != SyncSource.Feed,
                            enabled = !syncInProgress,
                            unsyncedCount = syncStatus.unsyncedCount,
                            onSync = { scope.launch { requestManualSync(SyncSource.AppBar) } },
                        )
                    }
                    ActionIconButton(
                        label = stringResource(R.string.search),
                        onClick = { rootNavController.navigate(RouteName.SEARCH) },
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.symbol_search), contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            if (showComposeFab) {
                ExtendedFloatingActionButton(
                    onClick = { rootNavController.navigate(RouteName.INPUT) },
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
                            ImageVector.vectorResource(R.drawable.symbol_add),
                            contentDescription = stringResource(R.string.compose),
                        )
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        content = { innerPadding ->
            MemosList(
                onMemoClick = onMemoClick,
                lazyListState = listState,
                contentPadding = innerPadding,
                additionalBottomPadding = if (showComposeFab) MemoListFabAvoidancePadding else 20.dp,
                onRefresh = { requestManualSync(SyncSource.Feed) },
                refreshEnabled = remoteAccount && !syncInProgress,
                showComments = true,
                showInitialPlaceholder = waitingForFirstMemos,
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
        null, ManualSyncResult.Completed, is ManualSyncResult.Failed -> Unit
        is ManualSyncResult.Blocked -> {
            AlertDialog(
                onDismissRequest = { syncAlert = null },
                title = { Text(R.string.unsupported_memos_version_title.string) },
                text = { Text(alert.message) },
                confirmButton = {
                    TextButton(onClick = { syncAlert = null }, shapes = ButtonDefaults.shapes()) {
                        Text(R.string.close.string)
                    }
                },
            )
        }
        is ManualSyncResult.RequiresConfirmation -> {
            AlertDialog(
                onDismissRequest = { syncAlert = null },
                title = { Text(R.string.unsupported_memos_version_title.string) },
                text = { Text(alert.message) },
                confirmButton = {
                    TextButton(
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
                            syncAlert = null
                            scope.launch {
                                requestManualSync(SyncSource.AppBar, allowHigherV1Version = alert.version)
                            }
                        },
                    ) {
                        Text(R.string.still_sync.string)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { syncAlert = null }, shapes = ButtonDefaults.shapes()) {
                        Text(R.string.cancel.string)
                    }
                },
            )
        }
    }
}

private val MemoListFabAvoidancePadding = 96.dp

private enum class SyncSource { AppBar, Feed }
