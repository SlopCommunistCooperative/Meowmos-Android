package io.github.slopcommunistcooperative.meowmos.ui

import android.content.res.Configuration
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import io.github.slopcommunistcooperative.meowmos.ui.theme.MeowmosTheme
import java.util.Locale
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.paging.LoadState
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.model.MemoSpace
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility
import io.github.slopcommunistcooperative.meowmos.ext.titleResource
import io.github.slopcommunistcooperative.meowmos.ui.component.MemoMenuAction
import io.github.slopcommunistcooperative.meowmos.ui.component.MemoOverflowMenu
import io.github.slopcommunistcooperative.meowmos.ui.component.SyncStatusBadge
import io.github.slopcommunistcooperative.meowmos.ui.page.memoinput.MemoInputTopBar
import io.github.slopcommunistcooperative.meowmos.ui.page.memoinput.MemoSpacePicker
import io.github.slopcommunistcooperative.meowmos.ui.page.memoinput.MemoVisibilityPicker
import io.github.slopcommunistcooperative.meowmos.ui.page.memos.MemoListDetailLayout
import io.github.slopcommunistcooperative.meowmos.ui.page.common.RouteName
import io.github.slopcommunistcooperative.meowmos.ui.page.memos.MeowmosDestination
import io.github.slopcommunistcooperative.meowmos.ui.page.memos.MemosNavigationScaffold
import io.github.slopcommunistcooperative.meowmos.ui.page.memos.MemoSearchBar
import io.github.slopcommunistcooperative.meowmos.ui.page.memos.PagingStatus
import io.github.slopcommunistcooperative.meowmos.ui.page.memos.navigateToMemoTab
import io.github.slopcommunistcooperative.meowmos.ui.page.settings.SettingSwitchItem
import io.github.slopcommunistcooperative.meowmos.ui.util.edgeToEdgeContentPadding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UiComponentsTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun postingSpaceAppearsBeforePostAndCanBeCleared() {
        val family = MemoSpace("spaces/family", "Family")
        val noSpace = context.getString(R.string.memo_no_space)
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                var selected by remember { mutableStateOf<MemoSpace?>(null) }
                MemoInputTopBar(isEditMode = false, initialized = true, canSubmit = true, onClose = {}, onSubmit = {}, destination = {
                    MemoSpacePicker(listOf(family), selected?.name, null) { selected = it }
                })
            }
        }
        val picker = compose.onNodeWithText(noSpace)
        val post = compose.onNodeWithContentDescription(context.getString(R.string.post))
        val pickerBounds = picker.fetchSemanticsNode().boundsInRoot
        val postBounds = post.fetchSemanticsNode().boundsInRoot
        assertTrue(pickerBounds.right <= postBounds.left)
        picker.performClick()
        compose.onNodeWithText(family.title).performClick().assertIsDisplayed()
        compose.onNodeWithText(family.title).performClick()
        compose.onNodeWithText(noSpace).performClick()
        compose.onNodeWithText(noSpace).assertIsDisplayed()
    }

    @Test fun backWaitsForTheRememberedSpaceBeforeSaving() {
        val preference = CompletableDeferred<MemoSpace>()
        val publishedTo = mutableListOf<String?>()
        compose.setContent {
            MeowmosTheme(dynamicColor = false) {
                var destination by remember { mutableStateOf<MemoSpace?>(null) }
                LaunchedEffect(Unit) { destination = preference.await() }
                MemoInputTopBar(
                    isEditMode = false,
                    initialized = destination != null,
                    canSubmit = true,
                    onClose = { publishedTo += destination?.name },
                    onSubmit = { publishedTo += destination?.name },
                    destination = {},
                )
            }
        }
        compose.onNodeWithContentDescription(context.getString(R.string.post)).assertIsNotEnabled()
        Espresso.pressBack()
        compose.runOnIdle {
            assertTrue(publishedTo.isEmpty())
            preference.complete(MemoSpace("spaces/family", "Family"))
        }
        compose.runOnIdle { assertEquals(listOf("spaces/family"), publishedTo) }
    }

    @Test fun commentAudienceLabelOpensTheDropdownAndTracksSelection() {
        compose.setContent {
            MeowmosTheme(dynamicColor = false) {
                var visibility by remember { mutableStateOf(MemoVisibility.PRIVATE) }
                var expanded by remember { mutableStateOf(false) }
                MemoVisibilityPicker(
                    currentVisibility = visibility,
                    showSpaceVisibility = false,
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    onVisibilitySelected = { visibility = it },
                    showLabel = true,
                )
            }
        }
        val privateLabel = context.getString(MemoVisibility.PRIVATE.titleResource)
        val publicLabel = context.getString(MemoVisibility.PUBLIC.titleResource)
        compose.onNodeWithText(privateLabel).performClick()
        compose.onNodeWithText(context.getString(MemoVisibility.SPACE.titleResource)).assertDoesNotExist()
        compose.onNodeWithText(publicLabel).performClick()
        compose.onNodeWithContentDescription(publicLabel).assertIsDisplayed().performClick()
        compose.onNodeWithText(privateLabel).assertIsDisplayed()
    }

    @Test fun bottomNavigationSwitchesDestinationsAndRespectsDestinationList() {
        var selected by mutableStateOf(RouteName.MEMOS)
        val localDestinations = MeowmosDestination.entries.filterNot { it == MeowmosDestination.EXPLORE }
        compose.setContent {
            MeowmosTheme(dynamicColor = false) {
                MemosNavigationScaffold(
                    useRail = false,
                    destinations = localDestinations,
                    selectedRoute = selected,
                    onSelectDestination = { selected = it.route },
                    onCompose = {},
                ) {
                    Text("Active: $selected")
                }
            }
        }
        compose.onNodeWithText(context.getString(R.string.memos)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.resources)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.nav_me)).assertIsDisplayed()
        compose.onAllNodesWithText(context.getString(R.string.explore)).assertCountEquals(0)
        compose.onNodeWithText(context.getString(R.string.nav_me)).performClick()
        compose.onNodeWithText("Active: ${RouteName.ME}").assertIsDisplayed()

        val zhConfig = Configuration(context.resources.configuration).apply {
            setLocale(Locale.SIMPLIFIED_CHINESE)
        }
        val zhContext = context.createConfigurationContext(zhConfig)
        assertEquals(
            listOf("笔记", "探索", "资源", "我"),
            MeowmosDestination.entries.map { zhContext.getString(it.labelRes) },
        )
    }

    @Test fun returningToATabRestoresItsPreviousSelection() {
        compose.setContent {
            MeowmosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                val entry by navController.currentBackStackEntryAsState()
                MemosNavigationScaffold(
                    useRail = false,
                    destinations = listOf(MeowmosDestination.MEMOS, MeowmosDestination.RESOURCES),
                    selectedRoute = entry?.destination?.route ?: RouteName.MEMOS,
                    onSelectDestination = { navController.navigateToMemoTab(it.route) },
                    onCompose = {},
                ) {
                    NavHost(navController, startDestination = RouteName.MEMOS) {
                        composable(RouteName.MEMOS) { Text("Memo feed") }
                        composable(RouteName.RESOURCE) {
                            var showImages by rememberSaveable { mutableStateOf(true) }
                            Button(onClick = { showImages = !showImages }) {
                                Text(if (showImages) "Images" else "Other files")
                            }
                        }
                    }
                }
            }
        }
        val resources = compose.onNodeWithText(context.getString(R.string.resources))
        resources.performClick()
        compose.onNodeWithText("Images").performClick()
        compose.onNodeWithText("Other files").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.memos)).performClick()
        compose.onNodeWithText("Memo feed").assertIsDisplayed()
        resources.performClick()
        compose.onNodeWithText("Other files").assertIsDisplayed()
    }

    @Test fun settingRowExposesOneSwitchAndTogglesFromItsLabel() {
        compose.setContent {
            MaterialTheme {
                var checked by remember { mutableStateOf(false) }
                SettingSwitchItem(ImageVector.vectorResource(R.drawable.symbol_save), "Autosave", checked = checked) { checked = it }
            }
        }
        compose.onAllNodesWithText("Autosave").assertCountEquals(1)
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOff().performClick().assertIsOn()
    }

    @Test fun disabledSettingCannotBeToggled() {
        compose.setContent {
            MaterialTheme {
                SettingSwitchItem(ImageVector.vectorResource(R.drawable.symbol_save), "Autosave", checked = false, enabled = false) {
                    error("A disabled setting must not be changed")
                }
            }
        }
        compose.onNodeWithText("Autosave").assertIsNotEnabled().assertIsOff()
    }

    @Test fun overflowMenuClosesOnBackAndAfterSelectingAnAction() {
        var copies = 0
        var deletes = 0
        compose.setContent {
            MeowmosTheme(dynamicColor = false) {
                MemoOverflowMenu(onDelete = { deletes++ }) {
                    listOf(MemoMenuAction(R.string.copy, R.drawable.symbol_content_copy) { copies++ })
                }
            }
        }

        val trigger = compose.onNodeWithContentDescription(context.getString(R.string.more_options))
        val copy = compose.onNodeWithText(context.getString(R.string.copy))
        val delete = compose.onNodeWithText(context.getString(R.string.delete))
        trigger.performClick()
        copy.assertIsDisplayed()
        delete.assertIsDisplayed()
        Espresso.pressBack()
        copy.assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, copies + deletes) }

        trigger.performClick()
        copy.performClick()
        copy.assertDoesNotExist()
        trigger.performClick()
        delete.performClick()
        delete.assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, copies)
            assertEquals(1, deletes)
        }
    }

    @Test fun syncingKeepsTheSearchTargetInPlaceAndDisablesSync() {
        var syncing by mutableStateOf(false)
        compose.setContent {
            MeowmosTheme(dynamicColor = false) {
                Row {
                    SyncStatusBadge(syncing = syncing, unsyncedCount = 3, onSync = { syncing = true })
                    Button(onClick = {}) { Text("Search target") }
                }
            }
        }
        val search = compose.onNodeWithText("Search target")
        val restingBounds = search.fetchSemanticsNode().boundsInRoot
        val sync = compose.onNodeWithContentDescription(context.getString(R.string.sync_status_sync_now))
        sync.performClick()
        sync.assertIsNotEnabled()
        assertEquals(restingBounds, search.fetchSemanticsNode().boundsInRoot)
    }

    @Test fun searchRestoresItsQueryAndClearActionRemovesIt() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MaterialTheme { MemoSearchBar(rememberTextFieldState(), onBack = {}) }
        }
        compose.onNode(hasSetTextAction()).performTextInput("offline notes")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction()).assertTextEquals("offline notes")
        compose.onNodeWithContentDescription(context.getString(R.string.clear_search)).performClick()
        compose.onNode(hasSetTextAction()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
        )
    }

    @Test fun rtlPaddingPreservesPhysicalInsetsAndAddsBottomSpace() {
        var result: PaddingValues? = null
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                result = edgeToEdgeContentPadding(
                    PaddingValues.Absolute(left = 12.dp, right = 28.dp, bottom = 16.dp),
                    additionalBottomPadding = 20.dp,
                )
            }
        }
        compose.runOnIdle {
            val padding = requireNotNull(result)
            assertEquals(12.dp, padding.calculateLeftPadding(LayoutDirection.Rtl))
            assertEquals(28.dp, padding.calculateRightPadding(LayoutDirection.Rtl))
            assertEquals(36.dp, padding.calculateBottomPadding())
        }
    }

    @Test fun pagingFailureExposesWorkingRetryAction() {
        var retries = 0
        compose.setContent {
            MaterialTheme { PagingStatus(LoadState.Error(IllegalStateException("offline"))) { retries++ } }
        }
        compose.onNodeWithText(context.getString(R.string.failed_to_load_memos)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.retry)).performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun compactMemoSelectionRestoresAndBackReturnsToList() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MaterialTheme {
                // Constrain logical width to a phone-sized pane on any test device.
                CompositionLocalProvider(LocalDensity provides Density(4f)) {
                    MemoListDetailLayout(
                        listPane = { select -> Button(onClick = { select("memo-one") }) { Text("Open memo") } },
                        detailPane = { id, back -> Button(onClick = back) { Text("Close $id") } },
                    )
                }
            }
        }
        compose.onNodeWithText("Open memo").performClick()
        compose.onNodeWithText("Close memo-one").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Close memo-one").assertIsDisplayed().performClick()
        compose.onNodeWithText("Open memo").assertIsDisplayed()
    }

    @Test fun selectionSurvivesChangingBetweenOneAndTwoPanes() {
        var wide by mutableStateOf(true)
        compose.setContent {
            MeowmosTheme(dynamicColor = false) {
                CompositionLocalProvider(LocalDensity provides Density(if (wide) 0.5f else 4f)) {
                    MemosNavigationScaffold(
                        useRail = wide,
                        destinations = MeowmosDestination.entries,
                        selectedRoute = RouteName.MEMOS,
                        onSelectDestination = {},
                        onCompose = {},
                    ) {
                        MemoListDetailLayout(
                            listPane = { select -> Button(onClick = { select("memo-two") }) { Text("List") } },
                            detailPane = { id, _ -> Text(id) },
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("List").performClick()
        compose.onNodeWithText("List").assertIsDisplayed()
        compose.onNodeWithText("memo-two").assertIsDisplayed()
        compose.runOnIdle { wide = false }
        compose.onNodeWithText("memo-two").assertIsDisplayed()
        compose.runOnIdle { wide = true }
        compose.onNodeWithText("List").assertIsDisplayed()
        compose.onNodeWithText("memo-two").assertIsDisplayed()
    }
}
