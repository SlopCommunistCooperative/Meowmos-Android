package io.github.slopcommunistcooperative.meowmos.ui.page.memos

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.slopcommunistcooperative.meowmos.ui.util.edgeToEdgeContentPadding
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.ui.component.MeowmosEmptyState
import io.github.slopcommunistcooperative.meowmos.ui.component.ArchivedMemoCard
import io.github.slopcommunistcooperative.meowmos.viewmodel.ArchivedMemoListViewModel
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalArchivedMemos

@Composable
fun ArchivedMemoList(
    viewModel: ArchivedMemoListViewModel = hiltViewModel(),
    contentPadding: PaddingValues
) {
    val listContentPadding = edgeToEdgeContentPadding(contentPadding)

    CompositionLocalProvider(LocalArchivedMemos provides viewModel) {
        LazyColumn(
            modifier = Modifier.consumeWindowInsets(contentPadding),
            contentPadding = listContentPadding
        ) {
            if (viewModel.memos.isEmpty()) {
                item(key = "empty") {
                    MeowmosEmptyState(
                        icon = Icons.Outlined.Inventory2,
                        title = stringResource(R.string.no_archived_memos),
                    )
                }
            }
            items(viewModel.memos, key = { it.identifier }) { memo ->
                ArchivedMemoCard(memo)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadMemos()
    }
}