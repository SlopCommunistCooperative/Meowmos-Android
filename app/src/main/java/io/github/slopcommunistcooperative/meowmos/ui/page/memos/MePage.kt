package io.github.slopcommunistcooperative.meowmos.ui.page.memos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.data.model.displayTitle
import io.github.slopcommunistcooperative.meowmos.ui.component.ActionIconButton
import io.github.slopcommunistcooperative.meowmos.ui.component.Heatmap
import io.github.slopcommunistcooperative.meowmos.ui.component.MemosIcon
import io.github.slopcommunistcooperative.meowmos.ui.component.Stats
import io.github.slopcommunistcooperative.meowmos.ui.component.WayfindingBadge
import io.github.slopcommunistcooperative.meowmos.ui.component.WayfindingTone
import io.github.slopcommunistcooperative.meowmos.ui.component.meowmosCardBorder
import io.github.slopcommunistcooperative.meowmos.ui.component.meowmosCardColors
import io.github.slopcommunistcooperative.meowmos.ui.page.common.LocalRootNavController
import io.github.slopcommunistcooperative.meowmos.ui.page.common.RouteName
import io.github.slopcommunistcooperative.meowmos.ui.theme.MeowmosTheme
import io.github.slopcommunistcooperative.meowmos.ui.util.edgeToEdgeContentPadding
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalMemos
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MePage() {
    val rootNavController = LocalRootNavController.current
    val memosViewModel = LocalMemos.current
    val userStateViewModel = LocalUserState.current
    val currentAccount by userStateViewModel.currentAccount.collectAsStateWithLifecycle()
    val syncStatus by memosViewModel.syncStatus.collectAsStateWithLifecycle()

    val weekDays = remember {
        val day = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        List(DayOfWeek.entries.size) { index ->
            day.plus(index.toLong()).getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }
    }

    LaunchedEffect(Unit) {
        memosViewModel.loadTags()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_me),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    ActionIconButton(
                        label = stringResource(R.string.settings),
                        onClick = { rootNavController.navigate(RouteName.SETTINGS) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
            contentPadding = edgeToEdgeContentPadding(
                contentPadding = innerPadding,
                additionalBottomPadding = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "account-card") {
                AccountIdentityCard(
                    account = currentAccount,
                    unsyncedCount = if (currentAccount is Account.Local) 0 else syncStatus.unsyncedCount,
                    onClick = {
                        val key = currentAccount?.accountKey()
                        if (key != null) {
                            rootNavController.navigate("${RouteName.ACCOUNT}?accountKey=$key")
                        } else {
                            rootNavController.navigate(RouteName.SETTINGS)
                        }
                    },
                )
            }

            item(key = "stats") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = meowmosCardColors(),
                    border = meowmosCardBorder(),
                ) {
                    Stats()
                }
            }

            item(key = "heatmap") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = meowmosCardColors(),
                    border = meowmosCardBorder(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.activity),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            HeatmapLegend()
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(112.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = weekDays[0],
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = weekDays[3],
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = weekDays[6],
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Heatmap()
                        }
                    }
                }
            }

            item(key = "utilities") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = meowmosCardColors(),
                    border = meowmosCardBorder(),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        ListItem(
                            modifier = Modifier.clickable {
                                rootNavController.navigate(RouteName.ARCHIVED)
                            },
                            headlineContent = {
                                Text(
                                    text = stringResource(R.string.archived),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = stringResource(R.string.archived_description),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            leadingContent = {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Inventory2,
                                        contentDescription = null,
                                        modifier = Modifier.padding(8.dp).size(20.dp),
                                    )
                                }
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        )
                        ListItem(
                            modifier = Modifier.clickable {
                                rootNavController.navigate(RouteName.SETTINGS)
                            },
                            headlineContent = {
                                Text(
                                    text = stringResource(R.string.settings),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = stringResource(R.string.settings_description),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            leadingContent = {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.padding(8.dp).size(20.dp),
                                    )
                                }
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountIdentityCard(
    account: Account?,
    unsyncedCount: Int,
    onClick: () -> Unit,
) {
    val title = when (account) {
        is Account.MemosV0 -> account.info.displayTitle()
        is Account.MemosV1 -> account.info.displayTitle()
        is Account.Local -> stringResource(R.string.local_account)
        null -> stringResource(R.string.app_name)
    }
    val subtitle = when (account) {
        is Account.MemosV0 -> account.info.host
        is Account.MemosV1 -> account.info.host
        is Account.Local -> stringResource(R.string.local_account_description)
        null -> null
    }
    val badgeIcon = when (account) {
        is Account.Local -> Icons.Outlined.Home
        is Account.MemosV0, is Account.MemosV1 -> MemosIcon
        null -> Icons.Outlined.Person
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = meowmosCardColors(),
        border = meowmosCardBorder(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MeowmosTheme.colors.wayfindingContainer,
                contentColor = MeowmosTheme.colors.onWayfindingContainer,
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (unsyncedCount > 0) {
                        WayfindingBadge(
                            text = unsyncedCount.toString(),
                            tone = WayfindingTone.Alert,
                        )
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeatmapLegend() {
    val extended = MeowmosTheme.colors
    val steps = listOf(
        extended.heatmapEmpty,
        extended.heatmapLevel1,
        extended.heatmapLevel2,
        extended.heatmapLevel3,
        extended.heatmapLevel4,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.heatmap_less),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        steps.forEach { color ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(color),
            )
        }
        Text(
            text = stringResource(R.string.heatmap_more),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
