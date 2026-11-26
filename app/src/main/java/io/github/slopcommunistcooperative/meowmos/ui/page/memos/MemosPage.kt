package io.github.slopcommunistcooperative.meowmos.ui.page.memos

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.ui.page.common.LocalRootNavController
import io.github.slopcommunistcooperative.meowmos.ui.page.common.RouteName
import io.github.slopcommunistcooperative.meowmos.ui.theme.MeowmosTheme
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState

internal enum class MeowmosDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val selectedIcon: Int,
    @param:DrawableRes val unselectedIcon: Int,
) {
    MEMOS(
        route = RouteName.MEMOS,
        labelRes = R.string.memos,
        selectedIcon = R.drawable.symbol_sticky_note_2_fill1,
        unselectedIcon = R.drawable.symbol_sticky_note_2,
    ),
    EXPLORE(
        route = RouteName.EXPLORE,
        labelRes = R.string.explore,
        selectedIcon = R.drawable.symbol_explore_fill1,
        unselectedIcon = R.drawable.symbol_explore,
    ),
    RESOURCES(
        route = RouteName.RESOURCE,
        labelRes = R.string.resources,
        selectedIcon = R.drawable.symbol_photo_library_fill1,
        unselectedIcon = R.drawable.symbol_photo_library,
    ),
    ME(
        route = RouteName.ME,
        labelRes = R.string.nav_me,
        selectedIcon = R.drawable.symbol_person_fill1,
        unselectedIcon = R.drawable.symbol_person,
    ),
}

@Composable
fun MemosPage() {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val useRail = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val memosNavController = rememberNavController()
    val rootNavController = LocalRootNavController.current
    val userStateViewModel = LocalUserState.current
    val currentAccount by userStateViewModel.currentAccount.collectAsStateWithLifecycle()
    val hasExplore = currentAccount !is Account.Local

    val destinations = remember(hasExplore) {
        if (hasExplore) {
            MeowmosDestination.entries
        } else {
            MeowmosDestination.entries.filterNot { it == MeowmosDestination.EXPLORE }
        }
    }

    val navBackStackEntry by memosNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val selectedRoute = destinations.firstOrNull { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }?.route ?: RouteName.MEMOS

    LaunchedEffect(hasExplore, selectedRoute) {
        if (!hasExplore && currentDestination?.route == RouteName.EXPLORE) {
            memosNavController.navigateToMemoTab(RouteName.MEMOS)
        }
    }

    MemosNavigationScaffold(
        useRail = useRail,
        destinations = destinations,
        selectedRoute = selectedRoute,
        onSelectDestination = { destination ->
            if (destination.route != selectedRoute) {
                memosNavController.navigateToMemoTab(destination.route)
            }
        },
        onCompose = {
            rootNavController.navigate(RouteName.INPUT)
        },
    ) { inRailLayout ->
        MemosNavigation(
            navController = memosNavController,
            showHomeComposeFab = !inRailLayout,
        )
    }
}

internal fun NavHostController.navigateToMemoTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Adaptive Material 3 navigation shell:
 * - Compact width: Material 3 [ShortNavigationBar] at bottom.
 * - Medium / expanded width: persistent [NavigationRail] on the start edge with compose FAB.
 * Uses [movableContentOf] so list/detail selection and scroll state survive crossing the breakpoint.
 */
@Composable
internal fun MemosNavigationScaffold(
    useRail: Boolean,
    destinations: List<MeowmosDestination>,
    selectedRoute: String,
    onSelectDestination: (MeowmosDestination) -> Unit,
    onCompose: () -> Unit,
    content: @Composable (useRail: Boolean) -> Unit,
) {
    val currentContent = rememberUpdatedState(content)
    val movableBody = remember {
        movableContentOf<Boolean> { rail -> currentContent.value(rail) }
    }
    val wayfindingContainer = MeowmosTheme.colors.wayfindingContainer
    val onWayfindingContainer = MeowmosTheme.colors.onWayfindingContainer

    if (useRail) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                header = {
                    Spacer(Modifier.height(8.dp))
                    FloatingActionButton(
                        onClick = onCompose,
                        containerColor = wayfindingContainer,
                        contentColor = onWayfindingContainer,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 1.dp,
                        ),
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.symbol_add),
                            contentDescription = stringResource(R.string.new_memo),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                },
            ) {
                destinations.forEach { destination ->
                    val selected = destination.route == selectedRoute
                    val label = stringResource(destination.labelRes)
                    NavigationRailItem(
                        selected = selected,
                        onClick = { onSelectDestination(destination) },
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(if (selected) destination.selectedIcon else destination.unselectedIcon),
                                contentDescription = label,
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = onWayfindingContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = wayfindingContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            )
            Box(Modifier.weight(1f).fillMaxHeight()) {
                movableBody(true)
            }
        }
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.surface,
            bottomBar = {
                androidx.compose.foundation.layout.Column {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    )
                    ShortNavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        destinations.forEach { destination ->
                            val selected = destination.route == selectedRoute
                            val label = stringResource(destination.labelRes)
                            ShortNavigationBarItem(
                                selected = selected,
                                onClick = { onSelectDestination(destination) },
                                icon = {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(if (selected) destination.selectedIcon else destination.unselectedIcon),
                                        contentDescription = label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                colors = ShortNavigationBarItemDefaults.colors(
                                    selectedIconColor = onWayfindingContainer,
                                    selectedTextColorTopIconPosition = MaterialTheme.colorScheme.onSurface,
                                    selectedIndicatorColor = wayfindingContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .consumeWindowInsets(innerPadding),
            ) {
                movableBody(false)
            }
        }
    }
}
