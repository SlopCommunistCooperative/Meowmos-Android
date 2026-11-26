package io.github.slopcommunistcooperative.meowmos.ui.page.memos

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.model.Settings
import io.github.slopcommunistcooperative.meowmos.ext.popBackStackIfLifecycleIsResumed
import io.github.slopcommunistcooperative.meowmos.ext.settingsDataStore
import io.github.slopcommunistcooperative.meowmos.ui.component.ActionIconButton
import io.github.slopcommunistcooperative.meowmos.ui.component.WayfindingBadge
import io.github.slopcommunistcooperative.meowmos.ui.component.WayfindingTone
import io.github.slopcommunistcooperative.meowmos.ui.component.meowmosCardBorder
import io.github.slopcommunistcooperative.meowmos.ui.component.meowmosCardColors
import io.github.slopcommunistcooperative.meowmos.ui.page.common.RouteName
import io.github.slopcommunistcooperative.meowmos.util.archivedSearchResults
import io.github.slopcommunistcooperative.meowmos.viewmodel.ArchivedMemoListViewModel
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalArchivedMemos
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalMemos
import androidx.navigation.NavHostController

@Composable
fun SearchPage(
    navController: NavHostController,
    archivedViewModel: ArchivedMemoListViewModel = hiltViewModel(),
) {
    val searchText = rememberTextFieldState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val memosViewModel = LocalMemos.current
    val settings by context.settingsDataStore.data.collectAsStateWithLifecycle(initialValue = Settings())
    val includeArchived = settings.usersList
        .firstOrNull { it.accountKey == settings.currentUser }
        ?.settings
        ?.searchIncludesArchived
        ?: false

    LaunchedEffect(Unit) {
        memosViewModel.loadTags()
    }
    LaunchedEffect(includeArchived) {
        if (includeArchived) archivedViewModel.loadMemos()
    }

    val query = searchText.text.toString()
    val normalizedQuery = remember(query) { query.trim().removePrefix("#") }
    val allTags = memosViewModel.tags.toList()
    val matchingTags = remember(allTags, normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            allTags
        } else {
            allTags.filter { it.contains(normalizedQuery, ignoreCase = true) }
        }
    }
    val archivedResults = remember(archivedViewModel.memos.toList(), query, includeArchived) {
        archivedSearchResults(archivedViewModel.memos, query, includeArchived)
    }

    val openTag: (String) -> Unit = { tag ->
        navController.navigate("${RouteName.TAG}/${Uri.encode(tag)}") {
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalArchivedMemos provides archivedViewModel) {
        MemoBrowser { onMemoClick ->
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surface,
                topBar = {
                    MemoSearchBar(searchText) {
                        navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                    }
                },
            ) { innerPadding ->
                MemosList(
                    contentPadding = innerPadding,
                    searchString = query,
                    onMemoClick = onMemoClick,
                    onTagClick = openTag,
                    archivedResults = archivedResults,
                    headerContent = {
                        if (matchingTags.isNotEmpty() || normalizedQuery.isEmpty()) {
                            item(key = "search-tags-panel") {
                                SearchTagsPanel(
                                    tags = matchingTags,
                                    isFiltered = normalizedQuery.isNotEmpty(),
                                    onTagClick = openTag,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchTagsPanel(
    tags: List<String>,
    isFiltered: Boolean,
    onTagClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        colors = meowmosCardColors(),
        border = meowmosCardBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(if (isFiltered) R.string.tags else R.string.all_tags),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                WayfindingBadge(
                    text = tags.size.toString(),
                    tone = if (tags.isNotEmpty()) WayfindingTone.Signal else WayfindingTone.Muted,
                )
            }
            Spacer(Modifier.height(10.dp))
            if (tags.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_tags),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tags.forEach { tag ->
                        AssistChip(
                            onClick = { onTagClick(tag) },
                            label = {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Tag,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            shape = MaterialTheme.shapes.small,
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary,
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemoSearchBar(searchText: TextFieldState, onBack: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    SearchBar(
        state = rememberSearchBarState(),
        modifier = Modifier
            .windowInsetsPadding(SearchBarDefaults.windowInsets)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        inputField = {
            SearchBarDefaults.InputField(
                state = searchText,
                onSearch = { focusManager.clearFocus() },
                // Results occupy this route's list pane, rather than an overlay.
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier.focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search)) },
                leadingIcon = {
                    ActionIconButton(label = stringResource(R.string.back), onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                trailingIcon = {
                    if (searchText.text.isNotEmpty()) {
                        ActionIconButton(
                            label = stringResource(R.string.clear_search),
                            onClick = {
                                searchText.edit { replace(0, length, "") }
                                focusRequester.requestFocus()
                            },
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    }
                },
            )
        },
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
