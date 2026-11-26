package io.github.slopcommunistcooperative.meowmos.ui.page.memos

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.ext.popBackStackIfLifecycleIsResumed
import io.github.slopcommunistcooperative.meowmos.ui.component.ActionIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedMemoPage(
    navController: NavHostController,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.archived),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    ActionIconButton(
                        label = stringResource(R.string.back),
                        onClick = {
                            navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                        },
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.symbol_arrow_back),
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
            ArchivedMemoList(
                contentPadding = innerPadding,
            )
        },
    )
}
