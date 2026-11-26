package io.github.slopcommunistcooperative.meowmos.ui.page.memos

import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.ext.popBackStackIfLifecycleIsResumed
import io.github.slopcommunistcooperative.meowmos.ui.component.ActionIconButton
import io.github.slopcommunistcooperative.meowmos.ui.component.WayfindingBadge
import io.github.slopcommunistcooperative.meowmos.ui.component.WayfindingTone
import io.github.slopcommunistcooperative.meowmos.ui.page.common.RouteName

@Composable
fun TagMemoPage(
    tag: String,
    navController: NavHostController,
) {
    MemoBrowser { onMemoClick ->
        TagMemoPageContent(
            tag = tag,
            navController = navController,
            onMemoClick = onMemoClick,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagMemoPageContent(
    tag: String,
    navController: NavHostController,
    onMemoClick: (String) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val normalizedCurrentTag = remember(tag) { normalizeTag(tag) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WayfindingBadge(
                            text = normalizedCurrentTag,
                            icon = Icons.Outlined.Tag,
                            tone = WayfindingTone.Signal,
                        )
                    }
                },
                navigationIcon = {
                    ActionIconButton(
                        label = stringResource(R.string.back),
                        onClick = {
                            navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
        content = { innerPadding ->
            MemosList(
                onMemoClick = onMemoClick,
                contentPadding = innerPadding,
                tag = tag,
                onTagClick = { clickedTag ->
                    if (normalizeTag(clickedTag) == normalizedCurrentTag) {
                        return@MemosList
                    }
                    navController.navigate("${RouteName.TAG}/${Uri.encode(clickedTag)}") {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    )
}

private fun normalizeTag(tag: String): String {
    return tag.removePrefix("#")
}
