package io.github.slopcommunistcooperative.meowmos.ui.page.settings

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import io.github.slopcommunistcooperative.meowmos.ui.component.ActionIconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.data.model.MemoEditGesture
import io.github.slopcommunistcooperative.meowmos.data.model.Settings
import io.github.slopcommunistcooperative.meowmos.data.model.UserSettings
import io.github.slopcommunistcooperative.meowmos.data.model.displayTitle
import io.github.slopcommunistcooperative.meowmos.data.mtls.MtlsManager
import io.github.slopcommunistcooperative.meowmos.ext.popBackStackIfLifecycleIsResumed
import io.github.slopcommunistcooperative.meowmos.ext.settingsDataStore
import io.github.slopcommunistcooperative.meowmos.ext.string
import io.github.slopcommunistcooperative.meowmos.ui.component.MemosIcon
import io.github.slopcommunistcooperative.meowmos.ui.component.WayfindingSectionHeader
import io.github.slopcommunistcooperative.meowmos.ui.component.meowmosCardBorder
import io.github.slopcommunistcooperative.meowmos.ui.component.meowmosCardColors
import io.github.slopcommunistcooperative.meowmos.ui.util.edgeToEdgeContentPadding
import io.github.slopcommunistcooperative.meowmos.ui.page.common.RouteName
import io.github.slopcommunistcooperative.meowmos.ui.security.AppLockAuthenticator
import io.github.slopcommunistcooperative.meowmos.ui.security.AppLockSession
import io.github.slopcommunistcooperative.meowmos.ui.security.findFragmentActivity
import io.github.slopcommunistcooperative.meowmos.util.AppLanguage
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState
import io.github.slopcommunistcooperative.meowmos.widget.WidgetUpdater

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    navController: NavHostController
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val userStateViewModel = LocalUserState.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val accounts by userStateViewModel.accounts.collectAsStateWithLifecycle()
    val currentAccount by userStateViewModel.currentAccount.collectAsStateWithLifecycle()
    val settings by context.settingsDataStore.data.collectAsStateWithLifecycle(initialValue = Settings())
    val appLockSupported = remember(context, AppLockSession.foregroundGeneration) {
        AppLockAuthenticator.canAuthenticate(context)
    }
    var showEditGestureDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(AppLanguage.currentTag()) }
    var showRemoveCertificateDialog by remember { mutableStateOf(false) }
    var hasClientCertificate by remember {
        mutableStateOf(
            MtlsManager.hasSelectedCertificate(context)
        )
    }

    fun chooseClientCertificate() {
        val activity = context as? Activity
            ?: return

        MtlsManager.chooseCertificate(
            activity
        ) { success ->
            if (success) {
                hasClientCertificate = true
            }
        }
    }

    fun removeClientCertificate() {
        MtlsManager.clearSelectedCertificate(context)
        hasClientCertificate = false
        showRemoveCertificateDialog = false
    }

    fun setAppLockEnabled(enabled: Boolean) {
        if (enabled && !appLockSupported) {
            return
        }
        if (enabled) {
            AppLockSession.lock()
        }
        scope.launch(Dispatchers.IO) {
            context.settingsDataStore.updateData { existingSettings ->
                existingSettings.copy(appLockEnabled = enabled)
            }
        }
    }

    fun selectLanguage(tag: String) {
        showLanguageDialog = false
        if (tag == selectedLanguage) return
        AppLanguage.setLanguage(context, tag)
        selectedLanguage = AppLanguage.currentTag()
        WidgetUpdater.updateWidgets(context)
        context.findFragmentActivity()?.recreate()
    }

    val currentEditGesture = settings.usersList
        .firstOrNull { it.accountKey == settings.currentUser }
        ?.settings
        ?.editGesture
        ?: MemoEditGesture.NONE
    val autosaveEnabled = settings.usersList
        .firstOrNull { it.accountKey == settings.currentUser }
        ?.settings
        ?.autosave
        ?: false

    val searchIncludesArchived = settings.usersList
        .firstOrNull { it.accountKey == settings.currentUser }
        ?.settings
        ?.searchIncludesArchived
        ?: false

    fun updateCurrentUserSettings(transform: (UserSettings) -> UserSettings) {
        scope.launch(Dispatchers.IO) {
            context.settingsDataStore.updateData { existingSettings ->
                val userIndex = existingSettings.usersList.indexOfFirst { user ->
                    user.accountKey == existingSettings.currentUser
                }
                if (userIndex == -1) {
                    return@updateData existingSettings
                }
                val users = existingSettings.usersList.toMutableList()
                val user = users[userIndex]
                users[userIndex] = user.copy(settings = transform(user.settings))
                existingSettings.copy(usersList = users)
            }
        }
    }

    fun setAutosaveEnabled(enabled: Boolean) =
        updateCurrentUserSettings { it.copy(autosave = enabled) }

    fun setSearchIncludesArchived(enabled: Boolean) =
        updateCurrentUserSettings { it.copy(searchIncludesArchived = enabled) }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = R.string.settings.string) },
                navigationIcon = {
                    ActionIconButton(label = R.string.back.string, onClick = {
                        navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                    }) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.symbol_arrow_back),
                            contentDescription = R.string.back.string
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = edgeToEdgeContentPadding(
                contentPadding = innerPadding,
                additionalBottomPadding = 24.dp,
            ),
        ) {
            item {
                WayfindingSectionHeader(title = R.string.accounts.string)
                SettingsGroupCard {
                    accounts.forEachIndexed { index, account ->
                        if (index > 0) SettingsGroupDivider()
                        when (account) {
                            is Account.MemosV0 -> SettingItem(
                                icon = MemosIcon,
                                text = account.info.displayTitle(),
                                subtitle = account.info.host,
                                trailingIcon = {
                                    if (currentAccount?.accountKey() == account.accountKey()) {
                                        Icon(
                                            ImageVector.vectorResource(R.drawable.symbol_check),
                                            contentDescription = R.string.account_selected.string,
                                            modifier = Modifier.padding(start = 16.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                            ) {
                                navController.navigate("${RouteName.ACCOUNT}?accountKey=${account.accountKey()}")
                            }

                            is Account.MemosV1 -> SettingItem(
                                icon = MemosIcon,
                                text = account.info.displayTitle(),
                                subtitle = account.info.host,
                                trailingIcon = {
                                    if (currentAccount?.accountKey() == account.accountKey()) {
                                        Icon(
                                            ImageVector.vectorResource(R.drawable.symbol_check),
                                            contentDescription = R.string.account_selected.string,
                                            modifier = Modifier.padding(start = 16.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                            ) {
                                navController.navigate("${RouteName.ACCOUNT}?accountKey=${account.accountKey()}")
                            }

                            is Account.Local -> SettingItem(
                                icon = ImageVector.vectorResource(R.drawable.symbol_home),
                                text = R.string.local_account.string,
                                trailingIcon = {
                                    if (currentAccount?.accountKey() == account.accountKey()) {
                                        Icon(
                                            ImageVector.vectorResource(R.drawable.symbol_check),
                                            contentDescription = R.string.account_selected.string,
                                            modifier = Modifier.padding(start = 16.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                            ) {
                                navController.navigate("${RouteName.ACCOUNT}?accountKey=${account.accountKey()}")
                            }
                        }
                    }
                    if (accounts.isNotEmpty()) SettingsGroupDivider()
                    SettingItem(icon = ImageVector.vectorResource(R.drawable.symbol_person_add), text = R.string.add_account.string) {
                        navController.navigate(RouteName.ADD_ACCOUNT)
                    }
                }
            }

            item {
                WayfindingSectionHeader(
                    title = R.string.preferences.string,
                    modifier = Modifier.padding(top = 12.dp),
                )
                SettingsGroupCard {
                    SettingSwitchItem(
                        icon = ImageVector.vectorResource(R.drawable.symbol_palette),
                        text = R.string.dynamic_color.string,
                        subtitle = R.string.dynamic_color_summary.string,
                        checked = settings.dynamicColorEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                context.settingsDataStore.updateData { it.copy(dynamicColorEnabled = enabled) }
                            }
                        },
                    )
                    SettingsGroupDivider()
                    SettingItem(
                        icon = ImageVector.vectorResource(R.drawable.symbol_language),
                        text = R.string.language.string,
                        trailingIcon = {
                            Text(
                                text = AppLanguage.options.firstOrNull { it.tag == selectedLanguage }?.displayName
                                    ?: R.string.language_system_default.string,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    ) {
                        showLanguageDialog = true
                    }
                    SettingsGroupDivider()
                    SettingItem(
                        icon = ImageVector.vectorResource(R.drawable.symbol_edit),
                        text = R.string.edit_gesture.string,
                        trailingIcon = {
                            Text(
                                text = currentEditGesture.titleResource.string,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    ) {
                        showEditGestureDialog = true
                    }
                    SettingsGroupDivider()
                    SettingSwitchItem(
                        icon = ImageVector.vectorResource(R.drawable.symbol_save),
                        text = R.string.autosave.string,
                        subtitle = R.string.autosave_summary.string,
                        checked = autosaveEnabled,
                        onCheckedChange = ::setAutosaveEnabled,
                    )
                    SettingsGroupDivider()
                    SettingSwitchItem(
                        icon = ImageVector.vectorResource(R.drawable.symbol_archive),
                        text = R.string.search_include_archived.string,
                        subtitle = R.string.search_include_archived_summary.string,
                        checked = searchIncludesArchived,
                        onCheckedChange = ::setSearchIncludesArchived,
                    )
                }
            }

            item {
                WayfindingSectionHeader(
                    title = R.string.security.string,
                    modifier = Modifier.padding(top = 12.dp),
                )
                SettingsGroupCard {
                    val appLockToggleEnabled = appLockSupported || settings.appLockEnabled
                    SettingSwitchItem(
                        icon = ImageVector.vectorResource(R.drawable.symbol_lock),
                        text = R.string.app_lock.string,
                        subtitle = if (appLockSupported) {
                            R.string.app_lock_summary.string
                        } else {
                            R.string.app_lock_unavailable_short.string
                        },
                        checked = settings.appLockEnabled,
                        enabled = appLockToggleEnabled,
                        onCheckedChange = ::setAppLockEnabled,
                    )
                    SettingsGroupDivider()
                    SettingItem(
                        icon = ImageVector.vectorResource(R.drawable.symbol_lock),
                        text = R.string.client_certificate.string,
                        subtitle = if (hasClientCertificate) {
                            R.string.client_certificate_selected.string
                        } else {
                            R.string.client_certificate_not_selected.string
                        },
                    ) {
                        chooseClientCertificate()
                    }
                    if (hasClientCertificate) {
                        SettingsGroupDivider()
                        SettingItem(
                            icon = ImageVector.vectorResource(R.drawable.symbol_lock),
                            text = R.string.remove_client_certificate.string,
                            subtitle = R.string.remove_client_certificate_message.string,
                        ) {
                            showRemoveCertificateDialog = true
                        }
                    }
                }
            }

            item {
                WayfindingSectionHeader(
                    title = R.string.about.string,
                    modifier = Modifier.padding(top = 12.dp),
                )
                SettingsGroupCard {
                    SettingItem(icon = ImageVector.vectorResource(R.drawable.symbol_web), text = R.string.website.string) {
                        uriHandler.openUri("https://github.com/SlopCommunistCooperative/Meowmos-Android")
                    }
                    SettingsGroupDivider()
                    SettingItem(icon = ImageVector.vectorResource(R.drawable.symbol_lock), text = R.string.privacy_policy.string) {
                        uriHandler.openUri("https://memos.moe/privacy")
                    }
                    SettingsGroupDivider()
                    SettingItem(icon = ImageVector.vectorResource(R.drawable.symbol_code), text = R.string.acknowledgements.string) {
                        uriHandler.openUri("https://memos.moe/android-acknowledgements")
                    }
                    SettingsGroupDivider()
                    SettingItem(
                        icon = ImageVector.vectorResource(R.drawable.symbol_bug_report),
                        text = R.string.report_an_issue.string,
                    ) {
                        uriHandler.openUri("https://github.com/SlopCommunistCooperative/Meowmos-Android/issues")
                    }
                }
            }
        }
    }
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(R.string.language.string) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    item {
                        LanguageChoiceRow(
                            text = R.string.language_system_default.string,
                            selected = selectedLanguage == AppLanguage.SYSTEM,
                            onClick = { selectLanguage(AppLanguage.SYSTEM) }
                        )
                    }
                    items(AppLanguage.options.size) { index ->
                        val option = AppLanguage.options[index]
                        LanguageChoiceRow(
                            text = option.displayName,
                            selected = option.tag == selectedLanguage,
                            onClick = { selectLanguage(option.tag) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(R.string.cancel.string)
                }
            }
        )
    }
    if (showRemoveCertificateDialog) {
        AlertDialog(
            onDismissRequest = {
                showRemoveCertificateDialog = false
            },
            title = {
                Text(R.string.remove_client_certificate_title.string)
            },
            text = {
                Text(
                    R.string.remove_client_certificate_message.string
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        removeClientCertificate()
                    }
                ) {
                    Text(R.string.remove.string)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemoveCertificateDialog = false
                    }
                ) {
                    Text(R.string.cancel.string)
                }
            }
        )
    }
    if (showEditGestureDialog) {
        AlertDialog(
            onDismissRequest = { showEditGestureDialog = false },
            title = { Text(R.string.edit_gesture.string) },
            text = {
                LazyColumn {
                    items(MemoEditGesture.entries.size) { index ->
                        val gesture = MemoEditGesture.entries[index]
                        TextButton(
                            onClick = {
                                showEditGestureDialog = false
                                scope.launch(Dispatchers.IO) {
                                    context.settingsDataStore.updateData { existingSettings ->
                                        val userIndex =
                                            existingSettings.usersList.indexOfFirst { user ->
                                                user.accountKey == existingSettings.currentUser
                                            }
                                        if (userIndex == -1) {
                                            return@updateData existingSettings
                                        }
                                        val users = existingSettings.usersList.toMutableList()
                                        val user = users[userIndex]
                                        users[userIndex] = user.copy(
                                            settings = user.settings.copy(editGesture = gesture)
                                        )
                                        existingSettings.copy(usersList = users)
                                    }
                                    WidgetUpdater.updateWidgets(context)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = gesture.titleResource.string,
                                color = if (gesture == currentEditGesture) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEditGestureDialog = false }) {
                    Text(R.string.close.string)
                }
            }
        )
    }
}


@Composable
private fun SettingsGroupCard(
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = meowmosCardColors(),
        border = meowmosCardBorder(),
    ) {
        Column(Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsGroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}
@Composable
private fun LanguageChoiceRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val MemoEditGesture.titleResource: Int
    get() = when (this) {
        MemoEditGesture.NONE -> R.string.edit_gesture_none
        MemoEditGesture.SINGLE -> R.string.edit_gesture_single
        MemoEditGesture.DOUBLE -> R.string.edit_gesture_double
        MemoEditGesture.LONG -> R.string.edit_gesture_long
    }
