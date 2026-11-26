package io.github.slopcommunistcooperative.meowmos.ui.page.memos

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.ui.page.common.LocalRootNavController
import io.github.slopcommunistcooperative.meowmos.ui.page.common.RouteName
import io.github.slopcommunistcooperative.meowmos.ui.page.resource.ResourceListPage
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState

@Composable
fun MemosNavigation(
    navController: NavHostController,
    showHomeComposeFab: Boolean = true,
) {
    val rootNavController = LocalRootNavController.current
    val userStateViewModel = LocalUserState.current
    val currentAccount by userStateViewModel.currentAccount.collectAsStateWithLifecycle()
    val hasExplore = currentAccount !is Account.Local

    // Top-level bottom-bar / rail destinations use Material 3 fade-through motion rather than
    // lateral push transitions.
    NavHost(
        navController = navController,
        startDestination = RouteName.MEMOS,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        enterTransition = { fadeIn(animationSpec = tween(180)) },
        exitTransition = { fadeOut(animationSpec = tween(140)) },
        popEnterTransition = { fadeIn(animationSpec = tween(180)) },
        popExitTransition = { fadeOut(animationSpec = tween(140)) },
    ) {
        composable(RouteName.MEMOS) {
            MemosHomePage(
                showComposeFab = showHomeComposeFab,
            )
        }

        composable(RouteName.EXPLORE) {
            if (hasExplore) {
                ExplorePage()
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(RouteName.MEMOS) {
                        popUpTo(RouteName.EXPLORE) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }

        composable(RouteName.RESOURCE) {
            ResourceListPage(
                navController = rootNavController,
                showBackButton = false,
            )
        }

        composable(RouteName.ME) {
            MePage()
        }
    }
}
