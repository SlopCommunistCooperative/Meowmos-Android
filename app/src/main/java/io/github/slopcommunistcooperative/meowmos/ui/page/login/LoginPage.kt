package io.github.slopcommunistcooperative.meowmos.ui.page.login

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.skydoves.sandwich.suspendOnSuccess
import kotlinx.coroutines.launch
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.mtls.MtlsManager
import io.github.slopcommunistcooperative.meowmos.ext.popBackStackIfLifecycleIsResumed
import io.github.slopcommunistcooperative.meowmos.ext.string
import io.github.slopcommunistcooperative.meowmos.ext.suspendOnErrorMessage
import io.github.slopcommunistcooperative.meowmos.ui.component.Markdown
import io.github.slopcommunistcooperative.meowmos.ui.util.rememberLocalNetworkPermissionRequest
import io.github.slopcommunistcooperative.meowmos.ui.page.common.RouteName
import io.github.slopcommunistcooperative.meowmos.ui.theme.MeowmosTheme
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState
import io.github.slopcommunistcooperative.meowmos.viewmodel.LoginCompatibility

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoginPage(
    navController: NavHostController
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val lifecycleOwner = LocalLifecycleOwner.current
    val userStateViewModel = LocalUserState.current
    val snackbarState = remember { SnackbarHostState() }
    val requestLocalNetworkPermission = rememberLocalNetworkPermissionRequest()

    var accountLabel by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    var host by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    var accessToken by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    val context = LocalContext.current

    var hasClientCertificate by remember {
        mutableStateOf(
            MtlsManager.hasSelectedCertificate(context)
        )
    }

    var loginCompatibilityWarning by remember { mutableStateOf<String?>(null) }

    fun normalizedHost(): String {
        val trimmed = host.text.trim()
        return if (trimmed.contains("//")) trimmed else "https://$trimmed"
    }

    fun login(allowHigherV1Version: Boolean = false) = coroutineScope.launch {
        if (host.text.isBlank() || accessToken.text.isBlank()) {
            snackbarState.showSnackbar(R.string.fill_login_form.string)
            return@launch
        }

        val sanitizedHost = normalizedHost()
        host = TextFieldValue(sanitizedHost)
        if (!requestLocalNetworkPermission(sanitizedHost)) return@launch

        if (!allowHigherV1Version) {
            when (val compatibility = userStateViewModel.checkLoginCompatibility(sanitizedHost)) {
                LoginCompatibility.Supported -> Unit
                is LoginCompatibility.Unsupported -> {
                    snackbarState.showSnackbar(compatibility.message)
                    return@launch
                }

                is LoginCompatibility.RequiresConfirmation -> {
                    loginCompatibilityWarning = compatibility.message
                    return@launch
                }
            }
        }

        val resp = userStateViewModel.loginMemosWithAccessToken(
            host = sanitizedHost,
            accessToken = accessToken.text.trim(),
            accountLabel = accountLabel.text,
            allowHigherV1Version = allowHigherV1Version,
        )
        resp.suspendOnSuccess {
            navController.navigate(RouteName.MEMOS) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
            .suspendOnErrorMessage {
                snackbarState.showSnackbar(it)
            }
    }

    if (loginCompatibilityWarning != null) {
        AlertDialog(
            onDismissRequest = { loginCompatibilityWarning = null },
            title = { Text(text = R.string.unsupported_memos_version_title.string) },
            text = { Text(text = loginCompatibilityWarning ?: "") },
            confirmButton = {
                TextButton(
                    onClick = {
                        loginCompatibilityWarning = null
                        login(allowHigherV1Version = true)
                    }
                ) {
                    Text(R.string.still_login.string)
                }
            },
            dismissButton = {
                TextButton(onClick = { loginCompatibilityWarning = null }) {
                    Text(R.string.cancel.string)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .imePadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        },
        topBar = {
            LargeTopAppBar(
                title = { Text(text = if (userStateViewModel.currentUser != null) R.string.add_account.string else R.string.app_name.string) },
                navigationIcon = {
                    if (userStateViewModel.currentUser != null) {
                        IconButton(onClick = {
                            navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                        }) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.symbol_arrow_back),
                                contentDescription = R.string.back.string
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
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                actions = {},
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { login() },
                        containerColor = MeowmosTheme.colors.wayfindingContainer,
                        contentColor = MeowmosTheme.colors.onWayfindingContainer,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                        text = { Text(R.string.add_account.string) },
                        icon = {
                            Icon(
                                ImageVector.vectorResource(R.drawable.symbol_login),
                                contentDescription = R.string.add_account.string
                            )
                        }
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxHeight()
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Markdown(
                R.string.input_login_information.string,
                modifier = Modifier.padding(bottom = 20.dp),
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 30.dp)
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                    value = accountLabel,
                    onValueChange = { accountLabel = it },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.symbol_badge),
                            contentDescription = R.string.account_name.string
                        )
                    },
                    label = {
                        Text(R.string.account_name.string)
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                    value = host,
                    onValueChange = { host = it },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.symbol_computer),
                            contentDescription = R.string.address.string
                        )
                    },
                    label = {
                        Text(R.string.host.string)
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                    value = accessToken,
                    onValueChange = { accessToken = it },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.symbol_person),
                            contentDescription = R.string.access_token.string
                        )
                    },
                    label = {
                        Text(R.string.access_token.string)
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { login() })
                )
                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = R.string.client_certificate.string,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = if (hasClientCertificate) {
                            R.string.client_certificate_selected.string
                        } else {
                            R.string.client_certificate_not_selected.string
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        modifier = Modifier.padding(top = 8.dp),
                        onClick = {
                            val activity = context as? Activity
                                ?: return@Button

                            MtlsManager.chooseCertificate(
                                activity
                            ) { success ->
                                if (success) {
                                    hasClientCertificate = true
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.symbol_lock),
                            contentDescription = null
                        )

                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = if (hasClientCertificate) {
                                R.string.change_certificate.string
                            } else {
                                R.string.select_certificate.string
                            }
                        )
                    }
                }
            }
        }
    }
}
