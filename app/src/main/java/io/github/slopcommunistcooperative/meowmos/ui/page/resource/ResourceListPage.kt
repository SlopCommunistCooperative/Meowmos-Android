package io.github.slopcommunistcooperative.meowmos.ui.page.resource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.ext.popBackStackIfLifecycleIsResumed
import io.github.slopcommunistcooperative.meowmos.ui.component.ActionIconButton
import io.github.slopcommunistcooperative.meowmos.ui.component.Attachment
import io.github.slopcommunistcooperative.meowmos.ui.component.MemoImage
import io.github.slopcommunistcooperative.meowmos.ui.component.MeowmosEmptyState
import io.github.slopcommunistcooperative.meowmos.ui.theme.MeowmosTheme
import io.github.slopcommunistcooperative.meowmos.viewmodel.ResourceListViewModel
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredGridItems

private enum class ResourceFilter {
    IMAGE,
    OTHER,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceListPage(
    navController: NavHostController,
    showBackButton: Boolean = true,
    viewModel: ResourceListViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var selectedFilter by rememberSaveable { mutableStateOf(ResourceFilter.IMAGE) }
    val imageResources = viewModel.resources.filter { it.mimeType?.startsWith("image/") == true }
    val otherResources = viewModel.resources.filterNot { it.mimeType?.startsWith("image/") == true }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.resources),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
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
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .padding(innerPadding),
        ) {
            val segmentColors = SegmentedButtonDefaults.colors(
                activeContainerColor = MeowmosTheme.colors.wayfindingContainer,
                activeContentColor = MeowmosTheme.colors.onWayfindingContainer,
                inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    selected = selectedFilter == ResourceFilter.IMAGE,
                    onClick = { selectedFilter = ResourceFilter.IMAGE },
                    colors = segmentColors,
                    label = { Text("${stringResource(R.string.image)} (${imageResources.size})") },
                )
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    selected = selectedFilter == ResourceFilter.OTHER,
                    onClick = { selectedFilter = ResourceFilter.OTHER },
                    colors = segmentColors,
                    label = { Text("${stringResource(R.string.other)} (${otherResources.size})") },
                )
            }

            if (selectedFilter == ResourceFilter.IMAGE) {
                if (imageResources.isEmpty()) {
                    MeowmosEmptyState(
                        icon = Icons.Outlined.PhotoLibrary,
                        title = stringResource(R.string.no_resources),
                    )
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(minSize = 156.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalItemSpacing = 10.dp,
                        contentPadding = PaddingValues(bottom = 20.dp),
                    ) {
                        staggeredGridItems(imageResources, key = { it.identifier }) { resource ->
                            MemoImage(
                                url = resource.localUri ?: resource.uri,
                                resourceIdentifier = resource.identifier,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium),
                            )
                        }
                    }
                }
            } else {
                if (otherResources.isEmpty()) {
                    MeowmosEmptyState(
                        icon = Icons.Outlined.Attachment,
                        title = stringResource(R.string.no_resources),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 20.dp),
                    ) {
                        lazyItems(otherResources, key = { it.identifier }) { resource ->
                            Attachment(resource = resource)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadResources()
    }
}
