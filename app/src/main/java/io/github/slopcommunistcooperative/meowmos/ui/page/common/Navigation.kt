package io.github.slopcommunistcooperative.meowmos.ui.page.common

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.slopcommunistcooperative.meowmos.MainActivity
import io.github.slopcommunistcooperative.meowmos.data.model.ShareContent
import io.github.slopcommunistcooperative.meowmos.ext.navigateToMemoEditor
import io.github.slopcommunistcooperative.meowmos.ext.string
import io.github.slopcommunistcooperative.meowmos.ui.page.account.AccountPage
import io.github.slopcommunistcooperative.meowmos.ui.page.account.AddAccountPage
import io.github.slopcommunistcooperative.meowmos.ui.page.login.LoginPage
import io.github.slopcommunistcooperative.meowmos.ui.page.memoinput.MemoInputPage
import io.github.slopcommunistcooperative.meowmos.ui.page.memos.MemoDetailPage
import io.github.slopcommunistcooperative.meowmos.ui.page.memos.MemosPage
import io.github.slopcommunistcooperative.meowmos.ui.page.memos.SearchPage
import io.github.slopcommunistcooperative.meowmos.ui.page.memos.ArchivedMemoPage
import io.github.slopcommunistcooperative.meowmos.ui.page.memos.TagMemoPage
import io.github.slopcommunistcooperative.meowmos.ui.page.resource.ResourceListPage
import io.github.slopcommunistcooperative.meowmos.ui.page.settings.SettingsPage
import io.github.slopcommunistcooperative.meowmos.ui.theme.MeowmosTheme
import io.github.slopcommunistcooperative.meowmos.ui.util.rememberLocalNetworkPermissionRequest
import io.github.slopcommunistcooperative.meowmos.util.usesLocalNetwork
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalMemos
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState

@Composable
fun Navigation(dynamicColor: Boolean) {
    val navController = rememberNavController()
    val userStateViewModel = LocalUserState.current
    val memosViewModel = LocalMemos.current
    val context = LocalContext.current
    val requestLocalNetworkPermission = rememberLocalNetworkPermissionRequest()

    val currentHost = userStateViewModel.host
    LaunchedEffect(currentHost) {
        if (Build.VERSION.SDK_INT >= 37 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) !=
                PackageManager.PERMISSION_GRANTED &&
            currentHost.isNotBlank() &&
            usesLocalNetwork(currentHost) &&
            requestLocalNetworkPermission(currentHost) &&
            currentHost == userStateViewModel.host
        ) {
            userStateViewModel.loadCurrentUser()
            memosViewModel.loadMemos()
        }
    }
    var shareContent by remember { mutableStateOf<ShareContent?>(null) }

    CompositionLocalProvider(LocalRootNavController provides navController) {
        MeowmosTheme(dynamicColor = dynamicColor) {
            MemosNavHost(
                navController = navController,
                startDestination = RouteName.MEMOS,
            ) {
                composable(RouteName.MEMOS) {
                    MemosPage()
                }

                composable(RouteName.SETTINGS) {
                    SettingsPage(navController = navController)
                }

                composable(RouteName.ADD_ACCOUNT) {
                    AddAccountPage(navController = navController)
                }

                composable(RouteName.LOGIN) {
                    LoginPage(navController = navController)
                }

                composable(RouteName.INPUT) {
                    MemoInputPage()
                }

                composable(RouteName.SHARE) {
                    MemoInputPage(shareContent = shareContent)
                }

                composable("${RouteName.EDIT}?memoId={id}"
                ) { entry ->
                    MemoInputPage(memoIdentifier = entry.arguments?.getString("id"))
                }

                composable(RouteName.RESOURCE) {
                    ResourceListPage(navController = navController)
                }

                composable(RouteName.ARCHIVED) {
                    ArchivedMemoPage(navController = navController)
                }

                composable("${RouteName.ACCOUNT}?accountKey={accountKey}") { entry ->
                    AccountPage(
                        navController = navController,
                        selectedAccountKey = entry.arguments?.getString("accountKey") ?: ""
                    )
                }

                composable(RouteName.SEARCH) {
                    SearchPage(navController = navController)
                }

                composable("${RouteName.TAG}/{tag}") { entry ->
                    val tag = entry.arguments?.getString("tag")?.let(Uri::decode) ?: ""
                    TagMemoPage(tag = tag, navController = navController)
                }

                composable("${RouteName.MEMO_DETAIL}?memoId={memoId}") { entry ->
                    val memoId = entry.arguments?.getString("memoId")
                    if (memoId != null) {
                        MemoDetailPage(navController = navController, memoIdentifier = Uri.decode(memoId))
                    }
                }
            }
        }
    }


    LaunchedEffect(Unit) {
        if (!userStateViewModel.hasAnyAccount()) {
            if (navController.currentDestination?.route != RouteName.ADD_ACCOUNT) {
                navController.navigate(RouteName.ADD_ACCOUNT) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
            return@LaunchedEffect
        }
        userStateViewModel.loadCurrentUser()
    }

    suspend fun handleIntent(intent: Intent) {
        when(intent.action) {
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> {
                shareContent = ShareContent.parseIntent(intent)
                navController.navigate(RouteName.SHARE)
            }
            Intent.ACTION_VIEW -> {
                when (intent.getStringExtra("action")) {
                    "compose" -> navController.navigate(RouteName.INPUT)
                    "search" -> navController.navigate(RouteName.SEARCH)
                }
            }
            MainActivity.ACTION_NEW_MEMO -> {
                navController.navigate(RouteName.INPUT)
            }
            MainActivity.ACTION_EDIT_MEMO -> {
                val memoId = intent.getStringExtra(MainActivity.EXTRA_MEMO_ID)
                if (memoId != null) {
                    memosViewModel.awaitInitialLoad()
                    if (memosViewModel.memos.none { it.identifier == memoId }) {
                        navController.navigate("${RouteName.MEMO_DETAIL}?memoId=${Uri.encode(memoId)}")
                    } else {
                        navController.navigateToMemoEditor(memoId)
                    }
                }
            }
            MainActivity.ACTION_VIEW_MEMO -> {
                val memoId = intent.getStringExtra(MainActivity.EXTRA_MEMO_ID)
                if (memoId != null) {
                    memosViewModel.awaitInitialLoad()
                    navController.navigate("${RouteName.MEMO_DETAIL}?memoId=${Uri.encode(memoId)}")
                }
            }
        }
    }

    val activity = context as? MainActivity
    val pendingIntent = activity?.pendingIntent
    LaunchedEffect(pendingIntent) {
        if (pendingIntent == null) return@LaunchedEffect
        handleIntent(pendingIntent)
        activity.pendingIntent = null
    }
}

val LocalRootNavController =
    compositionLocalOf<NavHostController> { error(io.github.slopcommunistcooperative.meowmos.R.string.nav_host_controller_not_found.string) }
