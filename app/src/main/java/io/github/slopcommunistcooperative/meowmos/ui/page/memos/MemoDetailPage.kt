package io.github.slopcommunistcooperative.meowmos.ui.page.memos

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import io.github.slopcommunistcooperative.meowmos.ui.component.ActionIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.ext.icon
import io.github.slopcommunistcooperative.meowmos.ext.popBackStackIfLifecycleIsResumed
import io.github.slopcommunistcooperative.meowmos.ext.string
import io.github.slopcommunistcooperative.meowmos.ext.titleResource
import io.github.slopcommunistcooperative.meowmos.ui.component.MemoContent
import io.github.slopcommunistcooperative.meowmos.ui.component.MemosCardActionButton
import io.github.slopcommunistcooperative.meowmos.ui.component.MeowmosEmptyState
import io.github.slopcommunistcooperative.meowmos.ui.component.WayfindingBadge
import io.github.slopcommunistcooperative.meowmos.ui.component.WayfindingTone
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalMemos
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoDetailPage(
    navController: NavHostController,
    memoIdentifier: String
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    MemoDetailContent(memoIdentifier) {
        navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemoDetailContent(memoIdentifier: String, onBack: () -> Unit) {
    val layoutDirection = LocalLayoutDirection.current
    val memosViewModel = LocalMemos.current
    val userStateViewModel = LocalUserState.current
    val currentAccount by userStateViewModel.currentAccount.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val memo = remember(memosViewModel.memos.toList(), memoIdentifier) {
        memosViewModel.memos.firstOrNull { it.identifier == memoIdentifier }
    }
    var hadMemo by rememberSaveable(memoIdentifier) { mutableStateOf(false) }

    LaunchedEffect(memo?.identifier) {
        when {
            memo != null -> hadMemo = true
            hadMemo -> onBack()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val spaceLabel = memosViewModel.spaceTitle(memo?.space)

    Scaffold(
        modifier = Modifier
            .imePadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = R.string.memo.string,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    ActionIconButton(label = R.string.back.string, onClick = onBack) {
                        Icon(ImageVector.vectorResource(R.drawable.symbol_arrow_back), contentDescription = R.string.back.string)
                    }
                },
                actions = {
                    memo?.let { MemosCardActionButton(it) }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        }
    ) { innerPadding ->
        if (memo == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                MeowmosEmptyState(
                    icon = ImageVector.vectorResource(R.drawable.symbol_sticky_note_2),
                    title = R.string.memo_not_found.string,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .padding(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(layoutDirection)
                )
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 6.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        modifier = Modifier.widthIn(max = 160.dp),
                    )
                }
                if (currentAccount !is Account.Local && memo.needsSync) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.symbol_cloud_off),
                        contentDescription = R.string.memo_sync_pending.string,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                if (userStateViewModel.currentUser?.defaultVisibility != memo.visibility) {
                    Icon(
                        imageVector = memo.visibility.icon,
                        contentDescription = stringResource(memo.visibility.titleResource),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            MemoContent(
                memo = memo,
                selectable = true,
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
                }
            )

            MemoComments(memo, memo.accountKey, memo.space)

            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}
