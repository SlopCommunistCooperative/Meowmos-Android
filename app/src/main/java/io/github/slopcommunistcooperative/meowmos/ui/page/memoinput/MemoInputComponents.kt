package io.github.slopcommunistcooperative.meowmos.ui.page.memoinput

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.local.entity.ResourceEntity
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.data.model.MemoLocation
import io.github.slopcommunistcooperative.meowmos.data.model.MemoSpace
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility
import io.github.slopcommunistcooperative.meowmos.ext.icon
import io.github.slopcommunistcooperative.meowmos.ext.string
import io.github.slopcommunistcooperative.meowmos.ext.titleResource
import io.github.slopcommunistcooperative.meowmos.ui.component.ActionIconButton
import io.github.slopcommunistcooperative.meowmos.ui.component.Attachment
import io.github.slopcommunistcooperative.meowmos.ui.component.InputImage
import io.github.slopcommunistcooperative.meowmos.ui.component.MemoLocationRow
import io.github.slopcommunistcooperative.meowmos.viewmodel.MemoInputViewModel
import io.github.slopcommunistcooperative.meowmos.ui.theme.MeowmosTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemoInputTopBar(
    isEditMode: Boolean,
    initialized: Boolean,
    canSubmit: Boolean,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
    destination: @Composable () -> Unit,
) {
    var closeRequested by rememberSaveable { mutableStateOf(false) }
    val requestClose: () -> Unit = { closeRequested = true }
    BackHandler(onBack = requestClose)
    LaunchedEffect(initialized, closeRequested) {
        if (initialized && closeRequested) {
            closeRequested = false
            onClose()
        }
    }

    TopAppBar(
        title = {
            if (isEditMode) {
                Text(R.string.edit.string)
            } else {
                Text(R.string.compose.string)
            }
        },
        navigationIcon = {
            ActionIconButton(label = stringResource(R.string.close), onClick = requestClose) {
                Icon(ImageVector.vectorResource(R.drawable.symbol_close), contentDescription = stringResource(R.string.close))
            }
        },
        actions = {
            destination()
            EditorSendButton(
                label = stringResource(R.string.post),
                enabled = initialized && canSubmit,
                onClick = onSubmit
            )
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EditorSendButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    posting: Boolean = false,
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shapes = IconButtonDefaults.shapes(),
        modifier = Modifier.semantics { contentDescription = label },
    ) {
        if (posting) {
            LoadingIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Icon(ImageVector.vectorResource(R.drawable.symbol_send), contentDescription = null)
        }
    }
}

@Composable
private fun EditorOptionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    aboveAnchor: Boolean = false,
    content: @Composable () -> Unit,
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
            if (aboveAnchor) MenuAnchorPosition.Above else MenuAnchorPosition.Below,
        ),
        properties = if (aboveAnchor) PopupProperties(focusable = false) else MenuDefaults.DefaultMenuProperties,
    ) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShapes(),
            modifier = Modifier.verticalScroll(rememberScrollState()),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) { content() }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MemoSpacePicker(
    spaces: List<MemoSpace>,
    selected: String?,
    remembered: MemoSpace?,
    onSelect: (MemoSpace?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val noSpace = stringResource(R.string.memo_no_space)
    val label = selected?.let { name ->
        spaces.find { it.name == name }?.title
            ?: remembered?.takeIf { it.name == name }?.title
            ?: name.substringAfter('/')
    } ?: noSpace

    val hasSelectedSpace = selected != null
    Box {
        FilledTonalButton(
            onClick = { expanded = !expanded },
            shapes = ButtonDefaults.shapes(),
            colors = if (hasSelectedSpace) {
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = MeowmosTheme.colors.wayfindingContainer,
                    contentColor = MeowmosTheme.colors.onWayfindingContainer,
                )
            } else {
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                label,
                Modifier.widthIn(max = 112.dp),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                ImageVector.vectorResource(R.drawable.symbol_arrow_drop_down),
                contentDescription = stringResource(R.string.memo_select_space),
                modifier = Modifier.size(18.dp),
            )
        }
        EditorOptionsMenu(expanded, onDismissRequest = { expanded = false }) {
            val options = listOf(null) + spaces
            options.forEachIndexed { index, space ->
                SelectableDropdownMenuItem(
                    selected = space?.name == selected,
                    shapes = MenuDefaults.itemShape(index, options.size),
                    text = { Text(space?.title ?: noSpace) },
                    onClick = {
                        expanded = false
                        onSelect(space)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MemoVisibilityPicker(
    currentVisibility: MemoVisibility,
    showSpaceVisibility: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onVisibilitySelected: (MemoVisibility) -> Unit,
    enabled: Boolean = true,
    label: String = stringResource(currentVisibility.titleResource),
    showLabel: Boolean = false,
) {
    Box {
        EditorOptionsMenu(
            expanded = expanded && enabled,
            onDismissRequest = { onExpandedChange(false) },
            aboveAnchor = true,
        ) {
            val options = if (showSpaceVisibility) MemoVisibility.entries else NonSpaceVisibilities
            options.forEachIndexed { index, visibility ->
                SelectableDropdownMenuItem(
                    selected = currentVisibility == visibility,
                    shapes = MenuDefaults.itemShape(index, options.size),
                    text = { Text(stringResource(visibility.titleResource)) },
                    onClick = {
                        onVisibilitySelected(visibility)
                        onExpandedChange(false)
                    },
                    leadingIcon = { Icon(visibility.icon, contentDescription = null) },
                )
            }
        }
        if (showLabel) {
            FilledTonalButton(
                shapes = ButtonDefaults.shapes(),
                enabled = enabled,
                onClick = { onExpandedChange(!expanded) },
                modifier = Modifier.semantics { contentDescription = label },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Icon(currentVisibility.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(currentVisibility.titleResource),
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    ImageVector.vectorResource(R.drawable.symbol_arrow_drop_down),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            FilledTonalIconButton(
                shapes = IconButtonDefaults.shapes(),
                enabled = enabled,
                onClick = { onExpandedChange(!expanded) },
                modifier = Modifier.semantics { contentDescription = label },
            ) {
                Icon(currentVisibility.icon, contentDescription = null)
            }
        }
    }
}

private val NonSpaceVisibilities = MemoVisibility.entries.filterNot { it == MemoVisibility.SPACE }

@Composable
private fun FormattingButtons(
    onFormat: (MarkdownFormat) -> Unit,
) {
    MarkdownFormat.entries.forEach { format ->
        ActionIconButton(label = format.label, onClick = { onFormat(format) }) {
            when (format) {
                MarkdownFormat.BOLD -> Icon(ImageVector.vectorResource(R.drawable.symbol_format_bold), contentDescription = format.label)
                MarkdownFormat.ITALIC -> Icon(ImageVector.vectorResource(R.drawable.symbol_format_italic), contentDescription = format.label)
                MarkdownFormat.STRIKETHROUGH -> Icon(ImageVector.vectorResource(R.drawable.symbol_format_strikethrough), contentDescription = format.label)
                MarkdownFormat.BULLET -> Icon(ImageVector.vectorResource(R.drawable.symbol_format_list_bulleted), contentDescription = format.label)
                MarkdownFormat.NUMBERED -> Icon(ImageVector.vectorResource(R.drawable.symbol_format_list_numbered), contentDescription = format.label)
                MarkdownFormat.H1, MarkdownFormat.H2, MarkdownFormat.H3 -> Text(
                    text = format.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
internal fun MemoInputBottomBar(
    currentAccount: Account?,
    currentVisibility: MemoVisibility,
    showSpaceVisibility: Boolean,
    visibilityMenuExpanded: Boolean,
    onVisibilityExpandedChange: (Boolean) -> Unit,
    onVisibilitySelected: (MemoVisibility) -> Unit,
    tags: List<String>,
    tagMenuExpanded: Boolean,
    onTagExpandedChange: (Boolean) -> Unit,
    onHashTagClick: () -> Unit,
    onTagSelected: (String) -> Unit,
    onToggleTodoItem: () -> Unit,
    onPickImage: () -> Unit,
    onPickAttachment: () -> Unit,
    onTakePhoto: () -> Unit,
    onFormat: (MarkdownFormat) -> Unit,
    showLocation: Boolean = false,
    onLocationClick: () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    HorizontalFloatingToolbar(
        expanded = true,
        modifier = Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (currentAccount !is Account.Local) {
                MemoVisibilityPicker(
                    currentVisibility = currentVisibility,
                    showSpaceVisibility = showSpaceVisibility,
                    expanded = visibilityMenuExpanded,
                    onExpandedChange = onVisibilityExpandedChange,
                    onVisibilitySelected = onVisibilitySelected,
                )
            }
            if (tags.isEmpty()) {
                ActionIconButton(label = stringResource(R.string.tag), onClick = onHashTagClick) {
                    Icon(ImageVector.vectorResource(R.drawable.symbol_tag), contentDescription = null)
                }
            } else {
                Box {
                    EditorOptionsMenu(
                        expanded = tagMenuExpanded,
                        onDismissRequest = { onTagExpandedChange(false) },
                        aboveAnchor = true,
                    ) {
                        tags.forEachIndexed { index, tag ->
                            DropdownMenuItem(
                                shape = MenuDefaults.itemShape(index, tags.size).shape,
                                text = { Text(tag) },
                                onClick = {
                                    onTagSelected(tag)
                                    onTagExpandedChange(false)
                                },
                                leadingIcon = {
                                    Icon(ImageVector.vectorResource(R.drawable.symbol_tag), contentDescription = null)
                                },
                            )
                        }
                    }
                    ActionIconButton(label = stringResource(R.string.tags), onClick = { onTagExpandedChange(!tagMenuExpanded) }) {
                        Icon(ImageVector.vectorResource(R.drawable.symbol_tag), contentDescription = null)
                    }
                }
            }
            ActionIconButton(label = stringResource(R.string.add_task), onClick = onToggleTodoItem) {
                Icon(ImageVector.vectorResource(R.drawable.symbol_check_box), contentDescription = null)
            }
            ActionIconButton(label = stringResource(R.string.add_image), onClick = onPickImage) {
                Icon(ImageVector.vectorResource(R.drawable.symbol_image), contentDescription = null)
            }
            ActionIconButton(label = stringResource(R.string.attachment), onClick = onPickAttachment) {
                Icon(ImageVector.vectorResource(R.drawable.symbol_attachment), contentDescription = null)
            }
            ActionIconButton(label = stringResource(R.string.take_photo), onClick = onTakePhoto) {
                Icon(ImageVector.vectorResource(R.drawable.symbol_photo_camera), contentDescription = null)
            }
            if (showLocation) {
                ActionIconButton(label = stringResource(R.string.memo_location), onClick = onLocationClick) {
                    Icon(ImageVector.vectorResource(R.drawable.symbol_location_on), contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.size(4.dp))
            FormattingButtons(onFormat = onFormat)
        }
    }
}

@Composable
internal fun MemoEditorTextField(
    modifier: Modifier,
    text: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    label: String,
    enabled: Boolean = true,
    errorMessage: String? = null,
) {
    TextField(
        modifier = modifier,
        value = text,
        label = { Text(label) },
        onValueChange = onTextChange,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        textStyle = MaterialTheme.typography.bodyLarge,
        isError = errorMessage != null,
        supportingText = errorMessage?.let { message -> { Text(message) } },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MemoInputEditor(
    modifier: Modifier = Modifier,
    text: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    validMimeTypePrefixes: Set<String>,
    onDroppedText: (String) -> Unit,
    uploadResources: List<ResourceEntity>,
    inputViewModel: MemoInputViewModel,
    location: MemoLocation? = null,
    onLocationClick: () -> Unit = {},
) {
    val imageResources = remember(uploadResources) {
        uploadResources.filter { it.mimeType?.startsWith("image/") == true }
    }
    val attachmentResources = remember(uploadResources) {
        uploadResources.filterNot { it.mimeType?.startsWith("image/") == true }
    }

    Column(
        modifier
            .fillMaxHeight()
            .dragAndDropTarget(
                shouldStartDragAndDrop = accept@{ startEvent ->
                    startEvent
                        .mimeTypes()
                        .any { eventMimeType ->
                            validMimeTypePrefixes.any(eventMimeType::startsWith)
                        }
                },
                target = object : DragAndDropTarget {
                    override fun onDrop(event: DragAndDropEvent): Boolean {
                        val androidDragEvent = event.toAndroidDragEvent()
                        val concatText = androidDragEvent.clipData
                            .textList()
                            .fold("") { acc, droppedText ->
                                if (acc.isNotBlank()) {
                                    acc.trimEnd { it == '\n' } + "\n\n" + droppedText.trimStart { it == '\n' }
                                } else {
                                    droppedText
                                }
                            }
                        onDroppedText(concatText)
                        return true
                    }
                }
            )
    ) {
        MemoEditorTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .weight(1f)
                .focusRequester(focusRequester),
            text = text,
            onTextChange = onTextChange,
            label = R.string.any_thoughts.string
        )

        location?.let {
            MemoLocationRow(it, Modifier.padding(horizontal = 20.dp), onClick = onLocationClick)
        }

        if (imageResources.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .height(80.dp)
                    .padding(
                        start = 15.dp,
                        end = 15.dp,
                        bottom = if (attachmentResources.isEmpty()) 15.dp else 8.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(imageResources, key = { it.identifier }) { resource ->
                    InputImage(resource = resource, inputViewModel = inputViewModel)
                }
            }
        }

        if (attachmentResources.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .padding(start = 15.dp, end = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(attachmentResources, key = { it.identifier }) { resource ->
                    Attachment(
                        resource = resource,
                        onRemove = { inputViewModel.deleteResource(resource.identifier) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SaveChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_changes_title)) },
        text = { Text(stringResource(R.string.save_changes_message)) },
        confirmButton = {
            Button(onClick = onSave, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.discard))
            }
        }
    )
}

private fun ClipData.textList(): List<String> {
    return (0 until itemCount)
        .mapNotNull(::getItemAt)
        .mapNotNull { it.text?.toString() }
}
